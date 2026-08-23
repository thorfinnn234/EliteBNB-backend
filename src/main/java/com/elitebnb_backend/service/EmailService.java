package com.elitebnb_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(
            String email,
            String code
    ) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(email);
        message.setSubject("Verify your EliteBNB account");

        message.setText(
                "Welcome to EliteBNB!\n\n" +
                        "Your verification code is: " + code +
                        "\n\nThis code expires in 10 minutes." +
                        "\n\nIf you did not create this account, ignore this email."
        );

        mailSender.send(message);
    }
}