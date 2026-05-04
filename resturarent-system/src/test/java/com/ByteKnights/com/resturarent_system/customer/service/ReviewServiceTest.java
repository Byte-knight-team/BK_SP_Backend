package com.ByteKnights.com.resturarent_system.customer.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.ReviewSubmissionRequest;
import com.ByteKnights.com.resturarent_system.dto.response.ReviewResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.exception.CheckoutException;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.CustomerRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderItemRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.ReviewRepository;
import com.ByteKnights.com.resturarent_system.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User customerUser;
    private Customer customer;
    private Order servedOrder;

    @BeforeEach
    void setUp() {
        customerUser = buildCustomerUser(10L, "Chamari Perera", "94770000001", "chamari@test.com");
        customer = buildCustomer(20L, customerUser);
        servedOrder = buildOrder(30L, customer, OrderStatus.SERVED);
    }

    @Test
    void getRecentReviews_shouldReturnOnlyThreeReviewsInDescendingOrder() {
        // Arrange
        Review olderReview = buildReview(1L, "Older review", 4, LocalDateTime.of(2026, 5, 1, 10, 0));
        Review middleReview = buildReview(2L, "Middle review", 5, LocalDateTime.of(2026, 5, 2, 10, 0));
        Review newestReview = buildReview(3L, "Newest review", 3, LocalDateTime.of(2026, 5, 3, 10, 0));

        when(reviewRepository.findRecentOrderReviews(PageRequest.of(0, 3)))
                .thenReturn(List.of(newestReview, middleReview, olderReview));

        // Act
        List<ReviewResponse> result = reviewService.getRecentReviews();

        // Assert
        assertEquals(3, result.size());
        assertEquals(3L, result.get(0).getReviewId());
        assertEquals(2L, result.get(1).getReviewId());
        assertEquals(1L, result.get(2).getReviewId());
        assertEquals("Newest review", result.get(0).getComment());

        verify(reviewRepository, times(1)).findRecentOrderReviews(PageRequest.of(0, 3));
    }

    @Test
    void submitReview_shouldSaveOrderReview_whenOrderIsServedAndNoDuplicateExists() {
        // Arrange
        ReviewSubmissionRequest.OrderReviewRequest orderReview = new ReviewSubmissionRequest.OrderReviewRequest();
        orderReview.setRating(5);
        orderReview.setComment("Great food and service");

        ReviewSubmissionRequest request = new ReviewSubmissionRequest();
        request.setOrderReview(orderReview);

        when(customerRepository.findByUserPhone("94770000001")).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomerId(30L, 20L)).thenReturn(Optional.of(servedOrder));
        when(reviewRepository.existsByOrderAndOrderItemIsNull(servedOrder)).thenReturn(false);

        // Act
        reviewService.submitReview("94770000001", 30L, request);

        // Assert
        verify(reviewRepository, times(1)).save(argThat(review ->
                review.getCustomer().equals(customer)
                        && review.getOrder().equals(servedOrder)
                        && review.getRating().equals(5)
                        && "Great food and service".equals(review.getComment())
                        && review.getOrderItem() == null
        ));
        verify(orderItemRepository, never()).findById(anyLong());
    }

    @Test
    void submitReview_shouldThrowWhenOrderAlreadyReviewed() {
        // Arrange
        ReviewSubmissionRequest.OrderReviewRequest orderReview = new ReviewSubmissionRequest.OrderReviewRequest();
        orderReview.setRating(4);
        orderReview.setComment("Already reviewed");

        ReviewSubmissionRequest request = new ReviewSubmissionRequest();
        request.setOrderReview(orderReview);

        when(customerRepository.findByUserPhone("94770000001")).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomerId(30L, 20L)).thenReturn(Optional.of(servedOrder));
        when(reviewRepository.existsByOrderAndOrderItemIsNull(servedOrder)).thenReturn(true);

        // Act + Assert
        CheckoutException exception = assertThrows(
                CheckoutException.class,
                () -> reviewService.submitReview("94770000001", 30L, request)
        );

        assertEquals("An order review already exists.", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void submitReview_shouldThrowWhenOrderIsNotServed() {
        // Arrange
        Order pendingOrder = buildOrder(31L, customer, OrderStatus.PLACED);

        ReviewSubmissionRequest.OrderReviewRequest orderReview = new ReviewSubmissionRequest.OrderReviewRequest();
        orderReview.setRating(5);
        orderReview.setComment("Too early");

        ReviewSubmissionRequest request = new ReviewSubmissionRequest();
        request.setOrderReview(orderReview);

        when(customerRepository.findByUserPhone("94770000001")).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomerId(31L, 20L)).thenReturn(Optional.of(pendingOrder));

        // Act + Assert
        CheckoutException exception = assertThrows(
                CheckoutException.class,
                () -> reviewService.submitReview("94770000001", 31L, request)
        );

        assertEquals("You can only review completed orders.", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void submitReview_shouldThrowWhenCustomerDoesNotExist() {
        // Arrange
        ReviewSubmissionRequest request = new ReviewSubmissionRequest();
        when(customerRepository.findByUserPhone("missing@test.com")).thenReturn(Optional.empty());
        when(customerRepository.findByUserEmail("missing@test.com")).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.submitReview("missing@test.com", 30L, request)
        );

        assertEquals("Customer not found", exception.getMessage());
        verify(orderRepository, never()).findByIdAndCustomerId(anyLong(), anyLong());
    }

    private User buildCustomerUser(Long id, String fullName, String phone, String email) {
        Role role = Role.builder().id(100L).name("CUSTOMER").build();
        return User.builder()
                .id(id)
                .fullName(fullName)
                .username(phone)
                .password("password")
                .email(email)
                .phone(phone)
                .role(role)
                .isActive(true)
                .passwordChanged(true)
                .build();
    }

    private Customer buildCustomer(Long id, User user) {
        return Customer.builder()
                .id(id)
                .user(user)
                .loyaltyPoints(0)
                .totalSpent(BigDecimal.ZERO)
                .emailVerified(true)
                .phoneVerified(true)
                .build();
    }

    private Order buildOrder(Long id, Customer owner, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setCustomer(owner);
        order.setStatus(status);
        order.setOrderNumber("ORD-" + id);
        order.setTotalAmount(BigDecimal.valueOf(1000));
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    private Review buildReview(Long id, String comment, Integer rating, LocalDateTime createdAt) {
        return Review.builder()
                .id(id)
                .customer(customer)
                .order(servedOrder)
                .rating(rating)
                .comment(comment)
                .createdAt(createdAt)
                .build();
    }
}