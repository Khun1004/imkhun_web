package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.AttendanceRecord;
import com.imkhun.imkhun.dto.DashboardResponse;
import com.imkhun.imkhun.dto.TopAbsentStudentResponse;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.AttendanceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ApplicationRepository applicationRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudyPostService studyPostService;
    private final NotificationService notificationService;

    public DashboardService(ApplicationRepository applicationRepository, AttendanceRecordRepository attendanceRecordRepository,
                            StudyPostService studyPostService, NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
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

        // "내 학생" = 지금 승인 상태인 신청들의 학생 아이디를 중복 없이 센 수
        long totalStudents = applicationRepository.findByStatus("APPROVED").stream()
                .map(Application::getUsername)
                .distinct()
                .count();

        YearMonth thisMonth = YearMonth.now();
        List<AttendanceRecord> monthRecords = attendanceRecordRepository.findByClassDateBetween(
                thisMonth.atDay(1), thisMonth.atEndOfMonth());

        double attendanceRate = computeAttendanceRate(monthRecords);
        List<TopAbsentStudentResponse> topAbsent = computeTopAbsentStudents(monthRecords);

        return new DashboardResponse(pending, paymentPending, newThisMonth, unreadNotifications,
                totalStudents, attendanceRate, topAbsent, recentPosts);
    }

    // 출석/지각을 "나왔다"로 치고, 결석만 "안 나왔다"로 봐서 비율을 계산함 (보강은 집계에서 빼요 — 정규 수업이 아니라서요)
    private double computeAttendanceRate(List<AttendanceRecord> records) {
        long counted = records.stream().filter(r -> !"MAKEUP".equals(r.getStatus())).count();
        if (counted == 0) return 0;
        long present = records.stream()
                .filter(r -> "PRESENT".equals(r.getStatus()) || "LATE".equals(r.getStatus()))
                .count();
        return Math.round((present * 1000.0) / counted) / 10.0;
    }

    // 이번 달 결석이 가장 많은 학생 5명을 뽑아줌
    private List<TopAbsentStudentResponse> computeTopAbsentStudents(List<AttendanceRecord> records) {
        Map<Long, Long> absentCountByApplicationId = records.stream()
                .filter(r -> "ABSENT".equals(r.getStatus()))
                .collect(Collectors.groupingBy(AttendanceRecord::getApplicationId, Collectors.counting()));

        if (absentCountByApplicationId.isEmpty()) return List.of();

        Map<Long, Application> applicationById = applicationRepository.findAllById(absentCountByApplicationId.keySet())
                .stream()
                .collect(Collectors.toMap(Application::getId, a -> a));

        return absentCountByApplicationId.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Application app = applicationById.get(entry.getKey());
                    return new TopAbsentStudentResponse(
                            app != null ? app.getUsername() : "-",
                            app != null ? app.getCourseName() : "-",
                            entry.getValue()
                    );
                })
                .toList();
    }
}