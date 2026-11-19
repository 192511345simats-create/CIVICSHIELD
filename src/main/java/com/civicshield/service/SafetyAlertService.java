package com.civicshield.service;

import com.civicshield.entity.*;
import com.civicshield.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SafetyAlertService {

    private final SafetyAlertRepository alertRepository;

    public List<SafetyAlert> getActive() {
        return alertRepository.findByStatusOrderByCreatedAtDesc(SafetyAlert.AlertStatus.ACTIVE);
    }

    public List<SafetyAlert> getAll() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<SafetyAlert> getById(Long id) {
        return alertRepository.findById(id);
    }

    @Transactional
    public SafetyAlert create(SafetyAlert alert) {
        return alertRepository.save(alert);
    }

    @Transactional
    public SafetyAlert updateStatus(Long id, SafetyAlert.AlertStatus status) {
        SafetyAlert alert = alertRepository.findById(id)
            .orElseThrow(() -> new com.civicshield.exception.ResourceNotFoundException("Alert not found: " + id));
        alert.setStatus(status);
        return alertRepository.save(alert);
    }
}
