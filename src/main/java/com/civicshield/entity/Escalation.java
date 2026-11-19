package com.civicshield.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "escalations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    private LocalDateTime originalDeadline;

    private int delayDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EscalationStatus status = EscalationStatus.FLAGGED;

    @Column(length = 500)
    private String delayReason;

    @Column(length = 500)
    private String adminAction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escalated_to")
    private User escalatedTo;

    @Column(updatable = false)
    private LocalDateTime escalatedAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        escalatedAt = LocalDateTime.now();
    }

    public enum EscalationStatus {
        FLAGGED, REVIEWED, ESCALATED, RESOLVED
    }
}
