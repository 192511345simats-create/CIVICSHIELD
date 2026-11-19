package com.civicshield.repository;

import com.civicshield.entity.SafetyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SafetyAlertRepository extends JpaRepository<SafetyAlert, Long> {
    List<SafetyAlert> findByStatusOrderByCreatedAtDesc(SafetyAlert.AlertStatus status);
    List<SafetyAlert> findAllByOrderByCreatedAtDesc();
    List<SafetyAlert> findByExpiresAtBeforeAndStatus(LocalDateTime now, SafetyAlert.AlertStatus status);
}
