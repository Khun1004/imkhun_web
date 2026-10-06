package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// 학생이 마이페이지에 등록해둔 생일(월/일)이 오늘이면, 사이트 알림으로 자동 축하 메시지를 보내줌.
// 연도는 저장 안 하니까 "같은 해에 또 보냈는지"는 birthdayNotifiedYear로만 구분함.
@Service
public class BirthdayReminderService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public BirthdayReminderService(UserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // 매일 아침 9시에 오늘 생일인 학생을 찾아서 축하 알림을 보냄
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendTodayBirthdayNotifications() {
        sendReminders();
    }

    // 관리자가 화면에서 "지금 보내기"를 눌렀을 때도 같은 로직을 즉시 실행함
    @Transactional
    public int sendRemindersNow() {
        return sendReminders();
    }

    private int sendReminders() {
        LocalDate today = LocalDate.now();
        int todayMonth = today.getMonthValue();
        int todayDay = today.getDayOfMonth();
        int thisYear = today.getYear();

        List<User> candidates = userRepository.findAll().stream()
                .filter(u -> u.getBirthMonth() != null && u.getBirthDay() != null)
                .filter(u -> u.getBirthMonth() == todayMonth && u.getBirthDay() == todayDay)
                .filter(u -> u.getBirthdayNotifiedYear() == null || u.getBirthdayNotifiedYear() != thisYear)
                .toList();

        int sentCount = 0;
        for (User user : candidates) {
            notificationService.notifyStudent(user.getUsername(), "BIRTHDAY",
                    (user.getNickname() != null ? user.getNickname() : "") + "님, 생일 축하해요! 🎉 오늘도 즐겁게 공부해봐요.", null);
            user.markBirthdayNotified(thisYear);
            userRepository.save(user);
            sentCount++;
        }
        return sentCount;
    }
}