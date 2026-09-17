package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.Notice;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.NoticeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 예약 공지 글의 시간이 되면, 전체 학생한테 알림을 보내고 "알림 보냄" 표시를 해두는 서비스.
@Service
public class NoticeReminderService {

    private final NoticeRepository noticeRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public NoticeReminderService(NoticeRepository noticeRepository, ApplicationRepository applicationRepository,
                                 NotificationService notificationService) {
        this.noticeRepository = noticeRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    // 매분 정각에 확인 — 예약 시간이 된 공지가 있으면 전체 학생한테 알림 보냄
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendDueNoticeReminders() {
        List<Notice> pending = noticeRepository.findByNotifiedAtIsNull();
        if (pending.isEmpty()) return;

        List<Notice> due = pending.stream().filter(Notice::isPublishedNow).toList();
        if (due.isEmpty()) return;

        List<String> studentUsernames = applicationRepository.findByStatus("APPROVED").stream()
                .map(Application::getUsername)
                .distinct()
                .toList();

        for (Notice notice : due) {
            for (String username : studentUsernames) {
                notificationService.notifyStudent(username, "NEW_NOTICE", "새 공지: " + notice.getTitle(), notice.getId());
            }
            notice.markNotified();
            noticeRepository.save(notice);
        }
    }
}