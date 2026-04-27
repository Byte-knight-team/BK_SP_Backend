package com.ByteKnights.com.resturarent_system.governance.controller;

import com.ByteKnights.com.resturarent_system.controller.SystemConfigController;
import com.ByteKnights.com.resturarent_system.dto.request.superadmin.UpdateBranchConfigRequest;
import com.ByteKnights.com.resturarent_system.dto.request.superadmin.UpdateGlobalConfigRequest;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.BranchConfigResponse;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.EffectiveBranchConfigResponse;
import com.ByteKnights.com.resturarent_system.dto.response.superadmin.GlobalConfigResponse;
import com.ByteKnights.com.resturarent_system.service.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone controller-layer tests for SystemConfigController.
 *
 * These tests cover the API layer for:
 * - global tax/service charge/loyalty config
 * - branch delivery/pickup/dine-in config
 * - effective config response
 *
 * Operating hours and order cancellation window are intentionally not tested here
 * because they are outside the simplified review focus.
 */
@ExtendWith(MockitoExtension.class)
class SystemConfigControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        SystemConfigController systemConfigController = new SystemConfigController(systemConfigService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(systemConfigController)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void getGlobalConfig_shouldReturnGlobalConfigResponse() throws Exception {
        // Arrange
        GlobalConfigResponse response = buildGlobalConfigResponse();

        when(systemConfigService.getGlobalConfig()).thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/api/admin/config/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taxEnabled").value(true))
                .andExpect(jsonPath("$.taxPercentage").value(10.00))
                .andExpect(jsonPath("$.serviceChargeEnabled").value(true))
                .andExpect(jsonPath("$.serviceChargePercentage").value(5.00))
                .andExpect(jsonPath("$.loyaltyEnabled").value(true))
                .andExpect(jsonPath("$.pointsPerAmount").value(1.00))
                .andExpect(jsonPath("$.amountPerPoint").value(100.00))
                .andExpect(jsonPath("$.minPointsToRedeem").value(50))
                .andExpect(jsonPath("$.valuePerPoint").value(2.00));

        verify(systemConfigService, times(1)).getGlobalConfig();
    }

    @Test
    void updateGlobalConfig_shouldReturnUpdatedGlobalConfigResponse() throws Exception {
        // Arrange
        UpdateGlobalConfigRequest request = buildUpdateGlobalConfigRequest();
        GlobalConfigResponse response = buildGlobalConfigResponse();

        when(systemConfigService.updateGlobalConfig(any(UpdateGlobalConfigRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(put("/api/admin/config/global")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taxEnabled").value(true))
                .andExpect(jsonPath("$.taxPercentage").value(10.00))
                .andExpect(jsonPath("$.serviceChargeEnabled").value(true))
                .andExpect(jsonPath("$.serviceChargePercentage").value(5.00))
                .andExpect(jsonPath("$.loyaltyEnabled").value(true))
                .andExpect(jsonPath("$.pointsPerAmount").value(1.00))
                .andExpect(jsonPath("$.amountPerPoint").value(100.00))
                .andExpect(jsonPath("$.minPointsToRedeem").value(50))
                .andExpect(jsonPath("$.valuePerPoint").value(2.00));

        verify(systemConfigService, times(1)).updateGlobalConfig(any(UpdateGlobalConfigRequest.class));
    }

    @Test
    void getBranchConfig_shouldReturnBranchConfigResponse() throws Exception {
        // Arrange
        BranchConfigResponse response = buildBranchConfigResponse();

        when(systemConfigService.getBranchConfig(2L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/api/admin/config/branches/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.branchId").value(2))
                .andExpect(jsonPath("$.branchName").value("Branch 02"))
                .andExpect(jsonPath("$.deliveryFee").value(8.50))
                .andExpect(jsonPath("$.deliveryEnabled").value(true))
                .andExpect(jsonPath("$.pickupEnabled").value(true))
                .andExpect(jsonPath("$.dineInEnabled").value(true))
                .andExpect(jsonPath("$.branchActiveForOrders").value(true));

        verify(systemConfigService, times(1)).getBranchConfig(2L);
    }

    @Test
    void updateBranchConfig_shouldReturnUpdatedBranchConfigResponse() throws Exception {
        // Arrange
        UpdateBranchConfigRequest request = new UpdateBranchConfigRequest();
        request.setDeliveryFee(new BigDecimal("12.00"));
        request.setDeliveryEnabled(false);
        request.setPickupEnabled(true);
        request.setDineInEnabled(true);
        request.setBranchActiveForOrders(false);

        BranchConfigResponse response = buildBranchConfigResponse();
        response.setDeliveryFee(new BigDecimal("12.00"));
        response.setDeliveryEnabled(false);
        response.setBranchActiveForOrders(false);

        when(systemConfigService.updateBranchConfig(eq(2L), any(UpdateBranchConfigRequest.class))).thenReturn(response);

        // Act + Assert
        mockMvc.perform(put("/api/admin/config/branches/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.branchId").value(2))
                .andExpect(jsonPath("$.deliveryFee").value(12.00))
                .andExpect(jsonPath("$.deliveryEnabled").value(false))
                .andExpect(jsonPath("$.pickupEnabled").value(true))
                .andExpect(jsonPath("$.dineInEnabled").value(true))
                .andExpect(jsonPath("$.branchActiveForOrders").value(false));

        verify(systemConfigService, times(1)).updateBranchConfig(eq(2L), any(UpdateBranchConfigRequest.class));
    }

    @Test
    void getEffectiveBranchConfig_shouldReturnCombinedEffectiveConfigResponse() throws Exception {
        // Arrange
        EffectiveBranchConfigResponse response = buildEffectiveBranchConfigResponse();

        when(systemConfigService.getEffectiveBranchConfig(2L)).thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/api/admin/config/branches/2/effective"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId").value(2))
                .andExpect(jsonPath("$.branchName").value("Branch 02"))
                .andExpect(jsonPath("$.taxEnabled").value(true))
                .andExpect(jsonPath("$.taxPercentage").value(10.00))
                .andExpect(jsonPath("$.serviceChargeEnabled").value(true))
                .andExpect(jsonPath("$.serviceChargePercentage").value(5.00))
                .andExpect(jsonPath("$.loyaltyEnabled").value(true))
                .andExpect(jsonPath("$.pointsPerAmount").value(1.00))
                .andExpect(jsonPath("$.amountPerPoint").value(100.00))
                .andExpect(jsonPath("$.minPointsToRedeem").value(50))
                .andExpect(jsonPath("$.valuePerPoint").value(2.00))
                .andExpect(jsonPath("$.deliveryFee").value(8.50))
                .andExpect(jsonPath("$.deliveryEnabled").value(true))
                .andExpect(jsonPath("$.pickupEnabled").value(true))
                .andExpect(jsonPath("$.dineInEnabled").value(true))
                .andExpect(jsonPath("$.branchActiveForOrders").value(true));

        verify(systemConfigService, times(1)).getEffectiveBranchConfig(2L);
    }

    private UpdateGlobalConfigRequest buildUpdateGlobalConfigRequest() {
        UpdateGlobalConfigRequest request = new UpdateGlobalConfigRequest();
        request.setTaxEnabled(true);
        request.setTaxPercentage(new BigDecimal("10.00"));
        request.setServiceChargeEnabled(true);
        request.setServiceChargePercentage(new BigDecimal("5.00"));
        request.setLoyaltyEnabled(true);
        request.setPointsPerAmount(new BigDecimal("1.00"));
        request.setAmountPerPoint(new BigDecimal("100.00"));
        request.setMinPointsToRedeem(50);
        request.setValuePerPoint(new BigDecimal("2.00"));

        /*
         * Required by the DTO validation.
         * Not tested here because order cancel window is outside this simplified scope.
         */
        request.setOrderCancelWindowMinutes(0);

        return request;
    }

    private GlobalConfigResponse buildGlobalConfigResponse() {
        GlobalConfigResponse response = new GlobalConfigResponse();
        response.setId(1L);
        response.setTaxEnabled(true);
        response.setTaxPercentage(new BigDecimal("10.00"));
        response.setServiceChargeEnabled(true);
        response.setServiceChargePercentage(new BigDecimal("5.00"));
        response.setLoyaltyEnabled(true);
        response.setPointsPerAmount(new BigDecimal("1.00"));
        response.setAmountPerPoint(new BigDecimal("100.00"));
        response.setMinPointsToRedeem(50);
        response.setValuePerPoint(new BigDecimal("2.00"));
        response.setOrderCancelWindowMinutes(0);
        response.setUpdatedAt(LocalDateTime.of(2026, 4, 28, 10, 0));
        return response;
    }

    private BranchConfigResponse buildBranchConfigResponse() {
        BranchConfigResponse response = new BranchConfigResponse();
        response.setId(5L);
        response.setBranchId(2L);
        response.setBranchName("Branch 02");
        response.setDeliveryFee(new BigDecimal("8.50"));
        response.setDeliveryEnabled(true);
        response.setPickupEnabled(true);
        response.setDineInEnabled(true);
        response.setBranchActiveForOrders(true);
        response.setUpdatedAt(LocalDateTime.of(2026, 4, 28, 10, 0));
        return response;
    }

    private EffectiveBranchConfigResponse buildEffectiveBranchConfigResponse() {
        EffectiveBranchConfigResponse response = new EffectiveBranchConfigResponse();
        response.setBranchId(2L);
        response.setBranchName("Branch 02");

        response.setTaxEnabled(true);
        response.setTaxPercentage(new BigDecimal("10.00"));

        response.setServiceChargeEnabled(true);
        response.setServiceChargePercentage(new BigDecimal("5.00"));

        response.setLoyaltyEnabled(true);
        response.setPointsPerAmount(new BigDecimal("1.00"));
        response.setAmountPerPoint(new BigDecimal("100.00"));
        response.setMinPointsToRedeem(50);
        response.setValuePerPoint(new BigDecimal("2.00"));

        response.setOrderCancelWindowMinutes(0);

        response.setDeliveryFee(new BigDecimal("8.50"));
        response.setDeliveryEnabled(true);
        response.setPickupEnabled(true);
        response.setDineInEnabled(true);
        response.setBranchActiveForOrders(true);

        /*
         * Operating hours are not part of this simplified test focus.
         */
        response.setOperatingHours(List.of());

        return response;
    }
}