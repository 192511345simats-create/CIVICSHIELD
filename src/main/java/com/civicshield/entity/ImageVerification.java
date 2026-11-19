package com.civicshield.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_verification")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private ComplaintImage image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    // Duplicate detection results
    @Column(nullable = false)
    private boolean exactDuplicate = false;

    @Column(nullable = false)
    private boolean visuallySimilar = false;

    @Column
    private Long duplicateOfImageId;

    @Column
    private Double perceptualHashDistance;

    // EXIF indicators
    @Column(nullable = false)
    private boolean hasGpsData = false;

    @Column(nullable = false)
    private boolean hasTimestamp = false;

    @Column(nullable = false)
    private boolean hasCameraInfo = false;

    // Overall result
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VerificationResult result = VerificationResult.PENDING;

    @Column(length = 500)
    private String notes;

    // Admin review
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String adminNotes;

    @Column(updatable = false)
    private LocalDateTime checkedAt;

    @PrePersist
    protected void onCreate() {
        checkedAt = LocalDateTime.now();
    }

    public enum VerificationResult {
        PENDING,
        CLEAN,
        POSSIBLE_DUPLICATE,
        SUSPICIOUS,
        FLAGGED_BY_ADMIN,
        ACCEPTED_BY_ADMIN
    }
}
