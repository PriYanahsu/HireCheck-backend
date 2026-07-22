package com.hirecheck.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${app.emailjs.public-key:}") private String publicKey;
    @Value("${app.emailjs.private-key:}") private String privateKey;
    @Value("${app.emailjs.service-id:}") private String serviceId;
    @Value("${app.emailjs.template-id:}") private String templateId;
    @Value("${app.public.url}") private String publicUrl;

    public Map<String, Object> sendTestInvitation(String email, String name, String title,
            String testLink, int duration, String company) {
        if (publicKey.isBlank() || privateKey.isBlank() || serviceId.isBlank() || templateId.isBlank()) {
            return Map.of("success", false, "error", "EmailJS configuration is missing.");
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("to_email", email);
            params.put("to_name", name);
            params.put("test_title", title);
            params.put("test_link", publicUrl + "/take-test/" + testLink);
            params.put("test_duration", duration);
            params.put("company_name", company != null ? company : "Cognivac");
            params.put("subject", "You've been invited to take a logical assessment");

            Map<String, Object> body = new HashMap<>();
            body.put("service_id", serviceId);
            body.put("template_id", templateId);
            body.put("user_id", publicKey);
            body.put("template_params", params);
            body.put("accessToken", privateKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity("https://api.emailjs.com/api/v1.0/email/send",
                    new HttpEntity<>(body, headers), String.class);
            return Map.of("success", true);
        } catch (Exception ex) {
            return Map.of("success", false, "error", ex.getMessage() != null ? ex.getMessage() : "Email failed");
        }
    }
}
