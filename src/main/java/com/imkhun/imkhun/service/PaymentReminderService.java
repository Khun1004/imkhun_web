package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.repository.ApplicationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentReminderService {

    // 결제 안내 등록(또는 마지막 리마인더) 후 이만큼 지나면 다시 리마인더를 보냄
    private static final long REMINDER_INTERVAL_DAYS = 3;

    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public PaymentReminderService(ApplicationRepository applicationRepository, NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    // 매일 오전 9시에 자동으로 실행됨
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendDueReminders() {
        sendReminders();
    }

    // 관리자가 화면에서 "지금 보내기"를 눌렀을 때도 같은 로직을 즉시 실행함
    @Transactional
    public int sendRemindersNow() {
        return sendReminders();
    }

    private int sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Application> candidates = applicationRepository
                .findByStatusAndPaymentConfirmedByStudentAtIsNull("APPROVED");

        int sentCount = 0;
        for (Application application : candidates) {
            if (application.getPaymentInfoRegisteredAt() == null) continue; // 결제 안내 자체가 아직 없음

            LocalDateTime lastPrompt = application.getPaymentReminderSentAt() != null
                    ? application.getPaymentReminderSentAt()
                    : application.getPaymentInfoRegisteredAt();

            long daysSinceLastPrompt = Duration.between(lastPrompt, now).toDays();
            if (daysSinceLastPrompt >= REMINDER_INTERVAL_DAYS) {
                notificationService.notifyStudent(application.getUsername(), "PAYMENT_REMINDER",
                        application.getCourseName() + " 강의 결제 확인이 아직 안 됐어요. 잊지 말고 확인해주세요!", null);
                application.markPaymentReminderSent(now);
                applicationRepository.save(application);
                sentCount++;
            }
        }
        return sentCount;
    }
}