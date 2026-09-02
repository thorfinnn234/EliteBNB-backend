package com.elitebnb_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final String brevoApiKey;
    private final String brevoApiUrl;
    private final RestTemplate restTemplate;

    public EmailService(
            @Value("${brevo.api.key}") String brevoApiKey,
            @Value("${brevo.api.url}") String brevoApiUrl
    ) {
        this.brevoApiKey = brevoApiKey;
        this.brevoApiUrl = brevoApiUrl;
        this.restTemplate = new RestTemplate();
    }

    public void sendVerificationCode(
            String email,
            String code
    ) {

        String html = """
                <div style="font-family: Arial, sans-serif;">
                    <h2>Verify your EliteBNB account</h2>

                    <p>Welcome to EliteBNB!</p>

                    <p>Your verification code is:</p>

                    <h1 style="letter-spacing: 6px;">
                        %s
                    </h1>

                    <p>This code expires in 10 minutes.</p>

                    <p>
                        If you did not create this account,
                        you can ignore this email.
                    </p>
                </div>
                """.formatted(code);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.set(
                "api-key",
                brevoApiKey
        );

        Map<String, Object> sender = Map.of(
                "name", "EliteBNB",
                "email", "habeeboreoluwa12@gmail.com"
        );

        Map<String, Object> recipient = Map.of(
                "email", email
        );

        Map<String, Object> body = Map.of(
                "sender", sender,
                "to", List.of(recipient),
                "subject", "Verify your EliteBNB account",
                "htmlContent", html
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        restTemplate.postForEntity(
                brevoApiUrl,
                request,
                String.class
        );
    }
}