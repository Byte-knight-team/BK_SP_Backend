package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.QrSessionStartRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.QrSessionStartResponseData;

public interface QrSessionService {
    QrSessionStartResponseData startSession(QrSessionStartRequest request);
}