package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.AttendanceRecord;
import com.imkhun.imkhun.dto.AttendanceStreakResponse;
import com.imkhun.imkhun.repository.AttendanceRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// 학생의 출석 기록을 보고 "연속 출석 스트릭"이랑 "이번 달 출석률"을 계산해서 배지를 만들어주는 서비스.
// PRESENT/LATE/MAKEUP은 "출석한 걸로" 치고, ABSENT를 만나면 스트릭이 끊김.
@Service
public class AttendanceStreakService {

    private static final Set<String> ATTENDED_STATUSES = Set.of("PRESENT", "LATE", "MAKEUP");

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentAuthService studentAuthService;

    public AttendanceStreakService(AttendanceRecordRepository attendanceRecordRepository, StudentAuthService studentAuthService) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.studentAuthService = studentAuthService;
    }

    public AttendanceStreakResponse getStreakForStudent(String username) {
        List<Application> applications = studentAuthService.getApprovedApplications(username);

        List<AttendanceRecord> allRecords = new ArrayList<>();
        for (Application application : applications) {
            allRecords.addAll(attendanceRecordRepository.findByApplicationIdOrderByClassDateDesc(application.getId()));
        }
        allRecords.sort((a, b) -> b.getClassDate().compareTo(a.getClassDate()));

        int currentStreak = 0;
        for (AttendanceRecord record : allRecords) {
            if (ATTENDED_STATUSES.contains(record.getStatus())) {
                currentStreak++;
            } else {
                break; // 결석을 만나면 거기서 스트릭 끊김
            }
        }

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        List<AttendanceRecord> thisMonthRecords = allRecords.stream()
                .filter(r -> !r.getClassDate().isBefore(monthStart) && !r.getClassDate().isAfter(today))
                .toList();
        long attendedThisMonth = thisMonthRecords.stream().filter(r -> ATTENDED_STATUSES.contains(r.getStatus())).count();
        double thisMonthRate = thisMonthRecords.isEmpty() ? 0
                : Math.round((attendedThisMonth * 1000.0) / thisMonthRecords.size()) / 10.0;

        List<String> badges = new ArrayList<>();
        if (currentStreak >= 3) badges.add("연속 3일 출석");
        if (currentStreak >= 5) badges.add("연속 5일 출석");
        if (currentStreak >= 10) badges.add("연속 10일 출석");
        if (currentStreak >= 20) badges.add("연속 20일 출석");
        if (!thisMonthRecords.isEmpty() && thisMonthRate == 100.0) badges.add("이번 달 출석률 100%");

        return new AttendanceStreakResponse(currentStreak, thisMonthRate, badges);
    }
}