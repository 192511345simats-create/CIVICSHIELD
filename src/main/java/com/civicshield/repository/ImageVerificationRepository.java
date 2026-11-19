package com.civicshield.repository;

import com.civicshield.entity.Complaint;
import com.civicshield.entity.ImageVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImageVerificationRepository extends JpaRepository<ImageVerification, Long> {
    Optional<ImageVerification> findByComplaint(Complaint complaint);
    List<ImageVerification> findByResult(ImageVerification.VerificationResult result);
    long countByResult(ImageVerification.VerificationResult result);
}
