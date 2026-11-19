package com.civicshield.config;

import com.civicshield.entity.Department;
import com.civicshield.entity.User;
import com.civicshield.repository.DepartmentRepository;
import com.civicshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@civicshield.gov}")
    private String adminEmail;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    @Override
    public void run(String... args) {
        createAdminIfNotExists();
        createDefaultDepartments();
    }

    private void createAdminIfNotExists() {
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setFullName(adminName);
            admin.setRole(User.Role.ADMIN);
            admin.setProvider(User.AuthProvider.LOCAL);
            admin.setEnabled(true);
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            userRepository.save(admin);
            log.warn("================================================");
            log.warn("DEFAULT ADMIN CREATED: {}", adminEmail);
            log.warn("DEFAULT PASSWORD: Admin@123");
            log.warn("CHANGE THIS PASSWORD IMMEDIATELY AFTER FIRST LOGIN");
            log.warn("================================================");
        }
    }

    private void createDefaultDepartments() {
        List<String[]> depts = List.of(
            new String[]{"Road Maintenance", "Roads, potholes, pavements and traffic infrastructure."},
            new String[]{"Electrical Maintenance", "Streetlights, electrical fixtures and public lighting."},
            new String[]{"Water Supply", "Water mains, leaks and water supply infrastructure."},
            new String[]{"Drainage and Sanitation", "Drains, sewers, manholes and sanitation."},
            new String[]{"Public Safety", "Fallen trees, open pits, collapsed structures and safety hazards."},
            new String[]{"General Infrastructure", "Public buildings, parks, benches and general structures."}
        );
        for (String[] d : depts) {
            if (!departmentRepository.existsByName(d[0])) {
                Department dept = new Department();
                dept.setName(d[0]);
                dept.setDescription(d[1]);
                dept.setActive(true);
                departmentRepository.save(dept);
                log.info("Created department: {}", d[0]);
            }
        }
    }
}
