package com.civicshield.repository;

import com.civicshield.entity.Complaint;
import com.civicshield.entity.ComplaintUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComplaintUpdateRepository extends JpaRepository<ComplaintUpdate, Long> {
    List<ComplaintUpdate> findByComplaintOrderByUpdatedAtAsc(Complaint complaint);
}
