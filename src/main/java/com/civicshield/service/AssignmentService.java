package com.civicshield.service;

import com.civicshield.dto.AssignmentDTO;
import com.civicshield.entity.*;
import com.civicshield.exception.ResourceNotFoundException;
import com.civicshield.repository.*;
import com.civicshield.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final DepartmentRepository departmentRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintUpdateRepository updateRepository;
    private final NotificationService notificationService;

    @Transactional
    public Assignment assign(Long complaintId, AssignmentDTO dto, User assignedBy) {
        Complaint complaint = complaintRepository.findById(complaintId)
            .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));
        Department department = departmentRepository.findById(dto.getDepartmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + dto.getDepartmentId()));

        // Update or create assignment
        Assignment assignment = assignmentRepository.findByComplaint(complaint)
            .orElse(new Assignment());

        assignment.setComplaint(complaint);
        assignment.setDepartment(department);
        assignment.setWorkerName(dto.getWorkerName());
        assignment.setWorkerContact(dto.getWorkerContact());
        assignment.setAssignedBy(assignedBy);
        assignment.setDeadline(dto.getDeadline());
        assignment.setInstructions(dto.getInstructions());
        assignment.setStatus(Assignment.AssignmentStatus.PENDING);

        assignment = assignmentRepository.save(assignment);

        // Update complaint status
        complaint.setStatus(Complaint.Status.ASSIGNED);
        complaintRepository.save(complaint);

        // Log status change
        ComplaintUpdate update = new ComplaintUpdate();
        update.setComplaint(complaint);
        update.setUpdatedBy(assignedBy);
        update.setPreviousStatus(Complaint.Status.VERIFIED);
        update.setNewStatus(Complaint.Status.ASSIGNED);
        update.setNotes("Assigned to " + department.getName()
            + (dto.getWorkerName() != null ? " — Worker: " + dto.getWorkerName() : ""));
        updateRepository.save(update);

        notificationService.notifyStatusChange(complaint, Complaint.Status.ASSIGNED);
        log.info("Complaint {} assigned to dept {} by {}", complaint.getComplaintId(),
            department.getName(), assignedBy.getEmail());
        return assignment;
    }

    @Transactional
    public Assignment updateStatus(Long assignmentId, Assignment.AssignmentStatus status, User updatedBy) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        assignment.setStatus(status);
        return assignmentRepository.save(assignment);
    }

    public List<Assignment> getAll() { return assignmentRepository.findAll(); }
    public List<Assignment> getOverdue() {
        return assignmentRepository.findOverdueAssignments(java.time.LocalDateTime.now());
    }
    public List<Object[]> countByDepartment() { return assignmentRepository.countByDepartment(); }
    public List<Object[]> countCompletedByDepartment() { return assignmentRepository.countCompletedByDepartment(); }
    public Optional<Assignment> getByComplaint(Complaint complaint) { return assignmentRepository.findByComplaint(complaint); }
}
