package com.ByteKnights.com.resturarent_system.service.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {

    /*
     * JavaMailSender is provided by MailConfig.
     * We use it to send the actual email.
     */
    private final JavaMailSender mailSender;

    private final EmailTemplateService templateService;

    /*
     * EmailTestingService is used to intentionally fail email sending
     * when we want to test failure scenarios.
     */
    private final EmailTestingService emailTestingService;

    /*
     * Constructor injection
     * Spring automatically provides:
     * - JavaMailSender
     * - EmailTemplateService
     * - EmailTestingService
     */
    public SmtpEmailService(JavaMailSender mailSender,
                            EmailTemplateService templateService,
                            EmailTestingService emailTestingService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
        this.emailTestingService = emailTestingService;
    }

    /*
     * Sends staff invite email
     * This method is called after creating a staff account
     * or when resending a staff invite.
     */
    @Override
    public void sendStaffInviteEmail(String toEmail, String username, String temporaryPassword) {

        /*
         * Basic email format validation
         * If email format is invalid, we stop before calling SMTP.
         * This prevents unnecessary email sending attempts.
         */
        if (!toEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + toEmail);
        }

        /*
         * Testing switch
         * This is useful for testing:
         * - staff created successfully
         * - email failed
         * - temporary password shown manually in frontend
         */
        if (emailTestingService.isForceFail()) {
            throw new RuntimeException("Forced email failure for testing");
        }

        /*
         * Get email subject and body from EmailTemplateService 
         * this service focused only on sending.
         */
        String subject = templateService.getStaffInviteSubject();
        String body = templateService.buildStaffInviteEmailBody(username, temporaryPassword);

        /*
         * SimpleMailMessage is used for plain text email, not sending HTML email
         */
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        /*
         * Send the email using SMTP.
         * If SMTP fails, this line throws an exception.
         * StaffService can catch that exception and still keep staff creation successful.
         */
        mailSender.send(message);
    }
}