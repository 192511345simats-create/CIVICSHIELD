package com.civicshield.repository;

import com.civicshield.entity.Complaint;
import com.civicshield.entity.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    boolean existsByComplaint(Complaint complaint);
    List<Escalation> findByStatus(Escalation.EscalationStatus status);
    List<Escalation> findAllByOrderByEscalatedAtDesc();
}
