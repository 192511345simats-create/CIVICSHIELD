package com.civicshield.service;

import com.civicshield.entity.Assignment;
import com.civicshield.entity.Complaint;
import com.civicshield.entity.RepairEvidence;
import com.civicshield.entity.User;
import com.civicshield.notification.NotificationService;
import com.civicshield.repository.AssignmentRepository;
import com.civicshield.repository.ComplaintRepository;
import com.civicshield.repository.RepairEvidenceRepository;
import com.civicshield.repository.UserRepository;
import com.civicshield.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepairEvidenceService {

    private final RepairEvidenceRepository repairEvidenceRepository;
    private final AssignmentRepository assignmentRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    @Transactional
    public RepairEvidence saveEvidence(Long assignmentId, MultipartFile before, MultipartFile after, User user) throws Exception {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));
        Complaint complaint = assignment.getComplaint();

        RepairEvidence evidence = repairEvidenceRepository.findByComplaint(complaint).orElse(new RepairEvidence());
        if (before != null && !before.isEmpty()) {
            evidence.setBeforeImagePath(fileStorageService.store(before));
        }
        if (after != null && !after.isEmpty()) {
            evidence.setAfterImagePath(fileStorageService.store(after));
        }
        evidence.setWorkerName(user != null ? user.getFullName() : assignment.getWorkerName());
        evidence.setComplaint(complaint);
        if (user != null) {
            evidence.setSubmittedBy(user);
        } else {
            userRepository.findByEmail(assignment.getWorkerContact()).ifPresent(evidence::setSubmittedBy);
        }
        evidence.setStatus(RepairEvidence.EvidenceStatus.PENDING_REVIEW);
        RepairEvidence saved = repairEvidenceRepository.save(evidence);

        complaint.setStatus(Complaint.Status.IN_PROGRESS);
        complaintRepository.save(complaint);

        return saved;
    }

    @Transactional
    public RepairEvidence submitEvidence(Long assignmentId, String workerNotes, List<String> imagePaths) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        Complaint complaint = assignment.getComplaint();

        RepairEvidence evidence = new RepairEvidence();
        if (imagePaths != null && !imagePaths.isEmpty()) {
            evidence.setBeforeImagePath(imagePaths.get(0));
            if (imagePaths.size() > 1) {
                evidence.setAfterImagePath(imagePaths.get(1));
            }
        }
        evidence.setWorkerName(assignment.getWorkerName());
        evidence.setCompletionNotes(workerNotes);
        evidence.setComplaint(complaint);
        Optional<User> workerOpt = userRepository.findByEmail(assignment.getWorkerContact());
        workerOpt.ifPresent(evidence::setSubmittedBy);
        evidence.setStatus(RepairEvidence.EvidenceStatus.PENDING_REVIEW);
        RepairEvidence saved = repairEvidenceRepository.save(evidence);

        complaint.setStatus(Complaint.Status.IN_PROGRESS);
        complaintRepository.save(complaint);

        notificationService.createNotification(null, "New repair evidence submitted for complaint " + complaint.getId(), complaint.getId());
        return saved;
    }

    @Transactional
    public RepairEvidence reviewEvidence(Long evidenceId, boolean approved, String adminNotes) {
        RepairEvidence evidence = repairEvidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found"));
        evidence.setStatus(approved ? RepairEvidence.EvidenceStatus.APPROVED : RepairEvidence.EvidenceStatus.REJECTED);
        evidence.setReviewNotes(adminNotes);
        if (approved) {
            Complaint complaint = evidence.getComplaint();
            complaint.setStatus(Complaint.Status.RESOLVED);
            complaintRepository.save(complaint);
            User citizen = complaint.getUser();
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
