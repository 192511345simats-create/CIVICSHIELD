package com.civicshield.controller;

import com.civicshield.dto.UserRegistrationDTO;
import com.civicshield.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/")
    public String landing() { return "landing/index"; }

    @GetMapping("/login")
    public String loginPage() { return "auth/login"; }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("dto", new UserRegistrationDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("dto") UserRegistrationDTO dto,
                           BindingResult result, Model model) {
        if (!dto.isPasswordMatch()) {
            model.addAttribute("error", "Passwords do not match.");
            return "auth/register";
        }
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(dto);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() { return "error/403"; }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() { return "auth/forgot-password"; }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, Model model) {
        try {
            userService.generatePasswordResetToken(email);
            model.addAttribute("success", "If an account exists for that email, a reset link has been generated. Contact your administrator to retrieve it.");
        } catch (Exception e) {
            model.addAttribute("success", "If an account exists for that email, a reset link has been generated.");
        }
        return "auth/forgot-password";
    }
}
