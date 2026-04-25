package com.ByteKnights.com.resturarent_system.service.email;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String buildStaffInviteEmailBody(String username, String temporaryPassword) {
        // Local frontend login URL for staff users.
        // Later, when deploying, replace this with the real hosted frontend URL.
        String loginUrl = "http://localhost:5173/staff/login";
    
        // Email body sent to newly created staff members.
        return "Hello " + username + ",\n\n" +
               "Your staff account has been created successfully.\n\n" +
               "Login URL: " + loginUrl + "\n\n" +
               "Temporary Password: " + temporaryPassword + "\n\n" +
               "Please log in using this temporary password and change your password immediately.\n\n" +
               "Regards,\n" +
               "Crave House Development Team";
    }

    public String getStaffInviteSubject() {
        return "Your Staff Account Credentials";
    }
}