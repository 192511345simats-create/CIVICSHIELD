package com.civicshield.service;

import com.civicshield.dto.UserRegistrationDTO;
import com.civicshield.entity.User;
import com.civicshield.exception.ResourceNotFoundException;
import com.civicshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(UserRegistrationDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }
        if (!dto.isPasswordMatch()) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User user = new User();
        user.setFullName(dto.getFullName().trim());
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setMobile(dto.getMobile());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.CITIZEN);
        user.setProvider(User.AuthProvider.LOCAL);
        user.setEnabled(true);

        user = userRepository.save(user);
        log.info("New citizen registered: {}", user.getEmail());
        return user;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional
    public User updateProfile(Long userId, String fullName, String mobile) {
        User user = getById(userId);
        user.setFullName(fullName.trim());
        user.setMobile(mobile);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public String generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("No account found with this email."));
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiry(java.time.LocalDateTime.now().plusHours(2));
        userRepository.save(user);
        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset link."));
        if (user.getPasswordResetExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("This reset link has expired. Please request a new one.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);
    }

    @Transactional
    public void setEnabled(Long userId, boolean enabled) {
        User user = getById(userId);
        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("User {} status set to: {}", user.getEmail(), enabled ? "active" : "deactivated");
    }

    public List<User> getAllCitizens() {
        return userRepository.findByRole(User.Role.CITIZEN);
    }

    public List<User> searchUsers(String query) {
        return userRepository.searchUsers(query);
    }

    public long countCitizens() {
        return userRepository.countByRole(User.Role.CITIZEN);
    }

    public long countNewUsersToday() {
        return userRepository.countByCreatedAtAfter(java.time.LocalDateTime.now().minusDays(1));
    }
}
