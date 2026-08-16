package com.ByteKnights.com.resturarent_system.customer.controller;

import com.ByteKnights.com.resturarent_system.controller.OrderController;
import com.ByteKnights.com.resturarent_system.dto.request.customer.PlaceOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.OrderPlacementResponse;
import com.ByteKnights.com.resturarent_system.exception.CheckoutException;
import com.ByteKnights.com.resturarent_system.exception.GlobalExceptionHandler;
import com.ByteKnights.com.resturarent_system.security.CustomerJwtService;
import com.ByteKnights.com.resturarent_system.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CustomerOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;
    
    @Mock
    private CustomerJwtService customerJwtService;

    @InjectMocks
    private OrderController orderController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testPlaceOrder_Success() throws Exception {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setOrderType("QR");
        request.setBranchId(1L);
        request.setTableId(10L);
        request.setQrSessionId(100L);
        
        PlaceOrderRequest.PlaceOrderItemRequest item = new PlaceOrderRequest.PlaceOrderItemRequest();
        item.setMenuItemId(1L);
        item.setQuantity(2);
        request.setItems(Collections.singletonList(item));

        OrderPlacementResponse mockResponse = OrderPlacementResponse.builder()
                .orderId(500L)
                .orderNumber("ORD-12345")
                .finalAmount(new java.math.BigDecimal("25.50"))
                .build();

        when(orderService.placeCustomerOrder(any(), any(PlaceOrderRequest.class))).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(() -> "customer@example.com")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order placed successfully!"))
                .andExpect(jsonPath("$.data.orderId").value(500))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-12345"));
    }

    @Test
    void testPlaceOrder_TableNotOccupied_Forbidden() throws Exception {
        // Arrange
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setOrderType("QR");
        request.setBranchId(1L);
        request.setTableId(10L);
        request.setQrSessionId(100L);
        request.setItems(Collections.emptyList());

        String errorMessage = "Orders can only be placed when the table is marked as OCCUPIED. Please wait for a staff member to seat you.";
        when(orderService.placeCustomerOrder(any(), any(PlaceOrderRequest.class)))
                .thenThrow(new CheckoutException(HttpStatus.FORBIDDEN, errorMessage));

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(() -> "customer@example.com")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(errorMessage));
    }
}
