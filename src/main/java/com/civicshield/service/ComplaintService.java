package com.civicshield.service;

import com.civicshield.algorithm.DuplicateDetector;
import com.civicshield.algorithm.HotspotDetector;
import com.civicshield.algorithm.RiskScoringEngine;
import com.civicshield.dto.ComplaintSubmitDTO;
import com.civicshield.entity.*;
import com.civicshield.exception.ResourceNotFoundException;
import com.civicshield.imageverification.ImageVerificationService;
import com.civicshield.notification.NotificationService;
import com.civicshield.repository.*;
import com.civicshield.util.ComplaintIdGenerator;
import com.civicshield.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintImageRepository imageRepository;
    private final ComplaintUpdateRepository updateRepository;
    private final ComplaintConfirmationRepository confirmationRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final ImageVerificationRepository verificationRepository;
    private final AssignmentRepository assignmentRepository;

    private final FileStorageService fileStorageService;
    private final ComplaintIdGenerator idGenerator;
    private final RiskScoringEngine riskScoringEngine;
    private final DuplicateDetector duplicateDetector;
    private final HotspotDetector hotspotDetector;
    private final ImageVerificationService imageVerificationService;
    private final NotificationService notificationService;
    private final RepairEvidenceService repairEvidenceService;

    @Transactional
    public Complaint submit(ComplaintSubmitDTO dto, User user, MultipartFile image) throws IOException {
        Complaint complaint = new Complaint();
        complaint.setComplaintId(generateUniqueId());
        complaint.setUser(user);
        complaint.setCategory(dto.getCategory());
        complaint.setDescription(dto.getDescription());
        complaint.setLocationAddress(dto.getLocationAddress());
        complaint.setLatitude(dto.getLatitude());
        complaint.setLongitude(dto.getLongitude());
        complaint.setWard(dto.getWard());
        complaint.setDistrict(dto.getDistrict());
        complaint.setStatus(Complaint.Status.SUBMITTED);
        complaint.setDataType(Complaint.DataType.CITIZEN_REPORT);

        complaint = complaintRepository.save(complaint);

        // Handle image upload
        if (image != null && !image.isEmpty()) {
            processImage(complaint, image);
        }

        // Duplicate detection
        Optional<Complaint> duplicate = duplicateDetector.findDuplicate(complaint);
        if (duplicate.isPresent()) {
            complaint.setPossibleDuplicate(true);
            complaint.setDuplicateOfComplaintId(duplicate.get().getId());
        }

        // Risk assessment
        boolean isHotspot = complaint.getLatitude() != null &&
            hotspotDetector.isHotspot(complaint.getLatitude(), complaint.getLongitude());
        RiskAssessment assessment = riskScoringEngine.assess(complaint, 0, isHotspot);
        complaint = complaintRepository.save(complaint);
        riskAssessmentRepository.save(assessment);

        // Record initial status
        recordStatusChange(complaint, null, Complaint.Status.SUBMITTED, user, "Report submitted.");

        // Notify user
        notificationService.notifyComplaintReceived(complaint);

        log.info("Complaint {} submitted by {}", complaint.getComplaintId(), user.getEmail());
        return complaint;
    }

    private void processImage(Complaint complaint, MultipartFile image) throws IOException {
        String path = fileStorageService.store(image);
        complaint.setImagePath(path);

        ComplaintImage ci = new ComplaintImage();
        ci.setComplaint(complaint);
        ci.setFilePath(path);
        ci.setOriginalFileName(image.getOriginalFilename());
        ci.setContentType(image.getContentType());
        ci.setFileSizeBytes(image.getSize());
        ci.setImageRole(ComplaintImage.ImageRole.COMPLAINT_PHOTO);
        ci.setSha256Hash(DigestUtils.sha256Hex(image.getInputStream()));

        try {
            ci.setPerceptualHash(imageVerificationService.computePerceptualHash(image));
        } catch (Exception e) {
            log.debug("pHash unavailable: {}", e.getMessage());
        }

        ci = imageRepository.save(ci);

        // EXIF extraction
        File file = fileStorageService.resolveFile(path);
        imageVerificationService.extractAndStoreExif(ci, file);

        // Run verification
        imageVerificationService.performVerification(ci);
    }

    private String generateUniqueId() {
        String id;
        do {
            id = idGenerator.generate();
        } while (complaintRepository.findByComplaintId(id).isPresent());
        return id;
    }

    @Transactional
    public Complaint updateStatus(Long complaintId, Complaint.Status newStatus,
                                  User updatedBy, String notes) {
        Complaint complaint = getById(complaintId);
        Complaint.Status prev = complaint.getStatus();
        complaint.setStatus(newStatus);
        complaint = complaintRepository.save(complaint);
        recordStatusChange(complaint, prev, newStatus, updatedBy, notes);
        notificationService.notifyStatusChange(complaint, newStatus);
        return complaint;
    }

    private void recordStatusChange(Complaint complaint, Complaint.Status prev,
                                    Complaint.Status next, User by, String notes) {
        ComplaintUpdate update = new ComplaintUpdate();
        update.setComplaint(complaint);
        update.setUpdatedBy(by);
        update.setPreviousStatus(prev != null ? prev : next);
        update.setNewStatus(next);
        update.setNotes(notes);
        updateRepository.save(update);
    }

    @Transactional
    public boolean confirmComplaint(Long complaintId, User user, String note) {
        Complaint complaint = getById(complaintId);
        if (complaint.getUser().getId().equals(user.getId())) return false;
        if (confirmationRepository.existsByComplaintAndUser(complaint, user)) return false;

        ComplaintConfirmation conf = new ComplaintConfirmation();
        conf.setComplaint(complaint); conf.setUser(user); conf.setNote(note);
        confirmationRepository.save(conf);

        int count = (int) confirmationRepository.countByComplaint(complaint);
        complaint.setConfirmationCount(count);

        // Re-run risk with updated confirmations
        RiskAssessment assessment = riskScoringEngine.assess(complaint, count,
            complaint.getLatitude() != null && hotspotDetector.isHotspot(complaint.getLatitude(), complaint.getLongitude()));
        complaintRepository.save(complaint);
        Optional<RiskAssessment> existing = riskAssessmentRepository.findByComplaint(complaint);
        existing.ifPresent(ra -> { ra.setTotalScore(assessment.getTotalScore());
            ra.setCalculatedPriority(assessment.getCalculatedPriority());
            ra.setConfirmationScore(assessment.getConfirmationScore());
            riskAssessmentRepository.save(ra); });
        if (existing.isEmpty()) riskAssessmentRepository.save(assessment);
        return true;
    }

    public Complaint getById(Long id) {
        Complaint c = complaintRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
        // Populate repair image URLs if evidence exists
        repairEvidenceService.getByComplaint(c).ifPresent(e -> {
            if (e.getBeforeImagePath() != null) {
                c.setRepairBeforeUrl("/uploads/" + e.getBeforeImagePath());
            }
            if (e.getAfterImagePath() != null) {
                c.setRepairAfterUrl("/uploads/" + e.getAfterImagePath());
            }
        });
        return c;
    }
        return complaintRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }

    public Complaint getByComplaintId(String complaintId) {
        return complaintRepository.findByComplaintId(complaintId)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + complaintId));
    }

    public List<Complaint> getByUser(User user) {
        return complaintRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Complaint> getAll() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Complaint> getNearby(double lat, double lng, double radiusDeg) {
        return complaintRepository.findNearbyComplaints(lat - radiusDeg, lat + radiusDeg,
            lng - radiusDeg, lng + radiusDeg);
    }

    public List<Complaint> getHighPriority() {
        return complaintRepository.findHighPriorityActive();
    }

    public List<ComplaintUpdate> getTimeline(Complaint complaint) {
        return updateRepository.findByComplaintOrderByUpdatedAtAsc(complaint);
    }

    public Optional<Assignment> getAssignment(Complaint complaint) {
        return assignmentRepository.findByComplaint(complaint);
    }

    // Dashboard stats
    public long countByStatus(Complaint.Status status) { return complaintRepository.countByStatus(status); }
    public long countByPriority(Complaint.Priority p)   { return complaintRepository.countByPriority(p); }
    public long countCitizenReports() { return complaintRepository.countByDataType(Complaint.DataType.CITIZEN_REPORT); }

    public List<Object[]> countByCategory()   { return complaintRepository.countByCategory(); }
    public List<Object[]> countByStatus()     { return complaintRepository.countByStatusGrouped(); }
    public List<Object[]> countByMonth()      { return complaintRepository.countByMonth(LocalDateTime.now().minusMonths(12)); }
}
