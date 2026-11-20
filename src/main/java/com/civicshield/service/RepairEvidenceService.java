package com.civicshield.service;

import com.civicshield.entity.Assignment;
import com.civicshield.entity.Complaint;
import com.civicshield.entity.RepairEvidence;
import com.civicshield.entity.User;
import com.civicshield.repository.AssignmentRepository;
import com.civicshield.repository.ComplaintRepository;
import com.civicshield.repository.RepairEvidenceRepository;
import com.civicshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@RequiredArgsConstructor
@Slf4j
public class RepairEvidenceService {

    private final RepairEvidenceRepository repairEvidenceRepository;
    private final AssignmentRepository assignmentRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public RepairEvidence submitEvidence(Long assignmentId, String workerNotes, List<String> imagePaths) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        // verify that the logged-in worker matches assignment workerContact - handled in controller
        Complaint complaint = assignment.getComplaint();

        // create evidence linked to complaint and assignment
        RepairEvidence evidence = new RepairEvidence();
        // Assuming before/after images stored as first two entries
        if (imagePaths != null && !imagePaths.isEmpty()) {
            evidence.setBeforeImagePath(imagePaths.get(0));
            if (imagePaths.size() > 1) {
                evidence.setAfterImagePath(imagePaths.get(1));
            }
        }
        evidence.setWorkerName(assignment.getWorkerName());
        evidence.setCompletionNotes(workerNotes);
        evidence.setComplaint(complaint);
        // link to worker user (by email)
        Optional<User> workerOpt = userRepository.findByEmail(assignment.getWorkerContact());
        workerOpt.ifPresent(evidence::setSubmittedBy);
        evidence.setStatus(RepairEvidence.EvidenceStatus.PENDING_REVIEW);
        // save
        RepairEvidence saved = repairEvidenceRepository.save(evidence);

        // update complaint status to REPAIR_COMPLETED
        complaint.setStatus(Complaint.Status.REPAIR_COMPLETED);
        complaintRepository.save(complaint);

        // notify admins
        notificationService.createNotification(null, "New repair evidence submitted for complaint " + complaint.getId(), complaint.getId());
        return saved;
    }

    @Transactional
    public RepairEvidence reviewEvidence(Long evidenceId, boolean approved, String adminNotes) {
        RepairEvidence evidence = repairEvidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found"));
        evidence.setStatus(approved ? RepairEvidence.EvidenceStatus.APPROVED : RepairEvidence.EvidenceStatus.REJECTED);
        evidence.setReviewNotes(adminNotes);
        // set reviewer (admin) - left as null for now, could be set later
        // update complaint status if approved
        if (approved) {
            Complaint complaint = evidence.getComplaint();
            complaint.setStatus(Complaint.Status.AWAITING_CONFIRMATION);
            complaintRepository.save(complaint);
            // notify citizen
            User citizen = complaint.getCitizen();
            notificationService.createNotification(citizen, "Your repair evidence has been approved for complaint " + complaint.getId(), complaint.getId());
        }
        return repairEvidenceRepository.save(evidence);
    }

    @Transactional(readOnly = true)
    public Optional<RepairEvidence> getByComplaint(Complaint complaint) {
        return repairEvidenceRepository.findByComplaint(complaint);
    }

    @Transactional(readOnly = true)
    public List<RepairEvidence> getAll() {
        return repairEvidenceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RepairEvidence> getPending() {
        return repairEvidenceRepository.findAll().stream()
                .filter(e -> e.getStatus() == RepairEvidence.EvidenceStatus.PENDING_REVIEW)
                .toList();
    }
}
