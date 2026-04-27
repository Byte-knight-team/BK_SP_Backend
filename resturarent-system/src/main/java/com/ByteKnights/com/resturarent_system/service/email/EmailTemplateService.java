package com.ByteKnights.com.resturarent_system.service.email;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    /*
     * Builds the email body for newly created staff members.
     * This class only creates email text.
     * It does not send the email.
     * Sending is handled by SmtpEmailService.
     */
    public String buildStaffInviteEmailBody(String username, String temporaryPassword) {

        /*
         * Local frontend login URL for staff users.
         * Current development URL
         */
        String loginUrl = "http://localhost:5173/staff/login";

        /*
         * Email body sent to newly created staff members.
         */
        return "Hello " + username + ",\n\n" +
               "Your staff account has been created successfully.\n\n" +
               "Login URL: " + loginUrl + "\n\n" +
               "Temporary Password: " + temporaryPassword + "\n\n" +
               "Please log in using this temporary password and change your password immediately.\n\n" +
               "Regards,\n" +
               "Crave House Development Team";
    }

    /*
     * Subject line for staff invite email.
     * Keeping this in a method makes it easy to change later
     * without touching the email sending logic.
     */
    public String getStaffInviteSubject() {
        return "Your Staff Account Credentials";
    }
}