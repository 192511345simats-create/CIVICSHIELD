package com.civicshield.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "repair_evidence")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(length = 255)
    private String beforeImagePath;

    @Column(length = 255)
    private String afterImagePath;

    @Column(length = 100)
    private String workerName;

    @Column(length = 500)
    private String completionNotes;

    private LocalDateTime repairDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvidenceStatus status = EvidenceStatus.PENDING_REVIEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String reviewNotes;

    @Column(updatable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }

    public enum EvidenceStatus {
        PENDING_REVIEW, APPROVED, REJECTED
    }
}
