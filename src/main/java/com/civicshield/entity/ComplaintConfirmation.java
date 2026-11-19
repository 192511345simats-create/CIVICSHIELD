package com.civicshield.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_confirmations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"complaint_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 300)
    private String note;

    @Column(updatable = false)
    private LocalDateTime confirmedAt;

    @PrePersist
    protected void onCreate() {
        confirmedAt = LocalDateTime.now();
    }
}
