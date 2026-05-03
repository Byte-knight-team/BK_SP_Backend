package com.ByteKnights.com.resturarent_system.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ByteKnights.com.resturarent_system.exception.CustomerAuthException;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmsService {

    @Value("${sms.textlk.url}")
    private String apiUrl;

    @Value("${sms.textlk.token}")
    private String apiToken;

    @Value("${sms.textlk.sender-id}")
    private String senderId;

    private final RestTemplate restTemplate;

    public SmsService() {
        this.restTemplate = new RestTemplate();
    }

    public void sendOtpSms(String recipientPhone, String otpCode) {
        try {
            // 1. Format Phone Number (Text.lk expects 947XXXXXXXX format, strip leading 0 if exists)
            String formattedPhone = recipientPhone.trim();
            if (formattedPhone.startsWith("0")) {
                formattedPhone = "94" + formattedPhone.substring(1);
            }

            // 2. Set Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.set("Authorization", "Bearer " + apiToken);

            // 3. Create Payload
            Map<String, String> payload = new HashMap<>();
            payload.put("recipient", formattedPhone);
            payload.put("sender_id", senderId);
            payload.put("type", "plain");
            payload.put("message", "Your Crave House verification code is: " + otpCode);

            // 4. Send Request
            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(apiUrl, request, String.class);

        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
            throw new CustomerAuthException(HttpStatus.INTERNAL_SERVER_ERROR, "SMS Service Error");
        }
    }
}
