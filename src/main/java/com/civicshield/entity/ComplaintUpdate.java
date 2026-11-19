package com.civicshield.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_updates", indexes = {
    @Index(name = "idx_updates_complaint", columnList = "complaint_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Complaint.Status previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Complaint.Status newStatus;

    @Column(length = 500)
    private String notes;

    @Column(updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }
}
