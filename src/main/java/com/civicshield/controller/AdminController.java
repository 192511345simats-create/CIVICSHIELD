package com.civicshield.controller;

import com.civicshield.algorithm.HotspotDetector;
import com.civicshield.dto.AssignmentDTO;
import com.civicshield.dto.DashboardStatsDTO;
import com.civicshield.entity.*;
import com.civicshield.repository.*;
import com.civicshield.security.CustomUserDetails;
import com.civicshield.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ComplaintService complaintService;
    private final UserService userService;
    private final DepartmentService departmentService;
    private final AssignmentService assignmentService;
    private final SafetyAlertService alertService;
    private final HotspotDetector hotspotDetector;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final EscalationRepository escalationRepository;
    private final ImageVerificationRepository imageVerificationRepository;
    private final ObjectMapper objectMapper;

    private User getAdmin(CustomUserDetails p) { return userService.getById(p.getId()); }

    // ===== OVERVIEW =====
    @GetMapping({"/", "/overview"})
    public String overview(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        DashboardStatsDTO stats = new DashboardStatsDTO(
            complaintService.countCitizenReports(),
            complaintService.countByStatus(Complaint.Status.SUBMITTED),
            complaintService.countByStatus(Complaint.Status.UNDER_REVIEW),
            complaintService.countByPriority(Complaint.Priority.HIGH),
            complaintService.countByPriority(Complaint.Priority.CRITICAL),
            complaintService.countByStatus(Complaint.Status.RESOLVED),
            complaintService.countByStatus(Complaint.Status.REJECTED)
        );
        model.addAttribute("stats", stats);
        model.addAttribute("recentComplaints", complaintService.getAll().stream().limit(10).toList());
        model.addAttribute("criticalComplaints", complaintService.getHighPriority().stream().limit(5).toList());
        model.addAttribute("escalations", escalationRepository.findByStatus(Escalation.EscalationStatus.FLAGGED).stream().limit(5).toList());
        model.addAttribute("admin", getAdmin(principal));
        return "admin/overview";
    }

    // ===== ALL COMPLAINTS =====
    @GetMapping("/complaints")
    public String allComplaints(@AuthenticationPrincipal CustomUserDetails p,
                                @RequestParam(required=false) String status,
                                @RequestParam(required=false) String priority,
                                Model model) {
        List<Complaint> complaints;
        if (status != null && !status.isEmpty()) {
            complaints = complaintService.getAll().stream()
                .filter(c -> c.getStatus().name().equals(status)).toList();
        } else {
            complaints = complaintService.getAll();
        }
        model.addAttribute("complaints", complaints);
        model.addAttribute("statuses", Complaint.Status.values());
        model.addAttribute("priorities", Complaint.Priority.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("admin", getAdmin(p));
        return "admin/complaints";
    }

    @GetMapping("/complaints/{id}")
    public String complaintDetail(@PathVariable Long id,
                                  @AuthenticationPrincipal CustomUserDetails p, Model model) {
        Complaint complaint = complaintService.getById(id);
        model.addAttribute("complaint", complaint);
        model.addAttribute("timeline", complaintService.getTimeline(complaint));
        model.addAttribute("departments", departmentService.getActive());
        model.addAttribute("assignment", complaintService.getAssignment(complaint).orElse(null));
        model.addAttribute("imageVerification", imageVerificationRepository.findByComplaint(complaint).orElse(null));
        model.addAttribute("riskAssessment", riskAssessmentRepository.findByComplaint(complaint).orElse(null));
        model.addAttribute("admin", getAdmin(p));
        return "admin/complaint-detail";
    }

    @PostMapping("/complaints/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required=false) String notes,
                               @AuthenticationPrincipal CustomUserDetails p,
                               RedirectAttributes ra) {
        complaintService.updateStatus(id, Complaint.Status.valueOf(status), getAdmin(p), notes);
        ra.addFlashAttribute("success", "Complaint status updated.");
        return "redirect:/admin/complaints/" + id;
    }

    // ===== IMAGE VERIFICATION =====
    @GetMapping("/image-verification")
    public String imageVerification(@AuthenticationPrincipal CustomUserDetails p, Model model) {
        model.addAttribute("verifications", imageVerificationRepository.findAll());
        model.addAttribute("admin", getAdmin(p));
        return "admin/image-verification";
    }

    @PostMapping("/image-verification/{id}/review")
    public String reviewImage(@PathVariable Long id,
                              @RequestParam String result,
                              @RequestParam(required=false) String notes,
                              @AuthenticationPrincipal CustomUserDetails p,
                              RedirectAttributes ra) {
        imageVerificationRepository.findById(id).ifPresent(v -> {
            v.setResult(ImageVerification.VerificationResult.valueOf(result));
            v.setReviewedBy(getAdmin(p));
            v.setReviewedAt(LocalDateTime.now());
            v.setAdminNotes(notes);
            imageVerificationRepository.save(v);
        });
        ra.addFlashAttribute("success", "Image review saved.");
        return "redirect:/admin/image-verification";
    }

    // ===== RISK ASSESSMENT =====
    @GetMapping("/risk-assessment")
    public String riskAssessment(@AuthenticationPrincipal CustomUserDetails p, Model model) {
        model.addAttribute("assessments", riskAssessmentRepository.findAll());
        model.addAttribute("admin", getAdmin(p));
        return "admin/risk-assessment";
    }

    @PostMapping("/risk-assessment/{id}/override")
    public String overrideRisk(@PathVariable Long id,
                               @RequestParam String priority,
                               @RequestParam String reason,
                               @AuthenticationPrincipal CustomUserDetails p,
                               RedirectAttributes ra) {
        riskAssessmentRepository.findById(id).ifPresent(ra2 -> {
            ra2.setAdminOverridePriority(Complaint.Priority.valueOf(priority));
            ra2.setOverriddenBy(getAdmin(p));
            ra2.setOverriddenAt(LocalDateTime.now());
            ra2.setOverrideReason(reason);
            ra2.getComplaint().setPriority(Complaint.Priority.valueOf(priority));
            riskAssessmentRepository.save(ra2);
        });
        ra.addFlashAttribute("success", "Priority overridden.");
        return "redirect:/admin/risk-assessment";
    }

    // ===== DEPARTMENTS =====
    @GetMapping("/departments")
    public String departments(@AuthenticationPrincipal CustomUserDetails p, Model model) {
        model.addAttribute("departments", departmentService.getAll());
        model.addAttribute("admin", getAdmin(p));
        return "admin/departments";
    }

    @PostMapping("/departments/create")
    public String createDept(@RequestParam String name,
                             @RequestParam(required=false) String description,
                             @RequestParam(required=false) String headName,
                             @RequestParam(required=false) String contactEmail,
                             @RequestParam(required=false) String contactPhone,
                             RedirectAttributes ra) {
        try {
            Department d = new Department();
            d.setName(name); d.setDescription(description); d.setHeadName(headName);
            d.setContactEmail(contactEmail); d.setContactPhone(contactPhone); d.setActive(true);
            departmentService.create(d);
            ra.addFlashAttribute("success", "Department created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/{id}/toggle")
    public String toggleDept(@PathVariable Long id,
                             @RequestParam boolean active, RedirectAttributes ra) {
        departmentService.setActive(id, active);
        ra.addFlashAttribute("success", "Department status updated.");
        return "redirect:/admin/departments";
    }

    // ===== WORKER ASSIGNMENTS =====
    @GetMapping("/assignments")
    public String assignments(@AuthenticationPrincipal CustomUserDetails p, Model model) {
        model.addAttribute("assignments", assignmentService.getAll());
        model.addAttribute("departments", departmentService.getActive());
        model.addAttribute("admin", getAdmin(p));
        return "admin/assignments";
    }

    @PostMapping("/complaints/{id}/assign")
    public String assign(@PathVariable Long id,
                         @ModelAttribute AssignmentDTO dto,
                         @AuthenticationPrincipal CustomUserDetails p,
                         RedirectAttributes ra) {
        try {
            assignmentService.assign(id, dto, getAdmin(p));
            ra.addFlashAttribute("success", "Complaint assigned successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Assignment failed: " + e.getMessage());
        }
        return "redirect:/admin/complaints/" + id;
    }

    // ===== ESCALATIONS =====
    @GetMapping("/escalations")
    public String escalations(@AuthenticationPrincipal CustomUserDetails p, Model model) {
        model.addAttribute("escalations", escalationRepository.findAllByOrderByEscalatedAtDesc());
        model.addAttribute("admin", getAdmin(p));
        return "admin/escalations";
    }

    @PostMapping("/escalations/{id}/action")
    public String escalationAction(@PathVariable Long id,
                                   @RequestParam String action,
                                   @RequestParam(required=false) String notes,
                                   RedirectAttributes ra) {
        escalationRepository.findById(id).ifPresent(esc -> {
            esc.setStatus(Escalation.EscalationStatus.REVIEWED);
            esc.setAdminAction(notes);
            escalationRepository.save(esc);
        });
        ra.addFlashAttribute("success", "Escalation action recorded.");
        return "redirect:/admin/escalations";
    }

    // ===== ANALYTICS =====
    @GetMapping("/analytics")
    public String analytics(@AuthenticationPrincipal CustomUserDetails p, Model model) {
        try {
            List<Object[]> byCategory = complaintService.countByCategory();
            List<Object[]> byStatus   = complaintService.countByStatus();
            List<Object[]> byMonth    = complaintService.countByMonth();
            List<Object[]> byDept     = assignmentService.countByDepartment();

            model.addAttribute("categoryLabels",   objectMapper.writeValueAsString(byCategory.stream().map(r -> r[0].toString()).toList()));
            model.addAttribute("categoryData",     objectMapper.writeValueAsString(byCategory.stream().map(r -> r[1]).toList()));
            model.addAttribute("statusLabels",     objectMapper.writeValueAsString(byStatus.stream().map(r -> r[0].toString()).toList()));
            model.addAttribute("statusData",       objectMapper.writeValueAsString(byStatus.stream().map(r -> r[1]).toList()));
            model.addAttribute("monthLabels",      objectMapper.writeValueAsString(byMonth.stream().map(r -> r[1] + "-" + String.format("%02d",(int)r[0])).toList()));
            model.addAttribute("monthData",        objectMapper.writeValueAsString(byMonth.stream().map(r -> r[2]).toList()));

            List<HotspotDetector.HotspotResult> hotspots = hotspotDetector.detectHotspots();
            model.addAttribute("hotspotsJson", objectMapper.writeValueAsString(
                hotspots.stream().map(h -> Map.of("lat", h.latitude(),"lng", h.longitude(),"count", h.count())).toList()
            ));
        } catch (Exception e) {
            model.addAttribute("categoryLabels","[]"); model.addAttribute("categoryData","[]");
            model.addAttribute("statusLabels","[]");   model.addAttribute("statusData","[]");
            model.addAttribute("monthLabels","[]");    model.addAttribute("monthData","[]");
            model.addAttribute("hotspotsJson","[]");
        }
        model.addAttribute("admin", getAdmin(p));
        return "admin/analytics";
    }

    // ===== USER MANAGEMENT =====
    @GetMapping("/users")
    public String users(@AuthenticationPrincipal CustomUserDetails p,
                        @RequestParam(required=false) String q, Model model) {
        List<?> users = (q != null && !q.isBlank()) ? userService.searchUsers(q) : userService.getAllCitizens();
        model.addAttribute("users", users);
        model.addAttribute("query", q);
        model.addAttribute("admin", getAdmin(p));
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id,
                             @RequestParam boolean enabled, RedirectAttributes ra) {
        userService.setEnabled(id, enabled);
        ra.addFlashAttribute("success", "User account " + (enabled ? "activated" : "deactivated") + ".");
        return "redirect:/admin/users";
    }

    // ===== SAFETY ALERTS =====
    @GetMapping("/alerts")
    public String alerts(@AuthenticationPrincipal CustomUserDetails p, Model model) {
        model.addAttribute("alerts", alertService.getAll());
        model.addAttribute("admin", getAdmin(p));
        return "admin/alerts";
    }

    @PostMapping("/alerts/create")
    public String createAlert(@RequestParam String title,
                              @RequestParam String description,
                              @RequestParam(required=false) String location,
                              @RequestParam(required=false) String priority,
                              @RequestParam(required=false) String safetyInstructions,
                              @AuthenticationPrincipal CustomUserDetails p,
                              RedirectAttributes ra) {
        SafetyAlert alert = new SafetyAlert();
        alert.setTitle(title);
        alert.setDescription(description);
        alert.setLocation(location);
        if (priority != null && !priority.isBlank())
            alert.setPriority(Complaint.Priority.valueOf(priority));
        alert.setSafetyInstructions(safetyInstructions);
        alert.setStatus(SafetyAlert.AlertStatus.ACTIVE);
        alert.setCreatedBy(getAdmin(p));
        alertService.create(alert);
        ra.addFlashAttribute("success", "Safety alert published.");
        return "redirect:/admin/alerts";
    }

    @PostMapping("/alerts/{id}/resolve")
    public String resolveAlert(@PathVariable Long id, RedirectAttributes ra) {
        alertService.updateStatus(id, SafetyAlert.AlertStatus.RESOLVED);
        ra.addFlashAttribute("success", "Alert resolved.");
        return "redirect:/admin/alerts";
    }
}
