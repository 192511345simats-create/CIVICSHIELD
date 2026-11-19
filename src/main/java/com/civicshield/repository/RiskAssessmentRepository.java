package com.civicshield.repository;

import com.civicshield.entity.Complaint;
import com.civicshield.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
    Optional<RiskAssessment> findByComplaint(Complaint complaint);

    @Query("SELECT r FROM RiskAssessment r WHERE r.totalScore >= :minScore ORDER BY r.totalScore DESC")
    List<RiskAssessment> findHighRisk(int minScore);
}
