# HireCheck API (Spring Boot)

## Run locally

```bash
# from hirecheck-backend/
# create .env from .env.example
mvn spring-boot:run
```

API: `http://localhost:8080`

## Deploy (Render)

**Option A — native**
- Root: this repo
- Build: `./mvnw -DskipTests package`
- Start: `java -jar target/*.jar`

**Option B — Docker**
```bash
docker build -t hirecheck-api .
docker run -p 8080:8080 --env-file .env hirecheck-api
```

Env: `JWT_SECRET`, `FRONTEND_URL` (Vercel URL), `PUBLIC_URL`, `PGHOST` / `PG*` or `SPRING_DATASOURCE_*`

This service is **API-only**. React is deployed separately (Vercel / frontend Docker).
