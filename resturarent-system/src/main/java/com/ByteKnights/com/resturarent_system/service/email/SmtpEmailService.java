package com.ByteKnights.com.resturarent_system.service.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;
    private final EmailTestingService emailTestingService;

    public SmtpEmailService(JavaMailSender mailSender,
                            EmailTemplateService templateService,
                            EmailTestingService emailTestingService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
        this.emailTestingService = emailTestingService;
    }

    @Override
    public void sendStaffInviteEmail(String toEmail, String username, String temporaryPassword) {
        // Validate email format first
        if (!toEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + toEmail);
        }

        // Runtime testing switch
        if (emailTestingService.isForceFail()) {
            throw new RuntimeException("Forced email failure for testing");
        }

        String subject = templateService.getStaffInviteSubject();
        String body = templateService.buildStaffInviteEmailBody(username, temporaryPassword);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}