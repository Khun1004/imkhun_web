package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.dto.MypageCourseSummaryResponse;
import com.imkhun.imkhun.dto.MypageSummaryResponse;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.AttendanceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

// 학생 마이페이지 "내 수강 정보" 탭 맨 위에 보여줄 요약 카드용 — 승인된 강의별 출석 집계와
// 다음 납부(수강 종료)일까지 남은 일수를 한 번에 모아서 내려줌
@Service
public class MypageSummaryService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final ApplicationRepository applicationRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public MypageSummaryService(ApplicationRepository applicationRepository,
                                AttendanceRecordRepository attendanceRecordRepository) {
        this.applicationRepository = applicationRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    @Transactional(readOnly = true)
    public MypageSummaryResponse getSummaryForStudent(String username) {
        List<Application> approved = applicationRepository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .filter(a -> "APPROVED".equals(a.getStatus()))
                .toList();

        LocalDate today = LocalDate.now();
        long totalPresent = 0, totalLate = 0, totalAbsent = 0, totalMakeup = 0;
        List<MypageCourseSummaryResponse> courses = new java.util.ArrayList<>();

        for (Application a : approved) {
            long present = attendanceRecordRepository.countByApplicationIdAndStatus(a.getId(), "PRESENT");
            long late = attendanceRecordRepository.countByApplicationIdAndStatus(a.getId(), "LATE");
            long absent = attendanceRecordRepository.countByApplicationIdAndStatus(a.getId(), "ABSENT");
            long makeup = attendanceRecordRepository.countByApplicationIdAndStatus(a.getId(), "MAKEUP");

            totalPresent += present;
            totalLate += late;
            totalAbsent += absent;
            totalMakeup += makeup;

            LocalDate endDate = a.getEnrollmentEndDate();
            String endDateStr = endDate != null ? endDate.format(DATE_FORMAT) : null;
            Long daysUntilDue = endDate != null ? ChronoUnit.DAYS.between(today, endDate) : null;

            courses.add(new MypageCourseSummaryResponse(
                    a.getId(), a.getCourseName(), a.getStudentNumber(),
                    present, late, absent, makeup, endDateStr, daysUntilDue,
                    a.getPaymentConfirmedByAdminAt() != null
            ));
        }

        return new MypageSummaryResponse(courses, totalPresent, totalLate, totalAbsent, totalMakeup);
    }
}