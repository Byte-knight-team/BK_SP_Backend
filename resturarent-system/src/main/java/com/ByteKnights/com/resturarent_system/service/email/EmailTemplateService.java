package com.ByteKnights.com.resturarent_system.service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

        @Value("${app.frontend.staff-login-url}")
        private String staffLoginUrl;

        /*
         * Staff invitation
         */
        public String getStaffInviteSubject() {
                return "Your Crave House staff account is ready";
        }

        public String buildStaffInviteEmailBody(
                        String username,
                        String temporaryPassword) {
                return "Hello " + username + ",\n\n"
                                + "An administrator has created a Crave House staff account for you.\n\n"
                                + "Staff login page:\n"
                                + staffLoginUrl + "\n\n"
                                + "Temporary password:\n"
                                + temporaryPassword + "\n\n"
                                + "For your security, sign in and change this temporary password immediately.\n"
                                + "Do not share your password with anyone.\n\n"
                                + "If you were not expecting this account, please contact the restaurant administrator.\n\n"
                                + "Regards,\n"
                                + "Crave House Team";
        }

        public String buildStaffInviteEmailHtml(
                        String username,
                        String temporaryPassword) {
                String content = "<p>Hello <strong>"
                                + escapeHtml(username)
                                + "</strong>,</p>"

                                + "<p>An administrator has created a "
                                + "ServeSync staff account for you.</p>"

                                + "<div style=\"margin:24px 0;\">"
                                + "<a href=\""
                                + escapeHtml(staffLoginUrl)
                                + "\" style=\""
                                + "display:inline-block;"
                                + "padding:12px 20px;"
                                + "background:#d97706;"
                                + "color:#ffffff;"
                                + "text-decoration:none;"
                                + "border-radius:6px;"
                                + "font-weight:600;"
                                + "\">Open staff login</a>"
                                + "</div>"

                                + "<p>Your temporary password is:</p>"

                                + "<div style=\""
                                + "padding:14px;"
                                + "background:#f4f4f5;"
                                + "border:1px solid #e4e4e7;"
                                + "border-radius:6px;"
                                + "font-family:monospace;"
                                + "font-size:18px;"
                                + "letter-spacing:1px;"
                                + "\">"
                                + escapeHtml(temporaryPassword)
                                + "</div>"

                                + "<p style=\"margin-top:20px;\">"
                                + "For your security, sign in and change this "
                                + "temporary password immediately. "
                                + "Do not share your password with anyone."
                                + "</p>"

                                + "<p>If you were not expecting this account, "
                                + "please contact the restaurant administrator.</p>";

                return buildLayout(
                                "Your Crave House staff account is ready",
                                content);
        }

        /*
         * Customer password reset
         */
        public String getCustomerPasswordResetSubject() {
                return "Reset your CraveHouse password";
        }

        public String buildCustomerPasswordResetEmailBody(
                        String resetLink) {
                return "Hello,\n\n"
                                + "We received a request to reset the password for your CraveHouse customer account.\n\n"
                                + "Use the following link to set a new password:\n"
                                + resetLink + "\n\n"
                                + "This link is valid for 15 minutes.\n\n"
                                + "If you did not request a password reset, you can ignore this email.\n\n"
                                + "Regards,\n"
                                + "Crave House Team";
        }

        public String buildCustomerPasswordResetEmailHtml(
                        String resetLink) {
                String content = "<p>Hello,</p>"

                                + "<p>We received a request to reset the password "
                                + "for your CraveHouse customer account.</p>"

                                + "<div style=\"margin:24px 0;\">"
                                + "<a href=\""
                                + escapeHtml(resetLink)
                                + "\" style=\""
                                + "display:inline-block;"
                                + "padding:12px 20px;"
                                + "background:#d97706;"
                                + "color:#ffffff;"
                                + "text-decoration:none;"
                                + "border-radius:6px;"
                                + "font-weight:600;"
                                + "\">Reset password</a>"
                                + "</div>"

                                + "<p>This link is valid for 15 minutes.</p>"

                                + "<p>If you did not request a password reset, "
                                + "you can safely ignore this email.</p>";

                return buildLayout(
                                "Reset your password",
                                content);
        }

        /*
         * Customer email verification
         */
        public String getCustomerEmailVerificationSubject() {
                return "Verify your CraveHouse email address";
        }

        public String buildCustomerEmailVerificationBody(
                        String verificationLink) {
                return "Hello,\n\n"
                                + "Please verify your email address for your CraveHouse customer account.\n\n"
                                + "Verification link:\n"
                                + verificationLink + "\n\n"
                                + "This link is valid for 24 hours.\n\n"
                                + "If you did not create this account, you can ignore this email.\n\n"
                                + "Regards,\n"
                                + "Crave House Team";
        }

        public String buildCustomerEmailVerificationHtml(
                        String verificationLink) {
                String content = "<p>Hello,</p>"

                                + "<p>Please verify your email address for your "
                                + "CraveHouse customer account.</p>"

                                + "<div style=\"margin:24px 0;\">"
                                + "<a href=\""
                                + escapeHtml(verificationLink)
                                + "\" style=\""
                                + "display:inline-block;"
                                + "padding:12px 20px;"
                                + "background:#d97706;"
                                + "color:#ffffff;"
                                + "text-decoration:none;"
                                + "border-radius:6px;"
                                + "font-weight:600;"
                                + "\">Verify email address</a>"
                                + "</div>"

                                + "<p>This link is valid for 24 hours.</p>"

                                + "<p>If you did not create this account, "
                                + "you can safely ignore this email.</p>";

                return buildLayout(
                                "Verify your email address",
                                content);
        }

        /*
         * Generic system email
         */
        public String buildSimpleEmailHtml(
                        String body) {
                String safeBody = escapeHtml(body == null ? "" : body)
                                .replace(
                                                "\r\n",
                                                "<br>")
                                .replace(
                                                "\n",
                                                "<br>");

                return buildLayout(
                                "Crave House notification",
                                "<p>" + safeBody + "</p>");
        }

        private String buildLayout(
                        String title,
                        String content) {
                return "<!DOCTYPE html>"
                                + "<html>"
                                + "<head>"
                                + "<meta charset=\"UTF-8\">"
                                + "<meta name=\"viewport\" "
                                + "content=\"width=device-width, initial-scale=1.0\">"
                                + "<title>"
                                + escapeHtml(title)
                                + "</title>"
                                + "</head>"

                                + "<body style=\""
                                + "margin:0;"
                                + "padding:0;"
                                + "background:#f5f5f5;"
                                + "font-family:Arial,sans-serif;"
                                + "color:#27272a;"
                                + "\">"

                                + "<div style=\""
                                + "max-width:600px;"
                                + "margin:30px auto;"
                                + "padding:0 16px;"
                                + "\">"

                                + "<div style=\""
                                + "background:#ffffff;"
                                + "border:1px solid #e4e4e7;"
                                + "border-radius:10px;"
                                + "overflow:hidden;"
                                + "\">"

                                + "<div style=\""
                                + "padding:22px 28px;"
                                + "background:#18181b;"
                                + "color:#ffffff;"
                                + "\">"

                                + "<div style=\"font-size:22px;font-weight:700;\">"
                                + "Crave House"
                                + "</div>"

                                + "<div style=\""
                                + "font-size:13px;"
                                + "margin-top:4px;"
                                + "color:#d4d4d8;"
                                + "\">"
                                + "Restaurant Management System"
                                + "</div>"

                                + "</div>"

                                + "<div style=\""
                                + "padding:28px;"
                                + "font-size:15px;"
                                + "line-height:1.6;"
                                + "\">"

                                + "<h2 style=\""
                                + "margin-top:0;"
                                + "font-size:21px;"
                                + "color:#18181b;"
                                + "\">"
                                + escapeHtml(title)
                                + "</h2>"

                                + content

                                + "<p style=\"margin-top:28px;\">"
                                + "Regards,<br>"
                                + "<strong>ServeSync Team</strong>"
                                + "</p>"

                                + "</div>"
                                + "</div>"

                                + "<p style=\""
                                + "text-align:center;"
                                + "font-size:12px;"
                                + "color:#71717a;"
                                + "margin-top:16px;"
                                + "\">"
                                + "This is an automated transactional email from Crave House."
                                + "</p>"

                                + "</div>"
                                + "</body>"
                                + "</html>";
        }

        private String escapeHtml(
                        String value) {
                if (value == null) {
                        return "";
                }

                return value
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&#39;");
        }
}