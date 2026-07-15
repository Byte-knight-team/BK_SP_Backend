package com.ByteKnights.com.resturarent_system;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/**
 * Standalone local test for Gmail API.
 *
 * This class does not start the Spring Boot application.
 * It authorizes one Gmail account and sends one test email.
 */
public class GmailApiLocalTest {

    private static final String APPLICATION_NAME =
            "ServeSync Gmail API Local Test";

    private static final JsonFactory JSON_FACTORY =
            GsonFactory.getDefaultInstance();

    private static final File CREDENTIALS_FILE =
            new File(".secrets/gmail-oauth-client.json");

    private static final File TOKENS_DIRECTORY =
            new File(".secrets/gmail-tokens");

    private static final List<String> SCOPES =
            List.of(GmailScopes.GMAIL_SEND);

    private static final String SENDER_EMAIL =
            "cravehouse.system.dev@gmail.com";

    private static final String RECEIVER_EMAIL =
            "ashenrandira2021@gmail.com";

    private static Credential authorize(
            NetHttpTransport httpTransport
    ) throws Exception {

        if (!CREDENTIALS_FILE.exists()) {
            throw new IllegalStateException(
                    "OAuth file not found: "
                            + CREDENTIALS_FILE.getAbsolutePath()
            );
        }

        GoogleClientSecrets clientSecrets;

        try (
                FileInputStream inputStream =
                        new FileInputStream(CREDENTIALS_FILE);
                InputStreamReader reader =
                        new InputStreamReader(
                                inputStream,
                                StandardCharsets.UTF_8
                        )
        ) {
            clientSecrets =
                    GoogleClientSecrets.load(JSON_FACTORY, reader);
        }

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport,
                        JSON_FACTORY,
                        clientSecrets,
                        SCOPES
                )
                        .setDataStoreFactory(
                                new FileDataStoreFactory(TOKENS_DIRECTORY)
                        )
                        .setAccessType("offline")
                        .build();

        LocalServerReceiver receiver =
                new LocalServerReceiver.Builder()
                        .setPort(8888)
                        .build();

        return new AuthorizationCodeInstalledApp(
                flow,
                receiver
        ).authorize("servesync-system-email");
    }

    private static MimeMessage createEmail() throws Exception {
        Properties properties = new Properties();
        Session session = Session.getDefaultInstance(properties);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(
                new InternetAddress(
                        SENDER_EMAIL,
                        "ServeSync"
                )
        );

        email.addRecipient(
                jakarta.mail.Message.RecipientType.TO,
                new InternetAddress(RECEIVER_EMAIL)
        );

        email.setSubject(
                "ServeSync Gmail API Local Test",
                StandardCharsets.UTF_8.name()
        );

        String htmlContent = """
                <html>
                    <body style="font-family: Arial, sans-serif;">
                        <h2>ServeSync Gmail API Test</h2>
                        <p>
                            This email was sent through the Gmail HTTPS API.
                        </p>
                        <p>
                            Render SMTP ports were not used.
                        </p>
                    </body>
                </html>
                """;

        email.setContent(htmlContent, "text/html; charset=UTF-8");

        return email;
    }

    private static Message convertToGmailMessage(
            MimeMessage email
    ) throws Exception {

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        email.writeTo(outputStream);

        String encodedEmail =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(outputStream.toByteArray());

        return new Message().setRaw(encodedEmail);
    }

    public static void main(String[] args) {
        try {
            NetHttpTransport httpTransport =
                    GoogleNetHttpTransport.newTrustedTransport();

            Credential credential = authorize(httpTransport);

            Gmail gmail = new Gmail.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    credential
            )
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            MimeMessage mimeMessage = createEmail();

            Message gmailMessage =
                    convertToGmailMessage(mimeMessage);

            Message sentMessage =
                    gmail.users()
                            .messages()
                            .send("me", gmailMessage)
                            .execute();

            System.out.println("Gmail API accepted the email.");
            System.out.println(
                    "Gmail message ID: " + sentMessage.getId()
            );
            System.out.println(
                    "Thread ID: " + sentMessage.getThreadId()
            );
        }
        catch (Exception exception) {
            System.err.println("Gmail API test failed.");
            exception.printStackTrace();
        }
    }
}