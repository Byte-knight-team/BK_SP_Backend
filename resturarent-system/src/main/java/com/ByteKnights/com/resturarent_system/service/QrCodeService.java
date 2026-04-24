package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.admin.QrCodeResponse;

public interface QrCodeService {

    QrCodeResponse createQrCode(Long tableId, Long actorUserId);

    QrCodeResponse revokeQrCode(Long qrCodeId, String revokedReason);

    QrCodeResponse regenerateQrCode(Long qrCodeId, Long actorUserId, String revokeReason);
}
