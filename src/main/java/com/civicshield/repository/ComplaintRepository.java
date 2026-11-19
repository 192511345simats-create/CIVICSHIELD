package com.civicshield.repository;

import com.civicshield.entity.Complaint;
import com.civicshield.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Optional<Complaint> findByComplaintId(String complaintId);
    List<Complaint> findByUserOrderByCreatedAtDesc(User user);
    List<Complaint> findByStatus(Complaint.Status status);
    List<Complaint> findByPriority(Complaint.Priority priority);
    List<Complaint> findByCategory(Complaint.Category category);
    List<Complaint> findByDataType(Complaint.DataType dataType);
    List<Complaint> findAllByOrderByCreatedAtDesc();

    long countByStatus(Complaint.Status status);
    long countByPriority(Complaint.Priority priority);
    long countByDataType(Complaint.DataType dataType);

    @Query("SELECT c FROM Complaint c WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND c.status != 'REJECTED'")
    List<Complaint> findAllWithLocation();

    @Query("SELECT c FROM Complaint c WHERE c.latitude BETWEEN :minLat AND :maxLat AND c.longitude BETWEEN :minLng AND :maxLng AND c.status != 'REJECTED'")
    List<Complaint> findNearbyComplaints(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                         @Param("minLng") double minLng, @Param("maxLng") double maxLng);

    @Query("SELECT c FROM Complaint c WHERE c.category = :category AND c.createdAt > :since AND c.status != 'REJECTED' AND c.id != :excludeId")
    List<Complaint> findSimilarCategoryRecent(@Param("category") Complaint.Category category,
                                               @Param("since") LocalDateTime since,
                                               @Param("excludeId") Long excludeId);

    @Query("SELECT c.category, COUNT(c) FROM Complaint c WHERE c.dataType = 'CITIZEN_REPORT' GROUP BY c.category")
    List<Object[]> countByCategory();

    @Query("SELECT c.status, COUNT(c) FROM Complaint c WHERE c.dataType = 'CITIZEN_REPORT' GROUP BY c.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT MONTH(c.createdAt), YEAR(c.createdAt), COUNT(c) FROM Complaint c WHERE c.createdAt > :since AND c.dataType = 'CITIZEN_REPORT' GROUP BY YEAR(c.createdAt), MONTH(c.createdAt) ORDER BY YEAR(c.createdAt), MONTH(c.createdAt)")
    List<Object[]> countByMonth(@Param("since") LocalDateTime since);

    @Query("SELECT c FROM Complaint c WHERE c.priority IN ('HIGH', 'CRITICAL') AND c.status NOT IN ('RESOLVED', 'REJECTED') ORDER BY c.createdAt DESC")
    List<Complaint> findHighPriorityActive();

    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED', 'REJECTED') AND c.createdAt < :cutoff ORDER BY c.createdAt ASC")
    List<Complaint> findOverdue(@Param("cutoff") LocalDateTime cutoff);
}
