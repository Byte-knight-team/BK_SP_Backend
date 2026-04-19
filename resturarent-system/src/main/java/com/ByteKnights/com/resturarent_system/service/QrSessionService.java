package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.QrSessionStartRequest;
import com.ByteKnights.com.resturarent_system.dto.response.QrSessionStartResponseData;

public interface QrSessionService {
    QrSessionStartResponseData startSession(QrSessionStartRequest request);
}