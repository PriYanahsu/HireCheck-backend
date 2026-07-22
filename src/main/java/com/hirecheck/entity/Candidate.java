package com.hirecheck.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "candidates")
public class Candidate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    private String phone;
    @Column(name = "test_id", nullable = false) private Integer testId;
    @Column(name = "invited_by", nullable = false) private Integer invitedBy;
    @Column(name = "test_link", nullable = false, unique = true) private String testLink;
    private String status = "pending";
    @Column(name = "invited_at") private Instant invitedAt;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    private Integer score;
    @Column(name = "auto_submitted") private Boolean autoSubmitted = false;
    @Column(name = "ip_address") private String ipAddress;

    @PrePersist void onCreate() {
        if (invitedAt == null) invitedAt = Instant.now();
        if (status == null) status = "pending";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getTestId() { return testId; }
    public void setTestId(Integer testId) { this.testId = testId; }
    public Integer getInvitedBy() { return invitedBy; }
    public void setInvitedBy(Integer invitedBy) { this.invitedBy = invitedBy; }
    public String getTestLink() { return testLink; }
    public void setTestLink(String testLink) { this.testLink = testLink; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getInvitedAt() { return invitedAt; }
    public void setInvitedAt(Instant invitedAt) { this.invitedAt = invitedAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Boolean getAutoSubmitted() { return autoSubmitted; }
    public void setAutoSubmitted(Boolean autoSubmitted) { this.autoSubmitted = autoSubmitted; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
