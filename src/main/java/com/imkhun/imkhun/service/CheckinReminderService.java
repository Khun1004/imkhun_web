package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.repository.ApplicationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

// 학생의 출석 체크 버튼이 뜨는 순간(수업 시작 10분 전) 알림을 보내주는 서비스.
// AttendanceService의 체크 가능 시작 시각(CHECK_IN_OPEN_MINUTES_BEFORE)과 반드시 같은 값을 써야 함.
@Service
public class CheckinReminderService {

    private static final long NOTIFY_MINUTES_BEFORE = 10;

    private static final Map<DayOfWeek, String> DAY_CODE = Map.of(
            DayOfWeek.MONDAY, "MON", DayOfWeek.TUESDAY, "TUE", DayOfWeek.WEDNESDAY, "WED",
            DayOfWeek.THURSDAY, "THU", DayOfWeek.FRIDAY, "FRI", DayOfWeek.SATURDAY, "SAT", DayOfWeek.SUNDAY, "SUN"
    );

    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public CheckinReminderService(ApplicationRepository applicationRepository, NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    // 매분 정각에 실행 — 지금 막 체크 가능 시간이 열린 수업이 있으면 그 학생한테 알림을 보냄
    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void notifyCheckinWindowOpened() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        String todayCode = DAY_CODE.get(now.getDayOfWeek());

        List<Application> scheduledToday = applicationRepository.findByStatusAndClassDaysIsNotNull("APPROVED").stream()
                .filter(a -> a.getClassDays() != null && List.of(a.getClassDays().split(",")).contains(todayCode))
                .toList();

        for (Application app : scheduledToday) {
            if (app.getClassTime() == null) continue;
            try {
                LocalTime classTime = LocalTime.parse(app.getClassTime());
                LocalDateTime windowOpensAt = LocalDateTime.of(today, classTime).minusMinutes(NOTIFY_MINUTES_BEFORE);

                // 지금이 "체크 가능 시간이 막 열리는 그 순간"인지 (1분짜리 스케줄러라서 1분 폭으로 확인)
                if (!now.isBefore(windowOpensAt) && now.isBefore(windowOpensAt.plusMinutes(1))) {
                    notificationService.notifyStudent(app.getUsername(), "CHECKIN_AVAILABLE",
                            app.getCourseName() + " 수업 출석 체크를 할 수 있어요! 지금 체크해주세요.", null);
                }
            } catch (Exception ignored) {
                // 시간 형식이 이상한 데이터는 그냥 건너뜀
            }
        }
    }
}