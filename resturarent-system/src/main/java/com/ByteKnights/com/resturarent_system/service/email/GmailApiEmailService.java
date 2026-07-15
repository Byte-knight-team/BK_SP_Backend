package com.ByteKnights.com.resturarent_system.service.email;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * Sends Crave House transactional emails through the Gmail HTTPS API.
 *
 * This implementation does not use SMTP ports 25, 465, or 587.
 */
@Service
public class GmailApiEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailApiEmailService.class);

    private static final String UTF_8 = StandardCharsets.UTF_8.name();

    private final EmailTemplateService templateService;
    private final EmailTestingService emailTestingService;
    private final RestClient restClient;

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String senderEmail;
    private final String senderName;
    private final String tokenUrl;
    private final String sendUrl;

    /*
     * Gmail access tokens normally have short lifetimes.
     * We cache the current access token and automatically request another
     * one by using the refresh token when necessary.
     */
    private final Object tokenLock = new Object();

    private volatile String cachedAccessToken;
    private volatile Instant cachedAccessTokenExpiry = Instant.EPOCH;

    public GmailApiEmailService(
            EmailTemplateService templateService,
            EmailTestingService emailTestingService,

            @Value("${app.email.gmail.client-id:}") String clientId,

            @Value("${app.email.gmail.client-secret:}") String clientSecret,

            @Value("${app.email.gmail.refresh-token:}") String refreshToken,

            @Value("${app.email.gmail.sender-email:}") String senderEmail,

            @Value("${app.email.gmail.sender-name:Crave House}") String senderName,

            @Value("${app.email.gmail.token-url:https://oauth2.googleapis.com/token}") String tokenUrl,

            @Value("${app.email.gmail.send-url:https://gmail.googleapis.com/gmail/v1/users/me/messages/send}") String sendUrl) {
        this.templateService = templateService;
        this.emailTestingService = emailTestingService;

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.tokenUrl = tokenUrl;
        this.sendUrl = sendUrl;

        this.restClient = RestClient.builder().build();
    }

    @Override
    public void sendStaffInviteEmail(
            String toEmail,
            String username,
            String temporaryPassword) {
        String subject = templateService.getStaffInviteSubject();

        String plainText = templateService.buildStaffInviteEmailBody(
                username,
                temporaryPassword);

        String htmlContent = templateService.buildStaffInviteEmailHtml(
                username,
                temporaryPassword);

        sendEmail(
                toEmail,
                subject,
                plainText,
                htmlContent);
    }

    @Override
    public void sendCustomerPasswordResetEmail(
            String toEmail,
            String resetLink) {
        String subject = templateService.getCustomerPasswordResetSubject();

        String plainText = templateService.buildCustomerPasswordResetEmailBody(
                resetLink);

        String htmlContent = templateService.buildCustomerPasswordResetEmailHtml(
                resetLink);

        sendEmail(
                toEmail,
                subject,
                plainText,
                htmlContent);
    }

    @Override
    public void sendCustomerEmailVerification(
            String toEmail,
            String verificationLink) {
        String subject = templateService.getCustomerEmailVerificationSubject();

        String plainText = templateService.buildCustomerEmailVerificationBody(
                verificationLink);

        String htmlContent = templateService.buildCustomerEmailVerificationHtml(
                verificationLink);

        sendEmail(
                toEmail,
                subject,
                plainText,
                htmlContent);
    }

    @Override
    public void sendSimpleEmail(
            String toEmail,
            String subject,
            String body) {
        String htmlContent = templateService.buildSimpleEmailHtml(body);

        sendEmail(
                toEmail,
                subject,
                body,
                htmlContent);
    }

    /**
     * Creates and submits one email to Gmail.
     */
    private void sendEmail(
            String toEmail,
            String subject,
            String plainText,
            String htmlContent) {
        validateEmailAddress(toEmail);
        validateSubject(subject);

        if (emailTestingService.isForceFail()) {
            throw new IllegalStateException(
                    "Forced email failure for testing");
        }

        ensureGmailConfigurationExists();

        try {
            MimeMessage mimeMessage = createMimeMessage(
                    toEmail,
                    subject,
                    plainText,
                    htmlContent);

            String rawMessage = encodeMimeMessage(mimeMessage);

            GmailSendResponse response = submitMessage(rawMessage);

            if (response == null ||
                    response.id() == null ||
                    response.id().isBlank()) {

                throw new IllegalStateException(
                        "Gmail API returned no message ID");
            }

            /*
             * Do not log passwords or email bodies.
             */
            log.info(
                    "Email accepted by Gmail API. recipient={}, messageId={}, threadId={}",
                    toEmail,
                    response.id(),
                    response.threadId());
        } catch (RestClientResponseException exception) {
            String safeBody = sanitizeApiError(
                    exception.getResponseBodyAsString());

            throw new IllegalStateException(
                    "Gmail API request failed with HTTP "
                            + exception.getStatusCode().value()
                            + ": "
                            + safeBody,
                    exception);
        } catch (MessagingException | IOException exception) {
            throw new IllegalStateException(
                    "Failed to construct or encode Gmail message",
                    exception);
        }
    }

    /**
     * Creates a MIME message containing both plain text and HTML.
     */
    private MimeMessage createMimeMessage(
            String toEmail,
            String subject,
            String plainText,
            String htmlContent) throws MessagingException, IOException {

        Session session = Session.getInstance(new Properties());

        MimeMessage message = new MimeMessage(session);

        InternetAddress fromAddress = new InternetAddress(
                senderEmail,
                senderName,
                UTF_8);

        InternetAddress recipientAddress = new InternetAddress(toEmail, true);

        recipientAddress.validate();

        message.setFrom(fromAddress);
        message.setReplyTo(
                new Address[] { fromAddress });

        message.setRecipient(
                Message.RecipientType.TO,
                recipientAddress);

        message.setSubject(subject, UTF_8);
        message.setSentDate(new Date());

        /*
         * Indicates that this is an automatically generated
         * transactional system email.
         */
        message.setHeader(
                "Auto-Submitted",
                "auto-generated");

        MimeBodyPart plainTextPart = new MimeBodyPart();

        plainTextPart.setText(
                plainText == null ? "" : plainText,
                UTF_8);

        MimeBodyPart htmlPart = new MimeBodyPart();

        htmlPart.setContent(
                htmlContent == null ? "" : htmlContent,
                "text/html; charset=UTF-8");

        MimeMultipart alternativeContent = new MimeMultipart("alternative");

        alternativeContent.addBodyPart(plainTextPart);
        alternativeContent.addBodyPart(htmlPart);

        message.setContent(alternativeContent);
        message.saveChanges();

        return message;
    }

    /**
     * Converts the complete MIME message into the Base64URL format
     * required by Gmail API.
     */
    private String encodeMimeMessage(
            MimeMessage mimeMessage) throws MessagingException, IOException {

        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            mimeMessage.writeTo(outputStream);

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            outputStream.toByteArray());
        }
    }

    /**
     * Submits the encoded message.
     *
     * When Gmail rejects an expired access token with HTTP 401,
     * the cached token is cleared and the request is attempted once more.
     */
    private GmailSendResponse submitMessage(
            String rawMessage) {
        try {
            return executeSendRequest(
                    rawMessage,
                    getValidAccessToken());
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401) {
                clearCachedAccessToken();

                return executeSendRequest(
                        rawMessage,
                        getValidAccessToken());
            }

            throw exception;
        }
    }

    private GmailSendResponse executeSendRequest(
            String rawMessage,
            String accessToken) {
        return restClient.post()
                .uri(sendUrl)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(
                        Map.of(
                                "raw",
                                rawMessage))
                .retrieve()
                .body(GmailSendResponse.class);
    }

    /**
     * Returns the cached access token when still valid.
     * Otherwise obtains a new access token using the refresh token.
     */
    private String getValidAccessToken() {
        Instant now = Instant.now();

        if (cachedAccessToken != null &&
                now.isBefore(cachedAccessTokenExpiry)) {

            return cachedAccessToken;
        }

        synchronized (tokenLock) {
            now = Instant.now();

            if (cachedAccessToken != null &&
                    now.isBefore(cachedAccessTokenExpiry)) {

                return cachedAccessToken;
            }

            GoogleTokenResponse tokenResponse = requestNewAccessToken();

            if (tokenResponse == null ||
                    tokenResponse.accessToken() == null ||
                    tokenResponse.accessToken().isBlank()) {

                throw new IllegalStateException(
                        "Google OAuth returned no access token");
            }

            long expiresInSeconds = tokenResponse.expiresIn() == null
                    ? 3600L
                    : tokenResponse.expiresIn();

            /*
             * Expire the cached token one minute early.
             */
            long safeLifetime = Math.max(
                    60L,
                    expiresInSeconds - 60L);

            cachedAccessToken = tokenResponse.accessToken();

            cachedAccessTokenExpiry = Instant.now().plusSeconds(safeLifetime);

            return cachedAccessToken;
        }
    }

    /**
     * Exchanges the long-lived refresh token for a temporary access token.
     */
    private GoogleTokenResponse requestNewAccessToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", refreshToken);
        formData.add("grant_type", "refresh_token");

        return restClient.post()
                .uri(tokenUrl)
                .contentType(
                        MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(formData)
                .retrieve()
                .body(GoogleTokenResponse.class);
    }

    private void clearCachedAccessToken() {
        synchronized (tokenLock) {
            cachedAccessToken = null;
            cachedAccessTokenExpiry = Instant.EPOCH;
        }
    }

    private void ensureGmailConfigurationExists() {
        requireConfigured(
                clientId,
                "GMAIL_CLIENT_ID");

        requireConfigured(
                clientSecret,
                "GMAIL_CLIENT_SECRET");

        requireConfigured(
                refreshToken,
                "GMAIL_REFRESH_TOKEN");

        requireConfigured(
                senderEmail,
                "GMAIL_SENDER_EMAIL");

        validateEmailAddress(senderEmail);
    }

    private void requireConfigured(
            String value,
            String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    variableName + " is not configured");
        }
    }

    private void validateEmailAddress(
            String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email address is required");
        }

        try {
            InternetAddress address = new InternetAddress(email, true);

            address.validate();
        } catch (MessagingException exception) {
            throw new IllegalArgumentException(
                    "Invalid email format: " + email,
                    exception);
        }
    }

    private void validateSubject(
            String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "Email subject is required");
        }

        /*
         * Prevent email-header injection.
         */
        if (subject.contains("\r") ||
                subject.contains("\n")) {

            throw new IllegalArgumentException(
                    "Email subject contains invalid characters");
        }
    }

    private String sanitizeApiError(
            String responseBody) {
        if (responseBody == null ||
                responseBody.isBlank()) {

            return "No response body";
        }

        String sanitized = responseBody.replaceAll(
                "[\\r\\n]+",
                " ");

        if (sanitized.length() > 500) {
            return sanitized.substring(0, 500);
        }

        return sanitized;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken,

            @JsonProperty("expires_in") Long expiresIn,

            @JsonProperty("token_type") String tokenType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GmailSendResponse(
            String id,
            String threadId,
            java.util.List<String> labelIds) {
    }
}