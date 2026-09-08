package com.imkhun.imkhun.service;

import com.imkhun.imkhun.dto.DashboardResponse;
import com.imkhun.imkhun.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DashboardService {

    private final ApplicationRepository applicationRepository;
    private final StudyPostService studyPostService;
    private final NotificationService notificationService;

    public DashboardService(ApplicationRepository applicationRepository, StudyPostService studyPostService,
                            NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.studyPostService = studyPostService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getSummary() {
        long pending = applicationRepository.countByStatus("PENDING");
        long paymentPending = applicationRepository.countByPaymentConfirmedByStudentAtIsNotNullAndPaymentConfirmedByAdminAtIsNull();

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long newThisMonth = applicationRepository.countByCreatedAtAfter(startOfMonth);

        long unreadNotifications = notificationService.getUnreadCountForAdmin();
        var recentPosts = studyPostService.getAllPostsForAdmin().stream().limit(5).toList();

        return new DashboardResponse(pending, paymentPending, newThisMonth, unreadNotifications, recentPosts);
    }
}