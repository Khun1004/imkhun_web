package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Notification;
import com.imkhun.imkhun.dto.NotificationResponse;
import com.imkhun.imkhun.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyStudent(String username, String type, String message, Long postId) {
        if (username == null) return;
        notificationRepository.save(Notification.forStudent(username, type, message, postId));
    }

    @Transactional
    public void notifyAdmin(String type, String message, Long postId) {
        notificationRepository.save(Notification.forAdmin(type, message, postId));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getStudentNotifications(String username) {
        return notificationRepository.findByRecipientTypeAndRecipientUsernameOrderByCreatedAtDesc("STUDENT", username)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAdminNotifications() {
        return notificationRepository.findByRecipientTypeOrderByCreatedAtDesc("ADMIN")
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForStudent(String username) {
        return notificationRepository.countByRecipientTypeAndRecipientUsernameAndIsReadFalse("STUDENT", username);
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForAdmin() {
        return notificationRepository.countByRecipientTypeAndIsReadFalse("ADMIN");
    }

    @Transactional
    public void markRead(Long id) {
        notificationRepository.findById(id).ifPresent(Notification::markRead);
    }

    @Transactional
    public void markAllReadForStudent(String username) {
        notificationRepository.findByRecipientTypeAndRecipientUsernameOrderByCreatedAtDesc("STUDENT", username)
                .forEach(Notification::markRead);
    }

    @Transactional
    public void markAllReadForAdmin() {
        notificationRepository.findByRecipientTypeOrderByCreatedAtDesc("ADMIN")
                .forEach(Notification::markRead);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getPostId(), n.isRead(), n.getCreatedAt().format(DATE_FORMAT));
    }
}