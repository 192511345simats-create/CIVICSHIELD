package com.civicshield.repository;

import com.civicshield.entity.Assignment;
import com.civicshield.entity.Complaint;
import com.civicshield.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Optional<Assignment> findByComplaint(Complaint complaint);
    List<Assignment> findByDepartment(Department department);
    List<Assignment> findByStatus(Assignment.AssignmentStatus status);
    List<Assignment> findByWorkerContactIgnoreCase(String email);

    @Query("SELECT a FROM Assignment a WHERE a.deadline < :now AND a.status NOT IN ('COMPLETED') ORDER BY a.deadline ASC")
    List<Assignment> findOverdueAssignments(@Param("now") LocalDateTime now);

    @Query("SELECT a.department, COUNT(a) FROM Assignment a GROUP BY a.department")
    List<Object[]> countByDepartment();

    @Query("SELECT a.department, COUNT(a) FROM Assignment a WHERE a.status = 'COMPLETED' GROUP BY a.department")
    List<Object[]> countCompletedByDepartment();
}
