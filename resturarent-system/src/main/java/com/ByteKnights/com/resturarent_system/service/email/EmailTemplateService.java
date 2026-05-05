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
}