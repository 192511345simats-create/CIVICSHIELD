package com.civicshield.controller;

import com.civicshield.entity.Assignment;
import com.civicshield.security.CustomUserDetails;
import com.civicshield.service.AssignmentService;
import com.civicshield.service.RepairEvidenceService;
import com.civicshield.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/worker")
@RequiredArgsConstructor
public class WorkerController {

    private final UserService userService;
    private final AssignmentService assignmentService;
    private final RepairEvidenceService repairEvidenceService;

    private com.civicshield.entity.User getCurrentUser(CustomUserDetails principal) {
        return userService.getById(principal.getId());
    }

    /** Dashboard – list of assignments for this worker */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        var user = getCurrentUser(principal);
        model.addAttribute("assignments", assignmentService.getByWorker(user));
        model.addAttribute("user", user);
        return "worker/dashboard";
    }

    /** View a single assignment */
    @GetMapping("/assignments/{id}")
    public String assignmentDetail(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal, Model model) {
        var user = getCurrentUser(principal);
        Assignment assignment = assignmentService.getById(id);
        // security: ensure assignment belongs to this worker
        if (assignment.getWorkerContact() != null && user.getEmail() != null
                && !assignment.getWorkerContact().equalsIgnoreCase(user.getEmail())) {
            return "redirect:/worker/dashboard";
        }
        model.addAttribute("assignment", assignment);
        model.addAttribute("complaint", assignment.getComplaint());
        model.addAttribute("user", user);
        return "worker/assignment-detail";
    }

    /** Update assignment status (PENDING, IN_PROGRESS, COMPLETED) */
    @PostMapping("/assignments/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes ra) {
        var user = getCurrentUser(principal);
        assignmentService.updateStatus(id, Assignment.AssignmentStatus.valueOf(status), user);
        ra.addFlashAttribute("success", "Status updated.");
        return "redirect:/worker/assignments/" + id;
    }

    /** Upload before/after repair images */
    @PostMapping("/assignments/{id}/evidence")
    public String uploadEvidence(@PathVariable Long id,
                                 @RequestParam(required = false) MultipartFile before,
                                 @RequestParam(required = false) MultipartFile after,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes ra) throws Exception {
        var user = getCurrentUser(principal);
        repairEvidenceService.saveEvidence(id, before, after, user);
        ra.addFlashAttribute("success", "Repair evidence uploaded.");
        return "redirect:/worker/assignments/" + id;
    }
}
