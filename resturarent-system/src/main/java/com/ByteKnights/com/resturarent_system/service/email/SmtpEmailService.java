package com.ByteKnights.com.resturarent_system.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    // For testing only:
    // if true, email sending will fail intentionally even if SMTP config is correct
    @Value("${app.email.force-fail:false}")
    private boolean forceFail;

    public SmtpEmailService(JavaMailSender mailSender, EmailTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
    }

    @Override
    public void sendStaffInviteEmail(String toEmail, String username, String temporaryPassword) {
        // Basic email format validation before attempting SMTP send
        if (!toEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + toEmail);
        }

        // Manual failure switch for testing fallback behavior
        if (forceFail) {
            throw new RuntimeException("Forced email failure for testing");
        }

        String subject = templateService.getStaffInviteSubject();
        String body = templateService.buildStaffInviteEmailBody(username, temporaryPassword);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        // If SMTP is broken at runtime (wrong password, auth issue, host issue, etc.)
        // this line should throw an exception and be handled in StaffService
        mailSender.send(message);
    }
}