package com.civicshield.repository;

import com.civicshield.entity.Complaint;
import com.civicshield.entity.ComplaintConfirmation;
import com.civicshield.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintConfirmationRepository extends JpaRepository<ComplaintConfirmation, Long> {
    boolean existsByComplaintAndUser(Complaint complaint, User user);
    long countByComplaint(Complaint complaint);
}
