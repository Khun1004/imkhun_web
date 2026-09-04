package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientTypeAndRecipientUsernameOrderByCreatedAtDesc(String recipientType, String recipientUsername);

    List<Notification> findByRecipientTypeOrderByCreatedAtDesc(String recipientType);

    long countByRecipientTypeAndRecipientUsernameAndIsReadFalse(String recipientType, String recipientUsername);

    long countByRecipientTypeAndIsReadFalse(String recipientType);
}