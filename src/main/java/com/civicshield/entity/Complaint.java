package com.civicshield.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

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

    // Image
    @Column(length = 255)
    private String imagePath;

    // Verification
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ImageVerificationStatus imageVerificationStatus = ImageVerificationStatus.PENDING;

    // Duplicate detection
    @Column
    private boolean possibleDuplicate = false;

    @Column
    private Long duplicateOfComplaintId;

    // Confirmations (community support count)
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

    // ---- Enums ----

    public enum Category {
        ROAD_DAMAGE,
        STREETLIGHT_FAILURE,
        DRAINAGE_BLOCKAGE,
        WATER_LEAKAGE,
        PUBLIC_STRUCTURE_DAMAGE,
        OPEN_MANHOLE,
        FALLEN_TREE,
        OTHER;

        public String getDisplayName() {
            return switch (this) {
                case ROAD_DAMAGE -> "Road Damage";
                case STREETLIGHT_FAILURE -> "Streetlight Failure";
                case DRAINAGE_BLOCKAGE -> "Drainage Blockage";
                case WATER_LEAKAGE -> "Water Leakage";
                case PUBLIC_STRUCTURE_DAMAGE -> "Public Structure Damage";
                case OPEN_MANHOLE -> "Open Manhole";
                case FALLEN_TREE -> "Fallen Tree";
                case OTHER -> "Other";
            };
        }
    }

    public enum Status {
        SUBMITTED,
        UNDER_REVIEW,
        VERIFIED,
        ASSIGNED,
        IN_PROGRESS,
        RESOLVED,
        REJECTED;

        public String getDisplayName() {
            return switch (this) {
                case SUBMITTED -> "Submitted";
                case UNDER_REVIEW -> "Under Review";
                case VERIFIED -> "Verified";
                case ASSIGNED -> "Assigned";
                case IN_PROGRESS -> "In Progress";
                case RESOLVED -> "Resolved";
                case REJECTED -> "Rejected";
            };
        }
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL;

        public String getDisplayName() {
            return switch (this) {
                case LOW -> "Low";
                case MEDIUM -> "Medium";
                case HIGH -> "High";
                case CRITICAL -> "Critical";
            };
        }

        public String getBadgeClass() {
            return switch (this) {
                case LOW -> "badge-low";
                case MEDIUM -> "badge-medium";
                case HIGH -> "badge-high";
                case CRITICAL -> "badge-critical";
            };
        }
    }

    public enum ImageVerificationStatus {
        PENDING, CLEAN, POSSIBLE_DUPLICATE, SUSPICIOUS, REVIEWED
    }

    public enum DataType {
        CITIZEN_REPORT, SAMPLE_DATA, HISTORICAL_DATASET
    }
}
