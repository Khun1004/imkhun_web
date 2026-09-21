package com.imkhun.imkhun.service;

import com.imkhun.imkhun.dto.AssignmentResponse;
import com.imkhun.imkhun.dto.TodaySummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

// 학생이 로그인하자마자 볼 "오늘 할 일" 요약 — 오늘 수업/체크인 상태, 마감 임박 숙제, 안 읽은 알림 개수를 한번에 모아줌
@Service
public class StudentDashboardService {

    // 이 날짜(오늘 포함)까지의 숙제를 "임박"으로 침 — 지난 마감(밀린 숙제)도 포함해서 보여줌
    private static final int UPCOMING_DAYS = 3;

    private final AttendanceService attendanceService;
    private final AssignmentService assignmentService;
    private final NotificationService notificationService;

    public StudentDashboardService(AttendanceService attendanceService, AssignmentService assignmentService,
                                   NotificationService notificationService) {
        this.attendanceService = attendanceService;
        this.assignmentService = assignmentService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public TodaySummaryResponse getTodaySummary(String username) {
        var checkinStatus = attendanceService.getCheckinStatusForStudent(username);

        LocalDate cutoff = LocalDate.now().plusDays(UPCOMING_DAYS);
        List<AssignmentResponse> upcomingAssignments = assignmentService.getForStudent(username).stream()
                .filter(a -> !a.completed())
                .filter(a -> a.dueDate() != null)
                .filter(a -> !LocalDate.parse(a.dueDate()).isAfter(cutoff))
                .sorted(Comparator.comparing(AssignmentResponse::dueDate))
                .toList();

        long unreadCount = notificationService.getUnreadCountForStudent(username);

        return new TodaySummaryResponse(checkinStatus, upcomingAssignments, unreadCount);
    }
}