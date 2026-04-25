package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.request.customer.QrSessionStartRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.QrSessionStartResponseData;
import com.ByteKnights.com.resturarent_system.service.QrSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/qr-sessions")
@CrossOrigin
public class QrSessionController {

    private final QrSessionService qrSessionService;

    public QrSessionController(QrSessionService qrSessionService) {
        this.qrSessionService = qrSessionService;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<QrSessionStartResponseData>> start(@RequestBody QrSessionStartRequest request) {
        QrSessionStartResponseData responseData = qrSessionService.startSession(request);
        return ResponseEntity.ok(ApiResponse.success("QR session started successfully.", responseData));
    }
}