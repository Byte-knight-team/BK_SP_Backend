package com.ByteKnights.com.resturarent_system.service.email;

/*

 * - Defines what email actions our system supports.
 * - Other classes can call this interface without caring about how the email is sent.
 * - Later, if we change from Gmail SMTP to another email provider,
 *   we only need to change the implementation, not the staff service logic.
 */
public interface EmailService {

    void sendStaffInviteEmail(String toEmail, String username, String temporaryPassword);
}