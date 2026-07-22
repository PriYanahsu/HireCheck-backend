package com.hirecheck.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "questions")
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "test_id", nullable = false) private Integer testId;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String content;
    @Column(name = "code_snippet") private String codeSnippet;
    @JdbcTypeCode(SqlTypes.JSON) private List<String> options;
    private String answer;
    @Column(name = "test_cases") @JdbcTypeCode(SqlTypes.JSON) private List<Map<String, String>> testCases;
    @Column(name = "evaluation_guidelines") private String evaluationGuidelines;
    @Column(name = "image_url") private String imageUrl;
    private Integer points = 1;
    @Column(name = "\"order\"") private Integer order = 0;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getTestId() { return testId; }
    public void setTestId(Integer testId) { this.testId = testId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<Map<String, String>> getTestCases() { return testCases; }
    public void setTestCases(List<Map<String, String>> testCases) { this.testCases = testCases; }
    public String getEvaluationGuidelines() { return evaluationGuidelines; }
    public void setEvaluationGuidelines(String evaluationGuidelines) { this.evaluationGuidelines = evaluationGuidelines; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
}
