package com.ByteKnights.com.resturarent_system.service.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    public SmtpEmailService(JavaMailSender mailSender,
                            EmailTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
    }

    @Override
    public void sendStaffInviteEmail(String toEmail, String username, String temporaryPassword) {
        String subject = templateService.getStaffInviteSubject();
        String body = templateService.buildStaffInviteEmailBody(username, temporaryPassword);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}
