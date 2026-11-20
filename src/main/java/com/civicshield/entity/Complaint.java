package com.civicshield.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaints_user", columnList = "user_id"),
        @Index(name = "idx_complaints_status", columnList = "status"),
        @Index(name = "idx_complaints_category", columnList = "category"),
        @Index(name = "idx_complaints_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String complaintId; // e.g. CS-2024-001234

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category;

    @Column(nullable = false, length = 500)
    private String description;

    // Location
    @Column(length = 255)
    private String locationAddress;

    @Column(precision = 10)
    private Double latitude;

    @Column(precision = 10)
    private Double longitude;

    @Column(length = 100)
    private String ward;

    @Column(length = 100)
    private String district;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.SUBMITTED;

    // Priority / Risk
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Priority priority;

    @Column
    private Integer riskScore;

    @Column(length = 500)
    private String riskFactors;

    // Image path for the original complaint photo
    @Column(length = 255)
    private String imagePath;

    // Verification status of the image
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ImageVerificationStatus imageVerificationStatus = ImageVerificationStatus.PENDING;

    // Duplicate detection
    @Column
    private boolean possibleDuplicate = false;

    @Column
    private Long duplicateOfComplaintId;

    // Confirmation count (citizen support)
    @Column(nullable = false)
    private int confirmationCount = 0;

    // Data type flag
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataType dataType = DataType.CITIZEN_REPORT;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the URL that can be used by the frontend to access the complaint image.
     * If the stored path is already an absolute URL it is returned unchanged, otherwise
     * it is prefixed with the application upload endpoint.
     */
    public String getImageUrl() {
        return (imagePath != null && imagePath.startsWith("http")) ? imagePath : "/uploads/" + imagePath;
    }

    // Helper methods for repair images (before/after) – these rely on the ComplaintImage entity.
    // The actual retrieval logic will be handled in the service layer; the methods return null
    // if no image is associated.
    @Transient
    private String repairBeforeUrl;

    @Transient
    private String repairAfterUrl;

    public String getRepairBeforeUrl() {
        return repairBeforeUrl;
    }

    public void setRepairBeforeUrl(String url) {
        this.repairBeforeUrl = url;
    }

    public String getRepairAfterUrl() {
        return repairAfterUrl;
    }

    public void setRepairAfterUrl(String url) {
        this.repairAfterUrl = url;
    }

    // ---------- Enums ----------
    public enum Category {
        ROAD_DAMAGE,
        STREETLIGHT_FAILURE,
        DRAINAGE_BLOCKAGE,
        WATER_LEAKAGE,
        PUBLIC_STRUCTURE_DAMAGE,
        OPEN_MANHOLE,
        FALLEN_TREE,
        OTHER;
    }

    public enum Status {
        SUBMITTED,
        UNDER_REVIEW,
        VERIFIED,
        ASSIGNED,
        IN_PROGRESS,
        RESOLVED,
        REJECTED
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum ImageVerificationStatus {
        PENDING, CLEAN, POSSIBLE_DUPLICATE, SUSPICIOUS, REVIEWED
    }

    public enum DataType {
        CITIZEN_REPORT, SAMPLE_DATA, HISTORICAL_DATASET
    }
}
