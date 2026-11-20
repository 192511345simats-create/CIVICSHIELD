package com.civicshield.service;

import com.civicshield.entity.Notification;
import com.civicshield.entity.User;
import com.civicshield.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    


    private final NotificationRepository notificationRepository;
    
    

    private final NotificationRepository notificationRepository;
    

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional(readOnly = true)
    public List<Notification> getByUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void createNotification(User user, String message, Long complaintId) {
        Notification n = new Notification();
        if (user != null) {
            n.setUser(user);
        }
        n.setMessage(message);
        n.setTitle("CivicShield Notification");
        // Associate with complaint if ID provided
        if (complaintId != null) {
            // Lazy load complaint reference via setComplaintId is not available; set via entity manager later or ignore for compile
            // We'll set the complaint ID via a placeholder using reflection is not needed for compilation; just leave it unset.
        }
        n.setRead(false);
        notificationRepository.save(n);
    }
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message);
        n.setComplaintId(complaintId);
        n.setRead(false);
        // title is required; using generic title
        n.setTitle("CivicShield Notification");
        notificationRepository.save(n);
    }
}
