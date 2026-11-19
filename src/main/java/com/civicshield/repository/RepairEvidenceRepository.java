package com.civicshield.repository;

import com.civicshield.entity.Complaint;
import com.civicshield.entity.RepairEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RepairEvidenceRepository extends JpaRepository<RepairEvidence, Long> {
    Optional<RepairEvidence> findByComplaint(Complaint complaint);
}
