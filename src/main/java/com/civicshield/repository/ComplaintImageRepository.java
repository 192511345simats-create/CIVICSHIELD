package com.civicshield.repository;

import com.civicshield.entity.Complaint;
import com.civicshield.entity.ComplaintImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintImageRepository extends JpaRepository<ComplaintImage, Long> {
    List<ComplaintImage> findByComplaint(Complaint complaint);
    Optional<ComplaintImage> findBySha256Hash(String hash);
    List<ComplaintImage> findByPerceptualHashIsNotNull();
    Optional<ComplaintImage> findByComplaintAndImageRole(Complaint complaint, ComplaintImage.ImageRole role);
}
