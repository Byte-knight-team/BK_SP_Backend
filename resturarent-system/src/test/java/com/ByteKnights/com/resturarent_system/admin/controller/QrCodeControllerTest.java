package com.ByteKnights.com.resturarent_system.admin.controller;

import com.ByteKnights.com.resturarent_system.controller.QrCodeController;
import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.response.admin.QrCodeResponse;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.QrCodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrCodeControllerTest {

    @Mock
    private QrCodeService qrCodeService;

    @Mock
    private Authentication authentication;

    @Test
    void createQrCode_returnsCreatedResponseAndUsesAuthenticatedUserId() {
        QrCodeController controller = new QrCodeController(qrCodeService);
        Long tableId = 12L;
        Long actorUserId = 77L;

        User user = new User();
        user.setId(actorUserId);
        user.setEmail("admin@example.com");
        user.setPassword("secret");
        user.setIsActive(true);

        JwtUserPrincipal principal = new JwtUserPrincipal(user);
        QrCodeResponse qrCodeResponse = QrCodeResponse.builder().id(101L).tableId(tableId).build();

        when(authentication.getPrincipal()).thenReturn(principal);
        when(qrCodeService.createQrCode(tableId, actorUserId)).thenReturn(qrCodeResponse);

        ResponseEntity<ApiResponse<QrCodeResponse>> response = controller.createQrCode(tableId, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("QR code generated successfully", response.getBody().getMessage());
        assertSame(qrCodeResponse, response.getBody().getData());
        verify(qrCodeService).createQrCode(tableId, actorUserId);
    }
}