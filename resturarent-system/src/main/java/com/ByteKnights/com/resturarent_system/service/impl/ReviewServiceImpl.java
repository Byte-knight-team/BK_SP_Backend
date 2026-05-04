package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.ReviewSubmissionRequest;
import com.ByteKnights.com.resturarent_system.dto.response.ReviewResponse;
import com.ByteKnights.com.resturarent_system.entity.Customer;
import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderItem;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.Review;
import com.ByteKnights.com.resturarent_system.exception.CheckoutException;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.CustomerRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderItemRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.ReviewRepository;
import com.ByteKnights.com.resturarent_system.service.ReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, OrderRepository orderRepository,
                             CustomerRepository customerRepository, OrderItemRepository orderItemRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public void submitReview(String userIdentifier, Long orderId, ReviewSubmissionRequest request) {
        Customer customer = customerRepository.findByUserPhone(userIdentifier)
                .orElseGet(() -> customerRepository.findByUserEmail(userIdentifier)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found")));

        Order order = orderRepository.findByIdAndCustomerId(orderId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or does not belong to user"));

        // Allow reviews for only SERVED
        if (order.getStatus() != OrderStatus.SERVED) {
            throw new CheckoutException(HttpStatus.BAD_REQUEST, "You can only review completed orders.");
        }

        // Process Order-level review
        if (request.getOrderReview() != null) {
            if (reviewRepository.existsByOrderAndOrderItemIsNull(order)) {
                throw new CheckoutException(HttpStatus.BAD_REQUEST, "An order review already exists.");
            }
            Review orderReview = Review.builder()
                    .customer(customer)
                    .order(order)
                    .rating(request.getOrderReview().getRating())
                    .comment(request.getOrderReview().getComment())
                    .build();
            reviewRepository.save(orderReview);
        }

        // Process Item-level reviews
        if (request.getItemReviews() != null && !request.getItemReviews().isEmpty()) {
            for (ReviewSubmissionRequest.ItemReviewRequest itemReq : request.getItemReviews()) {
                OrderItem item = orderItemRepository.findById(itemReq.getOrderItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));
                
                if (!item.getOrder().getId().equals(order.getId())) {
                    throw new CheckoutException(HttpStatus.BAD_REQUEST, "Item does not belong to this order.");
                }

                if (reviewRepository.existsByOrderItem(item)) {
                    continue; // Skip already reviewed items to be safe
                }

                Review itemReview = Review.builder()
                        .customer(customer)
                        .order(order)
                        .orderItem(item)
                        .rating(itemReq.getRating())
                        .comment(itemReq.getComment())
                        .build();
                reviewRepository.save(itemReview);
            }
        }
    }

    @Override
    public List<ReviewResponse> getRecentReviews() {
        //fetch last three recent order reviews with comments ad build response
        return reviewRepository.findRecentOrderReviews(PageRequest.of(0, 3)).stream()
                .map(review -> ReviewResponse.builder()
                        .reviewId(review.getId())
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .createdAt(review.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
