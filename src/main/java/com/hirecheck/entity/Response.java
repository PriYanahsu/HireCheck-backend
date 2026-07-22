package com.hirecheck.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "responses")
public class Response {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "candidate_id", nullable = false) private Integer candidateId;
    @Column(name = "question_id", nullable = false) private Integer questionId;
    @Column(nullable = false) private String response;
    @Column(name = "is_correct") private Boolean isCorrect;
    private Integer points;
    @Column(name = "submitted_at") private Instant submittedAt;

    @PrePersist void onCreate() { if (submittedAt == null) submittedAt = Instant.now(); }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCandidateId() { return candidateId; }
    public void setCandidateId(Integer candidateId) { this.candidateId = candidateId; }
    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
