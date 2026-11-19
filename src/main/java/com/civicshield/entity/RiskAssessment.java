package com.civicshield.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    // Score components (0–100 scale each, weighted)
    private int severityScore;         // based on category
    private int publicExposureScore;   // pedestrian/traffic impact
    private int reportedDurationScore; // how long it's been outstanding
    private int confirmationScore;     // community supports
    private int locationRiskScore;     // hotspot multiplier

    private int totalScore; // 0–100

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Complaint.Priority calculatedPriority;

    @Column(length = 1000)
    private String scoreBreakdown;

    // Admin can override
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Complaint.Priority adminOverridePriority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "overridden_by")
    private User overriddenBy;

    private LocalDateTime overriddenAt;

    @Column(length = 500)
    private String overrideReason;

    @Column(updatable = false)
    private LocalDateTime assessedAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        assessedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
