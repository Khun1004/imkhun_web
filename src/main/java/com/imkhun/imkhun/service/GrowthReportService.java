package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.AttendanceRecord;
import com.imkhun.imkhun.domain.StudentLevelRecord;
import com.imkhun.imkhun.dto.AssignmentResponse;
import com.imkhun.imkhun.dto.GrowthReportResponse;
import com.imkhun.imkhun.dto.LevelRecordResponse;
import com.imkhun.imkhun.repository.AttendanceRecordRepository;
import com.imkhun.imkhun.repository.StudentLevelRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

// 학생 본인이 자기 레벨 변화 / 출석률 / 숙제 완료율을 한눈에 볼 수 있게 모아주는 서비스.
// 학생이 신청한(승인된) 모든 강의를 다 합쳐서 계산함 (강의 하나만 보는 게 아니라 전체 기준)
@Service
public class GrowthReportService {

    private static final Set<String> ATTENDED_STATUSES = Set.of("PRESENT", "LATE", "MAKEUP");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StudentLevelRecordRepository studentLevelRecordRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AssignmentService assignmentService;
    private final StudentAuthService studentAuthService;

    public GrowthReportService(StudentLevelRecordRepository studentLevelRecordRepository,
                               AttendanceRecordRepository attendanceRecordRepository,
                               AssignmentService assignmentService, StudentAuthService studentAuthService) {
        this.studentLevelRecordRepository = studentLevelRecordRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.assignmentService = assignmentService;
        this.studentAuthService = studentAuthService;
    }

    @Transactional(readOnly = true)
    public GrowthReportResponse getReport(String username) {
        List<Application> applications = studentAuthService.getApprovedApplications(username);
        List<Long> applicationIds = applications.stream().map(Application::getId).toList();

        // 레벨 기록 — 오래된 것부터 최신순으로 (성장 타임라인이니까)
        List<LevelRecordResponse> levelHistory = applicationIds.stream()
                .flatMap(id -> studentLevelRecordRepository.findByApplicationIdOrderByRecordedDateDescCreatedAtDesc(id).stream())
                .sorted(Comparator.comparing(StudentLevelRecord::getRecordedDate))
                .map(r -> new LevelRecordResponse(r.getId(), r.getLevel(), r.getNote(),
                        r.getRecordedDate().format(DATE_FORMAT), r.getCreatedAt().format(DATETIME_FORMAT)))
                .toList();

        // 출석률 — 전체 강의 통틀어서
        List<AttendanceRecord> allAttendance = applicationIds.stream()
                .flatMap(id -> attendanceRecordRepository.findByApplicationIdOrderByClassDateDesc(id).stream())
                .toList();
        long totalAttendance = allAttendance.size();
        long attended = allAttendance.stream().filter(r -> ATTENDED_STATUSES.contains(r.getStatus())).count();
        double attendanceRate = totalAttendance == 0 ? 0 : round1((attended * 100.0) / totalAttendance);

        // 숙제 완료율
        List<AssignmentResponse> assignments = assignmentService.getForStudent(username);
        long totalAssignments = assignments.size();
        long completedAssignments = assignments.stream().filter(AssignmentResponse::completed).count();
        double assignmentRate = totalAssignments == 0 ? 0 : round1((completedAssignments * 100.0) / totalAssignments);

        return new GrowthReportResponse(levelHistory, attendanceRate, totalAttendance,
                assignmentRate, totalAssignments, completedAssignments);
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}