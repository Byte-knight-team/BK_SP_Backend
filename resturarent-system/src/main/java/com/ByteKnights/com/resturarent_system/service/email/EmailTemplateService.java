package com.ByteKnights.com.resturarent_system.service.email;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String buildStaffInviteEmailBody(String username, String temporaryPassword) {
        return "Hello " + username + ",\n\n" +
                "Your staff account has been created successfully.\n\n" +
                "Temporary Password: " + temporaryPassword + "\n\n" +
                "Please log in using this temporary password and change your password immediately.\n\n" +
                "Regards,\n" +
                "Restaurant Management System";
    }

    public String getStaffInviteSubject() {
        return "Your Staff Account Credentials";
    }
}
