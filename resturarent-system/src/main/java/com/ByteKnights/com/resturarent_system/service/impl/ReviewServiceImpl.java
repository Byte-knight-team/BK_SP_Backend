package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.ReviewSubmissionRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.ReviewImagePresignResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.ReviewImageResponse;
import com.ByteKnights.com.resturarent_system.dto.response.ReviewResponse;
import com.ByteKnights.com.resturarent_system.entity.Customer;
import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderItem;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.ReviewImage;
import com.ByteKnights.com.resturarent_system.entity.Review;
import com.ByteKnights.com.resturarent_system.exception.CheckoutException;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.CustomerRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderItemRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.ReviewImageRepository;
import com.ByteKnights.com.resturarent_system.repository.ReviewRepository;
import com.ByteKnights.com.resturarent_system.service.ReviewImageStorageService;
import com.ByteKnights.com.resturarent_system.service.ReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles review submission, presigned image URL creation, and fetching recent reviews.
 * Image files are stored in AWS S3; this service only deals with S3 object keys and metadata.
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    // Hard cap enforced in service layer as a second line of defence (DTO also validates this)
    private static final int MAX_IMAGES_PER_REVIEW = 5;

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewImageStorageService reviewImageStorageService;

    public ReviewServiceImpl(ReviewRepository reviewRepository, OrderRepository orderRepository,
            CustomerRepository customerRepository, OrderItemRepository orderItemRepository,
            ReviewImageRepository reviewImageRepository, ReviewImageStorageService reviewImageStorageService) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.orderItemRepository = orderItemRepository;
        this.reviewImageRepository = reviewImageRepository;
        this.reviewImageStorageService = reviewImageStorageService;
    }

    @Override
    public List<ReviewImagePresignResponse> createReviewImageUploadUrls(String userIdentifier,
            List<ReviewSubmissionRequest.ReviewImageUploadRequest> files) {
        return reviewImageStorageService.createPresignedUploadUrls(userIdentifier, files);
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

        // Build the expected S3 key prefix for this user to validate image ownership
        String expectedKeyPrefix = buildExpectedKeyPrefix(userIdentifier);

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
            Review savedOrderReview = reviewRepository.save(orderReview);
            saveReviewImages(savedOrderReview, request.getOrderReview().getImageKeys(), expectedKeyPrefix);
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
                Review savedItemReview = reviewRepository.save(itemReview);
                saveReviewImages(savedItemReview, itemReq.getImageKeys(), expectedKeyPrefix);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getRecentReviews() {
        List<Review> reviews = reviewRepository.findRecentOrderReviews(PageRequest.of(0, 3));
        if (reviews.isEmpty()) {
            return List.of();
        }

        // Batch-load all images for these reviews in a single query to avoid N+1
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Map<Long, List<ReviewImageResponse>> imagesByReviewId = reviewImageRepository.findByReviewIdIn(reviewIds)
                .stream()
                .collect(Collectors.groupingBy(
                        image -> image.getReview().getId(),
                        Collectors.mapping(
                                image -> reviewImageStorageService.toResponse(
                                        image.getImageKey(),
                                        image.getFileName(),
                                        image.getContentType()),
                                Collectors.toList())));

        return reviews.stream()
                .map(review -> ReviewResponse.builder()
                        .reviewId(review.getId())
                        .customerName(review.getCustomer().getUser().getFullName())
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .createdAt(review.getCreatedAt())
                        .images(imagesByReviewId.getOrDefault(review.getId(), List.of()))
                        .build())
                .collect(Collectors.toList());
    }

    private void saveReviewImages(Review review, List<ReviewSubmissionRequest.ReviewImageKeyEntry> imageEntries,
            String expectedKeyPrefix) {
        if (imageEntries == null || imageEntries.isEmpty()) {
            return;
        }

        if (imageEntries.size() > MAX_IMAGES_PER_REVIEW) {
            throw new CheckoutException(HttpStatus.BAD_REQUEST,
                    "A maximum of " + MAX_IMAGES_PER_REVIEW + " images per review is allowed.");
        }

        List<ReviewImage> reviewImages = new ArrayList<>();
        for (ReviewSubmissionRequest.ReviewImageKeyEntry entry : imageEntries) {
            if (entry == null || entry.getObjectKey() == null || entry.getObjectKey().isBlank()) {
                continue;
            }

            String objectKey = entry.getObjectKey().trim();

            // Prevent a user from claiming images uploaded by someone else.
            // The prefix is built the same way as in ReviewImageStorageServiceImpl.buildObjectKey(),
            // so it will only match keys that this user's presign request generated.
            if (!objectKey.startsWith(expectedKeyPrefix)) {
                throw new CheckoutException(HttpStatus.FORBIDDEN,
                        "Image key does not belong to the authenticated user.");
            }

            reviewImages.add(ReviewImage.builder()
                    .review(review)
                    .imageKey(objectKey)
                    .fileName(entry.getFileName() != null ? entry.getFileName().trim() : objectKey)
                    .contentType(entry.getContentType() != null ? entry.getContentType().trim() : "image/jpeg")
                    .build());
        }

        if (!reviewImages.isEmpty()) {
            reviewImageRepository.saveAll(reviewImages);
        }
    }

    /**
     * Builds the expected S3 key prefix for a given user identifier.
     * This mirrors the key structure in
     * ReviewImageStorageServiceImpl.buildObjectKey().
     */
    private String buildExpectedKeyPrefix(String userIdentifier) {
        String sanitized = (userIdentifier == null || userIdentifier.isBlank())
                ? "anonymous"
                : userIdentifier.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-");
        return "reviews/" + sanitized + "/";
    }
}
