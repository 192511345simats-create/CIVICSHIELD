package com.civicshield.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(nullable = false, length = 255)
    private String filePath;

    @Column(length = 100)
    private String originalFileName;

    @Column(length = 50)
    private String contentType;

    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ImageRole imageRole = ImageRole.COMPLAINT_PHOTO;

    // Verification hashes
    @Column(length = 64)
    private String sha256Hash;

    @Column(length = 64)
    private String perceptualHash;

    // EXIF metadata (stored as JSON text)
    @Column(columnDefinition = "TEXT")
    private String exifMetadataJson;

    // GPS from EXIF if available
    private Double exifLatitude;
    private Double exifLongitude;

    private LocalDateTime exifCaptureTime;

    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    public enum ImageRole {
        COMPLAINT_PHOTO, REPAIR_BEFORE, REPAIR_AFTER
    }
}
