package com.civicshield.scheduler;

import com.civicshield.entity.*;
import com.civicshield.repository.AssignmentRepository;
import com.civicshield.repository.EscalationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EscalationScheduler {

    private final AssignmentRepository assignmentRepository;
    private final EscalationRepository escalationRepository;

    @Scheduled(fixedDelayString = "${app.escalation.check-interval-ms:3600000}")
    @Transactional
    public void checkEscalations() {
        List<Assignment> overdue = assignmentRepository.findOverdueAssignments(LocalDateTime.now());
        int created = 0;
        for (Assignment a : overdue) {
            Complaint c = a.getComplaint();
            if (c.getStatus() == Complaint.Status.RESOLVED || c.getStatus() == Complaint.Status.REJECTED) continue;
            if (escalationRepository.existsByComplaint(c)) continue;

            Escalation esc = new Escalation();
            esc.setComplaint(c);
            esc.setAssignment(a);
            esc.setOriginalDeadline(a.getDeadline());
            esc.setDelayDays((int) Math.max(0, ChronoUnit.DAYS.between(a.getDeadline(), LocalDateTime.now())));
            esc.setStatus(Escalation.EscalationStatus.FLAGGED);
            escalationRepository.save(esc);

            a.setStatus(Assignment.AssignmentStatus.OVERDUE);
            assignmentRepository.save(a);
            created++;
        }
        if (created > 0) log.info("Escalation check: {} new escalation(s) flagged", created);
    }
}
