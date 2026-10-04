# HireCheck — Backend

Spring Boot REST API behind HireCheck, a technical assessment platform for hiring teams. It owns
recruiter accounts, tests and questions, candidate invitations, answer grading, the test clock,
and every number on the dashboard.

**Frontend:** https://github.com/PriYanahsu/HireCheck-frontend · **Live API:** https://hirecheck-backend-18ms.onrender.com

---

## What it's responsible for

The frontend presents a test. This service decides everything that has to be **trusted**: who a
recruiter is, which tests they can see, what the correct answers are, how many points a
candidate earned, and when their time ran out.

```
React SPA (Vercel)
   │
   ├── Recruiter:  Authorization: Bearer <JWT>
   └── Candidate:  /api/candidate/{testLink}     (no account — the link is the credential)
   │
   ▼
┌──────────────────────────────────────────────────────────┐
│  Spring Boot 3  (Render)                                 │
│                                                          │
│  CORS ─► JwtAuthFilter ─► SecurityFilterChain            │
│            parses JWT,       public / authenticated /    │
│            sets principal    deny-by-default             │
│                    │                                     │
│  Controllers ──────┤  auth · tests · questions ·         │
│                    │  candidates · candidate · public ·  │
│                    │  dashboard · imagekit               │
│                    ▼                                     │
│  Services          CandidateTestService (grading)        │
│                    StatsService · EmailService ·         │
│                    ImageKitService                       │
│                    ▼                                     │
│  Repositories      Spring Data JPA                       │
│                                                          │
│  AutoSubmitScheduler  ── every 60 s, closes expired tests│
└───────┬───────────────────────┬───────────────┬──────────┘
        ▼                       ▼               ▼
  ┌────────────┐         ┌────────────┐   ┌────────────┐
  │ PostgreSQL │         │  EmailJS   │   │  ImageKit  │
  │            │         │  invites   │   │  upload    │
  └────────────┘         └────────────┘   │  signing   │
                                          └────────────┘
```

There are no server sessions. A recruiter's request carries a signed token; a candidate's
request carries a random link. Either one is enough on its own, so any instance can serve any
request and a restart loses nothing.

---

## The one idea worth knowing

An assessment platform is only worth something if candidates can't game it, and the browser is
the one place a candidate has complete control. So the rule here is: **the client is told only
what it needs to render, and everything that decides an outcome happens on the server.**

- **The answer key never leaves the server.** When a candidate loads a test, each question is
  rebuilt into a response containing only the fields that question type needs — options for
  multiple choice, an image for pattern recognition, test cases for coding, guidelines for
  subjective. The `answer` field is never copied across.
- **Grading happens on save.** Each answer is checked against the key as it arrives and stored
  with `isCorrect` and the points earned. The final score is computed by the server from those
  stored rows, never accepted from the client.
- **The clock lives in the database.** `startedAt` is set once, by the server, when the test
  begins. A candidate can't restart it by refreshing, and can't start the same link twice.
- **Walking away doesn't stop the clock.** If the browser closes and never submits,
  `AutoSubmitScheduler` notices once the time limit plus a 5-minute grace period has passed,
  and submits the test itself.

The frontend's fullscreen and tab-switch checks discourage cheating. These rules prevent it.

---

## Tech stack

| | |
|---|---|
| Runtime | Java 17 |
| Framework | Spring Boot 3.3 (Web MVC, Data JPA, Security, Validation) |
| Database | PostgreSQL |
| ORM | Hibernate 6, with native JSON columns (`@JdbcTypeCode(SqlTypes.JSON)`) |
| Auth | Stateless JWT — JJWT 0.12 (HMAC-SHA) |
| Scheduling | Spring `@Scheduled` |
| Email | EmailJS REST API over `RestTemplate` |
| Image uploads | ImageKit — server-side HMAC-SHA1 upload signatures |
| Build | Maven (wrapper included) |
| Deploy | Docker → Render |

---

## Quick start

**Prerequisites:** JDK 17 and a PostgreSQL database.

```bash
cp .env.example .env     # then fill in the values below
./mvnw spring-boot:run   # http://localhost:8080
```

`application.properties` imports `.env` from the project root when it exists, so local
development needs no exported shell variables. Hibernate creates and updates the tables on
boot, so an empty database is enough.

Check it's up — signup needs no token:

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"username":"recruiter","password":"secret","email":"r@example.com","name":"Recruiter"}'
```

Run the [frontend](https://github.com/PriYanahsu/HireCheck-frontend) with `npm run dev`; its
Vite dev server proxies `/api` to this port.

### Environment variables

| Variable | Required | What it's for |
|---|---|---|
| `SPRING_DATASOURCE_URL` | yes* | JDBC URL, e.g. `jdbc:postgresql://host:5432/hirecheck?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | yes* | Database user |
| `SPRING_DATASOURCE_PASSWORD` | yes* | Database password |
| `JWT_SECRET` | yes | HMAC signing key. Use 32+ random bytes |
| `FRONTEND_URL` | yes | Allowed CORS origin(s), comma-separated, exact match |
| `PUBLIC_URL` | yes | Origin used to build the link in invitation emails — must be the **frontend**, since `/take-test/…` is a frontend route |
| `EMAILJS_PUBLIC_KEY` `EMAILJS_PRIVATE_KEY` `EMAILJS_SERVICE_ID` `EMAILJS_TEMPLATE_ID` | no | Invitation emails. Without them, invites still succeed and the response says the email wasn't sent |
| `IMAGEKIT_PRIVATE_KEY` | no | Signs image uploads for pattern-recognition questions |
| `PORT` | no | Defaults to 8080; Render injects it |

\* `application.properties` also has a fallback to the `PG*` variables that managed Postgres
services inject, but it is currently broken (see Known gaps). Set the three
`SPRING_DATASOURCE_*` values.

---

## API reference

All routes are under `/api`. Recruiter routes need `Authorization: Bearer <token>`. Errors
always come back as `{ "message": "..." }`.

### Auth — `/api/auth`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/signup` | public | Create a recruiter account; returns a token and profile |
| POST | `/login` | public | Exchange username and password for a token |
| GET | `/me` | required | The current recruiter's profile |
| POST | `/logout` | required | Acknowledges logout (the token is discarded client-side) |

```http
POST /api/auth/login
{ "username": "recruiter", "password": "..." }

200 OK
{ "token": "eyJhbGciOi...", "id": 1, "username": "recruiter",
  "email": "r@example.com", "name": "Recruiter", "company": "Acme" }
```

A wrong username and a wrong password both return the same `401 Incorrect username or
password`, so the endpoint can't be used to discover which usernames exist.

### Tests — `/api/tests`

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | The recruiter's tests, each with question count and candidate stats |
| GET | `/{id}` | One test with its ordered questions (answers included — this is the recruiter view) and stats |
| POST | `/` | Create: `title`, `description`, `duration` (minutes), `passingScore`, `shuffleQuestions` |
| PUT | `/{id}` | Partial update — only the fields sent are changed |
| DELETE | `/{id}` | Delete the test and, in order, its responses, candidates and questions |
| GET | `/{id}/candidates` | Everyone invited to this test |

### Questions — `/api/questions`

| Method | Path | Purpose |
|---|---|---|
| POST | `/` | Add a question to a test the recruiter owns |
| PUT | `/{id}` | Partial update |
| DELETE | `/{id}` | Remove |

```json
{
  "testId": 3,
  "type": "multipleChoice",
  "content": "Which of these is NOT a JavaScript data type?",
  "options": ["String", "Boolean", "Float", "Symbol"],
  "answer": "2",
  "points": 1,
  "order": 0
}
```

`type` is one of `multipleChoice`, `patternRecognition`, `coding`, `subjective`. `options` and
`testCases` are shape-checked and stored as JSON columns; anything malformed is a `400`.

### Candidates (recruiter) — `/api/candidates`

| Method | Path | Purpose |
|---|---|---|
| GET | `/?status=` | Candidates across all the recruiter's tests, newest first, optional status filter |
| POST | `/` | Invite one candidate: create the record, generate a link, send the email |
| POST | `/bulk-invite` | Invite many: `{ testId, candidates: [{ name, email, phone }] }` |
| DELETE | `/{id}` | Remove a candidate |

Bulk invite validates every row independently and never fails as a whole. The response lists
each candidate with `success: true` and their new link, or `success: false` with the reason —
so one bad row in a spreadsheet of fifty doesn't block the other forty-nine.

### Taking a test (candidate) — `/api/candidate/{testLink}`

Public. The link is the only credential.

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Test title, duration, `startedAt`, and the questions — answers stripped, shuffled if the test says so |
| POST | `/start` | Begin: status → `in_progress`, record `startedAt` and client IP |
| POST | `/responses` | Save or overwrite one answer: `{ questionId, response }` — graded on arrival |
| GET | `/responses/{questionId}` | Fetch a saved answer, so a reload can restore it |
| POST | `/submit` | Finish: `{ autoSubmitted }` — computes and stores the final score |

A completed link returns `403 This test has already been completed` on every route, and a
second `/start` returns `403 This test is already in progress`.

### Public registration — `/api/public-test`

| Method | Path | Purpose |
|---|---|---|
| POST | `/{testId}/register` | Self-register with `{ name, email, phone }`; returns `{ testLink }` |

One registration per email per test — a repeat gets `400 You have already taken this test`.

### Dashboard — `/api/dashboard`

| Method | Path | Purpose |
|---|---|---|
| GET | `/stats` | Active tests, pending assessments, completed assessments |
| GET | `/recent-activity` | The 10 latest starts and completions, with scores |
| GET | `/performance` | Top 5 tests by average score |

### Image uploads — `/api/imagekit-auth`

| Method | Path | Purpose |
|---|---|---|
| GET | `/imagekit-auth` | `{ token, expire, signature }` for one direct browser-to-ImageKit upload |

### Status codes

| Status | When |
|---|---|
| 400 | Validation failure, malformed body, duplicate username / email / registration |
| 401 | Missing, invalid or expired token; bad login |
| 403 | Authenticated but not the owner; test already completed or already started |
| 404 | Unknown test, question, candidate or test link |

---

## Scoring

Implemented in [`CandidateTestService`](src/main/java/com/hirecheck/service/CandidateTestService.java).

**On every saved answer**

1. The question is loaded and checked to belong to the candidate's test — a candidate can't
   post answers to questions from some other test.
2. For `multipleChoice` and `patternRecognition`, the response (the chosen option's index) is
   compared with the stored answer. A match earns the question's points; anything else earns 0.
3. `coding` and `subjective` answers are stored with 0 points, for the recruiter to review.
4. The row is upserted on `(candidate, question)`, so changing an answer replaces it rather than
   adding another.

**On submit** (`@Transactional`)

```
score % = round( Σ points earned  /  Σ points available  × 100 )
```

Points available is summed over every question in the test, so an unanswered question counts
against the candidate. The candidate is then marked `completed` with `completedAt`, the score,
and whether the submission was automatic.

---

## Background job: auto-submit

[`AutoSubmitScheduler`](src/main/java/com/hirecheck/scheduler/AutoSubmitScheduler.java) runs
every 60 seconds (`@EnableScheduling` on the application class).

```
for each candidate with status = in_progress, startedAt set, completedAt null
    deadline = startedAt + test.duration + 5 minutes
    if now > deadline  →  submitCandidateTest(candidate, autoSubmitted = true)
```

The frontend submits the moment its timer reaches zero; this job covers the cases where it
can't — a closed laptop, a dead battery, a lost connection. The 5-minute grace keeps the job
from racing a browser that is about to submit on its own. Errors are logged and the loop picks
up again on the next tick.

---

## Data model

```
users ──< tests ──< questions
            │
            └──< candidates ──< responses >── questions
```

| Table | Holds | Notes |
|---|---|---|
| `users` | recruiter: username, email, name, company, password | `username` and `email` unique |
| `tests` | title, description, duration (min), passing score, shuffle flag, owner | `created_by` → `users.id` |
| `questions` | type, content, code snippet, options, answer, test cases, guidelines, image URL, points, order | `options` and `test_cases` are **JSON** columns |
| `candidates` | name, email, phone, test, inviter, test link, status, timestamps, score, auto-submitted flag, IP | `test_link` unique |
| `responses` | candidate, question, answer text, correctness, points, submitted at | one row per candidate per question |

**Candidate lifecycle**

```
pending ──(POST /start)──► in_progress ──(POST /submit)──────────────► completed
                                 └──(scheduler: deadline passed)──────► completed (autoSubmitted)
```

Relationships are plain integer foreign-key columns rather than JPA associations. That keeps the
entities flat and avoids lazy-loading surprises, at the cost of cascades being done by hand —
which is why `DELETE /api/tests/{id}` removes responses, then candidates, then questions, then
the test.

`order` is a reserved word in SQL, so the column is mapped with explicit quoting
(`@Column(name = "\"order\"")`).

---

## Security

- **Stateless JWT.** `JwtAuthFilter` runs before Spring's username/password filter, verifies the
  signature and expiry with JJWT, and puts a `UserPrincipal` (id, username) in the security
  context. A bad token is simply ignored, so the request continues unauthenticated and is
  refused by the rules below. Tokens last 7 days.
- **Deny by default.** Login, signup, the candidate routes, public registration and ImageKit
  signing are public; every other `/api/**` route requires authentication; and
  `anyRequest().denyAll()` refuses anything unmatched. A new endpoint is protected the moment it
  exists.
- **Ownership checks.** Being logged in answers *who are you*; every recruiter route also asks
  *is this yours*. Tests, questions and candidates are loaded and their owner compared with the
  token's user, and a mismatch is a `403`. List endpoints only ever query the caller's own
  tests.
- **Unguessable test links.** Generated by [`NanoidUtil`](src/main/java/com/hirecheck/util/NanoidUtil.java)
  — 10 characters from a 62-symbol alphabet using `SecureRandom`, about 8 × 10¹⁷ possibilities.
- **No answer leakage.** The candidate's question payload is built field by field per question
  type, so the answer key can't slip through when the entity gains a new field.
- **Real client IPs behind proxies.** [`ClientIpUtil`](src/main/java/com/hirecheck/util/ClientIpUtil.java)
  reads `CF-Connecting-IP`, then `X-Real-IP`, then the first `X-Forwarded-For` entry, then the
  socket address, and strips IPv6-mapped prefixes. On Render the socket address belongs to the
  load balancer, so this is what makes the recorded IP meaningful.
- **Secrets stay server-side.** The EmailJS private key and the ImageKit private key are only
  ever read here. The browser gets a time-limited ImageKit signature (HMAC-SHA1 of a random
  token and an expiry about 40 minutes out), never the key.
- **CORS** allows only the origins in `FRONTEND_URL` (comma-separated, so local and production
  can coexist). Credentials are allowed, which is why the list can't be `*`.
- **Consistent errors.** `GlobalExceptionHandler` maps `ApiException` to its status, bean
  validation failures to `400` with the first field message, and anything else to `500`.

---

## Project structure

```
src/main/java/com/hirecheck/
├─ HireCheckApplication.java   entry point, @EnableScheduling
├─ config/       SecurityConfig — filter chain, route rules, CORS
├─ security/     JwtUtil (sign / parse), JwtAuthFilter, UserPrincipal
├─ controller/   Auth, Test, Question, Candidate, PublicTest, Dashboard, ImageKit
├─ service/      CandidateTestService (grading + submit), StatsService,
│                EmailService (EmailJS), ImageKitService (upload signing)
├─ scheduler/    AutoSubmitScheduler
├─ repository/   Spring Data JPA interfaces
├─ entity/       User, Test, Question, Candidate, Response
├─ dto/          LoginRequest, SignupRequest (Bean Validation)
├─ exception/    ApiException + @RestControllerAdvice
└─ util/         NanoidUtil, ClientIpUtil

src/main/resources/application.properties
```

---

## Docker and deployment

```bash
docker build -t hirecheck-api .
docker run -p 8080:8080 --env-file .env hirecheck-api
```

The [Dockerfile](Dockerfile) is a two-stage build:

1. **Build** on `maven:3.9-eclipse-temurin-17-alpine`. `pom.xml` and the Maven wrapper are
   copied and dependencies fetched *before* the source, so Docker caches that layer and a code
   change doesn't re-download every dependency.
2. **Run** on `eclipse-temurin:17-jre-alpine` — a JRE, not a JDK, with no build tools in the
   image — as a non-root `app` user.

```
-XX:+UseContainerSupport   # size the JVM from the container's memory limit, not the host's
-XX:MaxRAMPercentage=75.0  # let the heap use most of it instead of the default quarter
```

On **Render**, deploy either from the Dockerfile or natively (`./mvnw -DskipTests package`,
then `java -jar target/*.jar`). Render injects `PORT`, which Spring reads through
`server.port=${PORT:8080}`. Set the variables above in the dashboard; `.env` is excluded from
both git and the image.

---

## Known gaps

What still needs work, roughly in priority order:

- **Passwords are not hashed yet.** They're stored and compared as plain strings. Moving to
  BCrypt (`spring-security-crypto` is already on the classpath) is the next change.
- **Saved responses expose `isCorrect` to the candidate.** `GET /api/candidate/{link}/responses/{id}`
  returns the whole entity; it should return only the answer text.
- **`/api/imagekit-auth` is public**, so anyone can request an upload signature. It should
  require a recruiter token.
- **Short `JWT_SECRET`s are zero-padded** to 32 bytes rather than rejected at startup.
- **No schema migrations.** Hibernate's `ddl-auto=update` manages the tables; Flyway would make
  schema changes reviewable and repeatable. `show-sql` is also still on.
- **Stats are computed in memory** by looping over tests and candidates, one query per test.
  Fine at today's scale; aggregate queries would be needed for large accounts.
- **No recruiter endpoint for a candidate's individual answers**, so coding and subjective
  responses can't be reviewed or scored yet.
- **The `PG*` datasource fallback omits the host.** It builds
  `jdbc:postgresql://${PGPORT}/${PGDATABASE}`; it should be `${PGHOST}:${PGPORT}`.
- **Unexpected `500`s return the raw exception message**; they should log it and return a
  generic message.
- No automated tests yet.
