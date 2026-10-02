package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.repository.ApplicationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// 수업 하루 전 저녁에, 내일 수업이 있는 학생한테 미리 알려주는 서비스.
// (카카오톡/문자 연동은 별도 계약이 필요해서 당장은 어렵고, 우선 사이트 내 알림으로 보내줌)
@Service
public class ClassReminderService {

    private static final Map<DayOfWeek, String> DAY_CODE = Map.of(
            DayOfWeek.MONDAY, "MON", DayOfWeek.TUESDAY, "TUE", DayOfWeek.WEDNESDAY, "WED",
            DayOfWeek.THURSDAY, "THU", DayOfWeek.FRIDAY, "FRI", DayOfWeek.SATURDAY, "SAT", DayOfWeek.SUNDAY, "SUN"
    );

    private static final Map<DayOfWeek, String> DAY_LABEL_KO = Map.of(
            DayOfWeek.MONDAY, "월요일", DayOfWeek.TUESDAY, "화요일", DayOfWeek.WEDNESDAY, "수요일",
            DayOfWeek.THURSDAY, "목요일", DayOfWeek.FRIDAY, "금요일", DayOfWeek.SATURDAY, "토요일", DayOfWeek.SUNDAY, "일요일"
    );

    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public ClassReminderService(ApplicationRepository applicationRepository, NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    // 매일 저녁 8시에 "내일 수업 있는 학생" 목록을 확인해서 알림을 보냄
    @Scheduled(cron = "0 0 20 * * *")
    @Transactional
    public void sendTomorrowClassReminders() {
        sendReminders();
    }

    // 관리자가 화면에서 "지금 보내기"를 눌렀을 때도 같은 로직을 즉시 실행함
    @Transactional
    public int sendRemindersNow() {
        return sendReminders();
    }

    private int sendReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        DayOfWeek tomorrowDow = tomorrow.getDayOfWeek();
        String tomorrowCode = DAY_CODE.get(tomorrowDow);
        String tomorrowLabel = DAY_LABEL_KO.get(tomorrowDow);

        List<Application> candidates = applicationRepository.findByStatusAndClassDaysIsNotNull("APPROVED").stream()
                .filter(a -> a.getClassDays() != null && List.of(a.getClassDays().split(",")).contains(tomorrowCode))
                .toList();

        int sentCount = 0;
        for (Application application : candidates) {
            // 오늘 이미 이 학생한테 "내일 수업" 알림을 보냈으면 중복으로 또 보내지 않음
            if (tomorrow.equals(application.getClassReminderSentDate())) continue;

            String timePart = application.getClassTime() != null ? " " + application.getClassTime() : "";
            notificationService.notifyStudent(application.getUsername(), "CLASS_REMINDER_TOMORROW",
                    "내일(" + tomorrowLabel + timePart + ") " + application.getCourseName() + " 수업이 있어요. 잊지 말고 참여해주세요!", null);
            application.markClassReminderSent(tomorrow);
            applicationRepository.save(application);
            sentCount++;
        }
        return sentCount;
    }
}