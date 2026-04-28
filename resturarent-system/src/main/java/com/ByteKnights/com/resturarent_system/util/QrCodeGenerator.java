package com.ByteKnights.com.resturarent_system.util;

import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility for producing QR code PNG bytes from an arbitrary text payload.
 *
 * Responsibilities:
 * - Encode the provided string as a QR code using ZXing.
 * - Render the QR code to a PNG byte array suitable for HTTP responses
 *   or embedding in JSON as base64.
 *
 * Notes:
 * - This class performs only rendering. It does NOT create or validate
 *   any application-level token or expiry semantics — that is handled
 *   upstream by services that build the QR payload (e.g. `QrCodeServiceImpl`).
 * - Failures are converted into a runtime `InvalidOperationException` so
 *   callers can handle QR generation problems uniformly.
 */
public final class QrCodeGenerator {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;

    private QrCodeGenerator() {
    }

    /**
     * Generate a PNG-encoded QR code for the given text.
     *
     * @param text the text to encode (typically a URL containing a short-lived JWT)
     * @return PNG bytes of the generated QR image
     * @throws InvalidOperationException when QR generation fails
     */
    public static byte[] generateQRCodeImage(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, WIDTH, HEIGHT);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new InvalidOperationException("Failed to generate QR code image");
        }
    }
}