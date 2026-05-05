package com.ByteKnights.com.resturarent_system.service.email;

/*

 * EmailService defines the email actions used by the system.
 */
public interface EmailService {

    void sendStaffInviteEmail(String toEmail, String username, String temporaryPassword);
}