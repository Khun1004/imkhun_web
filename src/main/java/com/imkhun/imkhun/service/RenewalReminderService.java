package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.repository.ApplicationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// 수강 종료일(enrollmentEndDate)이 며칠 안 남은 학생한테 재등록 리마인더 알림을 보내는 서비스.
// 관리자가 "학생 관리"에서 종료일을 등록해둬야만 대상이 됨 (안 정해둔 학생은 그냥 건너뜀).
@Service
public class RenewalReminderService {

    // 종료일 며칠 전부터 알림을 보낼지
    private static final int REMIND_DAYS_BEFORE = 7;

    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public RenewalReminderService(ApplicationRepository applicationRepository, NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    // 매일 아침 9시에 확인
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendRenewalReminders() {
        LocalDate today = LocalDate.now();
        LocalDate remindUntil = today.plusDays(REMIND_DAYS_BEFORE);

        List<Application> approved = applicationRepository.findByStatus("APPROVED");

        for (Application application : approved) {
            LocalDate endDate = application.getEnrollmentEndDate();
            if (endDate == null) continue; // 종료일 자체를 안 정해둔 학생은 대상 아님
            if (application.getRenewalReminderSentAt() != null) continue; // 이미 이 종료일 기준으로 보냈음

            boolean withinReminderWindow = !endDate.isBefore(today) && !endDate.isAfter(remindUntil);
            if (!withinReminderWindow) continue;

            notificationService.notifyStudent(application.getUsername(), "RENEWAL_REMINDER",
                    application.getCourseName() + " 수강 기간이 " + endDate + "에 끝나요. 재등록을 원하시면 선생님께 문의해주세요.", null);
            application.markRenewalReminderSent();
            applicationRepository.save(application);
        }
    }
}