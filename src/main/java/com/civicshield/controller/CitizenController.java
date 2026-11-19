package com.civicshield.controller;

import com.civicshield.dto.ComplaintSubmitDTO;
import com.civicshield.entity.*;
import com.civicshield.notification.NotificationService;
import com.civicshield.repository.ComplaintRepository;
import com.civicshield.security.CustomUserDetails;
import com.civicshield.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/citizen")
@RequiredArgsConstructor
public class CitizenController {

    private final UserService userService;
    private final ComplaintService complaintService;
    private final SafetyAlertService alertService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    // Helper to get current user
    private User getCurrentUser(CustomUserDetails principal) {
        return userService.getById(principal.getId());
    }

    // ===== HOME =====
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = getCurrentUser(principal);
        List<Complaint> myComplaints = complaintService.getByUser(user);
        long total    = myComplaints.size();
        long waiting  = myComplaints.stream().filter(c -> c.getStatus() != Complaint.Status.RESOLVED && c.getStatus() != Complaint.Status.REJECTED).count();
        long resolved = myComplaints.stream().filter(c -> c.getStatus() == Complaint.Status.RESOLVED).count();

        model.addAttribute("user", user);
        model.addAttribute("totalReports", total);
        model.addAttribute("waitingReports", waiting);
        model.addAttribute("resolvedReports", resolved);
        model.addAttribute("recentComplaints", myComplaints.stream().limit(5).toList());
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        return "citizen/home";
    }

    // ===== REPORT =====
    @GetMapping("/report")
    public String reportPage(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("dto", new ComplaintSubmitDTO());
        model.addAttribute("categories", Complaint.Category.values());
        model.addAttribute("user", getCurrentUser(principal));
        model.addAttribute("unreadCount", notificationService.countUnread(getCurrentUser(principal)));
        return "citizen/report";
    }

    @PostMapping("/report")
    public String submitReport(@AuthenticationPrincipal CustomUserDetails principal,
                               @Valid @ModelAttribute("dto") ComplaintSubmitDTO dto,
                               BindingResult result,
                               @RequestParam(value = "image", required = false) MultipartFile image,
                               RedirectAttributes ra, Model model) {
        User user = getCurrentUser(principal);
        if (result.hasErrors()) {
            model.addAttribute("categories", Complaint.Category.values());
            model.addAttribute("user", user);
            return "citizen/report";
        }
        try {
            Complaint complaint = complaintService.submit(dto, user, image);
            ra.addFlashAttribute("success",
                "Your report has been submitted! Complaint ID: " + complaint.getComplaintId());
            if (complaint.isPossibleDuplicate()) {
                ra.addFlashAttribute("duplicateWarning",
                    "A similar complaint was found nearby (ID: CS-" + complaint.getDuplicateOfComplaintId()
                    + "). Your report has been submitted. You can also confirm the existing report.");
            }
            return "redirect:/citizen/complaints";
        } catch (Exception ex) {
            model.addAttribute("error", "Could not submit your report: " + ex.getMessage());
            model.addAttribute("categories", Complaint.Category.values());
            model.addAttribute("user", user);
            return "citizen/report";
        }
    }

    // ===== MY COMPLAINTS =====
    @GetMapping("/complaints")
    public String myComplaints(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = getCurrentUser(principal);
        model.addAttribute("complaints", complaintService.getByUser(user));
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        return "citizen/complaints";
    }

    @GetMapping("/complaints/{id}")
    public String complaintDetail(@AuthenticationPrincipal CustomUserDetails principal,
                                  @PathVariable Long id, Model model) {
        User user = getCurrentUser(principal);
        Complaint complaint = complaintService.getById(id);
        // Citizens can only see their own complaints
        if (!complaint.getUser().getId().equals(user.getId())) {
            return "redirect:/citizen/complaints";
        }
        model.addAttribute("complaint", complaint);
        model.addAttribute("timeline", complaintService.getTimeline(complaint));
        model.addAttribute("assignment", complaintService.getAssignment(complaint).orElse(null));
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        return "citizen/complaint-detail";
    }

    // ===== CONFIRM COMPLAINT =====
    @PostMapping("/complaints/{id}/confirm")
    public String confirmComplaint(@AuthenticationPrincipal CustomUserDetails principal,
                                   @PathVariable Long id,
                                   @RequestParam(required=false) String note,
                                   RedirectAttributes ra) {
        User user = getCurrentUser(principal);
        boolean confirmed = complaintService.confirmComplaint(id, user, note);
        if (confirmed) ra.addFlashAttribute("success", "Thank you! Your confirmation has been recorded.");
        else ra.addFlashAttribute("info", "You have already confirmed this report or cannot confirm your own report.");
        return "redirect:/citizen/nearby";
    }

    // ===== NEARBY ISSUES =====
    @GetMapping("/nearby")
    public String nearbyIssues(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = getCurrentUser(principal);
        try {
            // Get all public complaints with location data
            List<Complaint> allWithLocation = complaintService.getNearby(0, 0, 90);
            String complaintsJson = objectMapper.writeValueAsString(
                allWithLocation.stream().map(c -> new java.util.HashMap<String, Object>() {{
                    put("id", c.getId());
                    put("complaintId", c.getComplaintId());
                    put("lat", c.getLatitude());
                    put("lng", c.getLongitude());
                    put("category", c.getCategory().getDisplayName());
                    put("status", c.getStatus().getDisplayName());
                    put("priority", c.getPriority() != null ? c.getPriority().getDisplayName() : "");
                    put("address", c.getLocationAddress());
                }}).toList()
            );
            model.addAttribute("complaintsJson", complaintsJson);
        } catch (Exception e) {
            model.addAttribute("complaintsJson", "[]");
        }
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        return "citizen/nearby";
    }

    // ===== SAFETY ALERTS =====
    @GetMapping("/alerts")
    public String safetyAlerts(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = getCurrentUser(principal);
        model.addAttribute("alerts", alertService.getActive());
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        return "citizen/alerts";
    }

    // ===== PROFILE =====
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = getCurrentUser(principal);
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        return "citizen/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                @RequestParam String fullName,
                                @RequestParam(required=false) String mobile,
                                RedirectAttributes ra) {
        userService.updateProfile(principal.getId(), fullName, mobile);
        ra.addFlashAttribute("success", "Your profile has been updated.");
        return "redirect:/citizen/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmNewPassword,
                                 RedirectAttributes ra) {
        if (!newPassword.equals(confirmNewPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match.");
            return "redirect:/citizen/profile";
        }
        try {
            userService.changePassword(principal.getId(), currentPassword, newPassword);
            ra.addFlashAttribute("success", "Password changed successfully.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/citizen/profile";
    }

    // ===== NOTIFICATIONS =====
    @PostMapping("/notifications/mark-read")
    public String markNotificationsRead(@AuthenticationPrincipal CustomUserDetails principal,
                                        RedirectAttributes ra) {
        notificationService.markAllRead(getCurrentUser(principal));
        return "redirect:/citizen/home";
    }
}
