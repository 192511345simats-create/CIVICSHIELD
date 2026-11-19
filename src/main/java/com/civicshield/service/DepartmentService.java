package com.civicshield.service;

import com.civicshield.entity.Department;
import com.civicshield.exception.ResourceNotFoundException;
import com.civicshield.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<Department> getAll() { return departmentRepository.findAll(); }
    public List<Department> getActive() { return departmentRepository.findByActiveTrue(); }

    public Department getById(Long id) {
        return departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    @Transactional
    public Department create(Department dept) {
        if (departmentRepository.existsByName(dept.getName())) {
            throw new IllegalArgumentException("A department with this name already exists.");
        }
        return departmentRepository.save(dept);
    }

    @Transactional
    public Department update(Long id, Department updated) {
        Department dept = getById(id);
        dept.setName(updated.getName());
        dept.setDescription(updated.getDescription());
        dept.setHeadName(updated.getHeadName());
        dept.setContactEmail(updated.getContactEmail());
        dept.setContactPhone(updated.getContactPhone());
        dept.setActive(updated.isActive());
        return departmentRepository.save(dept);
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        Department dept = getById(id);
        dept.setActive(active);
        departmentRepository.save(dept);
    }
}
