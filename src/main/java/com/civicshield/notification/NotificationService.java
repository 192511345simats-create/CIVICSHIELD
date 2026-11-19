package com.civicshield.notification;

import com.civicshield.entity.*;
import com.civicshield.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notifyUser(User user, Complaint complaint, String title, String message,
                           Notification.NotificationType type) {
        Notification n = new Notification();
        n.setUser(user);
        n.setComplaint(complaint);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setRead(false);
        notificationRepository.save(n);
    }

    @Transactional
    public void notifyComplaintReceived(Complaint c) {
        notifyUser(c.getUser(), c, "Report Received",
            "Your report " + c.getComplaintId() + " has been received. We will review it shortly.",
            Notification.NotificationType.INFO);
    }

    @Transactional
    public void notifyStatusChange(Complaint c, Complaint.Status newStatus) {
        Notification.NotificationType type = switch (newStatus) {
            case RESOLVED    -> Notification.NotificationType.SUCCESS;
            case REJECTED    -> Notification.NotificationType.DANGER;
            default          -> Notification.NotificationType.INFO;
        };
        notifyUser(c.getUser(), c, "Report Update",
            "Your report " + c.getComplaintId() + " status is now: " + newStatus.getDisplayName() + ".",
            type);
    }

    public List<Notification> getUnread(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    public long countUnread(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAllRead(User user) {
        List<Notification> list = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        list.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(list);
    }

    public List<Notification> getAll(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
