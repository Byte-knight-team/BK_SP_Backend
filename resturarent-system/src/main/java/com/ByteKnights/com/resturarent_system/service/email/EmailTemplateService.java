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
                                + "Crave House staff account for you.</p>"

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

        /*
         * Order Lifecycle Emails
         */
        public String buildOrderPlacedHtml(String orderNumber, String branchName, String itemsSummary, java.math.BigDecimal finalAmount, String orderType, String paymentMethod) {
                String content = "<p>Hello,</p>"
                                + "<p>We have received your order <strong>" + escapeHtml(orderNumber) + "</strong> at " + escapeHtml(branchName) + ".</p>"
                                + "<p><strong>Order Type:</strong> " + escapeHtml(orderType) + "</p>"
                                + "<div style=\"margin:20px 0; padding:15px; background:#f4f4f5; border-radius:6px;\">"
                                + "<h3 style=\"margin-top:0; margin-bottom:10px; font-size:16px;\">Order Details</h3>"
                                + "<div style=\"font-family:monospace; white-space:pre-wrap; color:#3f3f46;\">" + escapeHtml(itemsSummary) + "</div>"
                                + "<p style=\"margin-bottom:0; margin-top:15px; font-size:16px;\"><strong>Total: Rs. " + finalAmount + "</strong></p>"
                                + "</div>";
                
                if ("CARD".equalsIgnoreCase(paymentMethod)) {
                    content += "<p><strong>Note:</strong> Since your payment method is CARD, order preparing will start after successful payment.</p>";
                } else {
                    content += "<p>We will start preparing your order shortly!</p>";
                }

                return buildLayout("Order Placed — " + escapeHtml(orderNumber), content);
        }

        public String buildOrderCancelledHtml(String orderNumber, String branchName, String cancelReason) {
                String content = "<p>Hello,</p>"
                                + "<p>Your order <strong>" + escapeHtml(orderNumber) + "</strong> at " + escapeHtml(branchName) + " has been cancelled.</p>"
                                + "<div style=\"margin:20px 0; padding:15px; background:#fef2f2; border-left:4px solid #ef4444; border-radius:4px;\">"
                                + "<p style=\"margin:0; color:#b91c1c;\"><strong>Reason:</strong> " + escapeHtml(cancelReason) + "</p>"
                                + "</div>"
                                + "<p>If you have already paid, a refund will be processed according to our policy.</p>";

                return buildLayout("Order Cancelled — " + escapeHtml(orderNumber), content);
        }

        public String buildOrderServedHtml(String orderNumber, String branchName, java.math.BigDecimal finalAmount) {
                String content = "<p>Hello,</p>"
                                + "<p>Your order <strong>" + escapeHtml(orderNumber) + "</strong> at " + escapeHtml(branchName) + " is complete!</p>"
                                + "<p>The final total was <strong>Rs. " + finalAmount + "</strong>.</p>"
                                + "<p>We hope you enjoyed your meal. We would love to hear your feedback on the items you ordered. You can leave a review from your past orders page.</p>"
                                + "<p>Thank you for choosing Crave House!</p>";

                return buildLayout("Order Complete — " + escapeHtml(orderNumber), content);
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
                                + "<strong>Crave House Team</strong>"
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