package com.ByteKnights.com.resturarent_system.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    /*
     * Staff login URL is loaded from application.properties.
     */
    @Value("${app.frontend.staff-login-url}")
    private String staffLoginUrl;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /*
     * Builds the email body for newly created staff members.
     */
    public String buildStaffInviteEmailBody(String username, String temporaryPassword) {

        /*
         * Email body sent to newly created staff members.
         */
        return "Hello " + username + ",\n\n" +
               "Your staff account has been created successfully.\n\n" +
               "Login URL: " + staffLoginUrl + "\n\n" +
               "Temporary Password: " + temporaryPassword + "\n\n" +
               "Please log in using this temporary password and change your password immediately.\n\n" +
               "Regards,\n" +
               "Crave House Development Team";
    }

    /*
     * Subject line for staff invite email.
     */
    public String getStaffInviteSubject() {
        return "Your Staff Account Credentials";
    }

    /*
     * Subject line for customer password reset email.
     */
    public String getCustomerPasswordResetSubject() {
        return "CraveHouse - Customer Password Reset";
    }

    /*
     * Builds the email body for customer password reset.
     */
    public String buildCustomerPasswordResetEmailBody(String resetLink) {
        return "Hello,\n\n" +
               "We received a request to reset your password for your CraveHouse Customer account.\n\n" +
               "Please click the link below to set a new password. This link is valid for 15 minutes.\n\n" +
               resetLink + "\n\n" +
               "If you did not request a password reset, please ignore this email.\n\n" +
               "Regards,\n" +
               "CraveHouse Team";
    }

    /*
     * Subject line for customer email verification.
     */
    public String getCustomerEmailVerificationSubject() {
        return "CraveHouse - Verify Your Email Address";
    }

    /*
     * Builds the email body for customer email verification.
     */
    public String buildCustomerEmailVerificationBody(String verificationLink) {
        return "Hello,\n\n" +
               "Please verify your email address for your CraveHouse Customer account.\n\n" +
               "Click the link below to verify your email. This link is valid for 24 hours.\n\n" +
               verificationLink + "\n\n" +
               "If you did not request this verification, please ignore this email.\n\n" +
               "Regards,\n" +
               "CraveHouse Team";
    }
}