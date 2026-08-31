package com.elitebnb_backend.service;

import com.resend.Resend;
import com.resend.services.emails.model.SendEmailRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final Resend resend;

    public EmailService(
            @Value("${resend.api.key}") String apiKey
    ) {
        this.resend = new Resend(apiKey);
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

        SendEmailRequest request =
                SendEmailRequest.builder()
                        .from("EliteBNB <onboarding@resend.dev>")
                        .to(email)
                        .subject("Verify your EliteBNB account")
                        .html(html)
                        .build();

        resend.emails().send(request);
    }
}