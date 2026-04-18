package com.ByteKnights.com.resturarent_system.service.email;

public interface EmailService {
    void sendStaffInviteEmail(String toEmail, String username, String temporaryPassword);
}
