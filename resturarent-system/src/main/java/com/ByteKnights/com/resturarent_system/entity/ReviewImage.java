package com.ByteKnights.com.resturarent_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores metadata for images attached to a review.
 * The actual image file lives in AWS S3; only the S3 object key is stored here.
 */
@Entity
@Table(name = "review_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many images can belong to one review
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // S3 object key, e.g. "reviews/94770000001/uuid.jpg"
    @Column(nullable = false, length = 512)
    private String imageKey;

    // Original file name chosen by the user (e.g. "food-photo.jpg")
    @Column(nullable = false)
    private String fileName;

    // MIME type of the image (image/jpeg, image/png, image/webp)
    @Column(nullable = false)
    private String contentType;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}