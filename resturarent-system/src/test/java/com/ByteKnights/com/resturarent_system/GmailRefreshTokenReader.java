package com.ByteKnights.com.resturarent_system;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;

/**
 * Reads the Gmail OAuth refresh token created by GmailApiLocalTest.
 *
 * The token is copied to the Windows clipboard instead of being
 * displayed in the terminal.
 */
public class GmailRefreshTokenReader {

    private static final File TOKEN_DIRECTORY =
            new File(".secrets/gmail-tokens");

    private static final String CREDENTIAL_KEY =
            "servesync-system-email";

    public static void main(String[] args) {
        try {
            FileDataStoreFactory dataStoreFactory =
                    new FileDataStoreFactory(TOKEN_DIRECTORY);

            DataStore<StoredCredential> credentialStore =
                    StoredCredential.getDefaultDataStore(
                            dataStoreFactory
                    );

            StoredCredential credential =
                    credentialStore.get(CREDENTIAL_KEY);

            if (credential == null) {
                throw new IllegalStateException(
                        "No Gmail credential was found for: "
                                + CREDENTIAL_KEY
                );
            }

            String refreshToken =
                    credential.getRefreshToken();

            if (refreshToken == null ||
                    refreshToken.isBlank()) {

                throw new IllegalStateException(
                        "The stored Gmail credential does not contain "
                                + "a refresh token."
                );
            }

            StringSelection selection =
                    new StringSelection(refreshToken);

            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(selection, null);

            System.out.println(
                    "Gmail refresh token copied to clipboard."
            );

            System.out.println(
                    "Paste it into GMAIL_REFRESH_TOKEN "
                            + "inside .env.properties."
            );
        }
        catch (Exception exception) {
            System.err.println(
                    "Could not read the Gmail refresh token."
            );

            exception.printStackTrace();
        }
    }
}