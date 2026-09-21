package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.AttendanceRecord;
import com.imkhun.imkhun.dto.AssignmentResponse;
import com.imkhun.imkhun.dto.CalendarEventResponse;
import com.imkhun.imkhun.repository.AttendanceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 학생의 한 달 달력에 표시할 이벤트(수업/숙제)를 모아주는 서비스
@Service
public class StudentCalendarService {

    private static final Map<DayOfWeek, String> DAY_CODE = Map.of(
            DayOfWeek.MONDAY, "MON", DayOfWeek.TUESDAY, "TUE", DayOfWeek.WEDNESDAY, "WED",
            DayOfWeek.THURSDAY, "THU", DayOfWeek.FRIDAY, "FRI", DayOfWeek.SATURDAY, "SAT", DayOfWeek.SUNDAY, "SUN"
    );

    private final StudentAuthService studentAuthService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AssignmentService assignmentService;

    public StudentCalendarService(StudentAuthService studentAuthService, AttendanceRecordRepository attendanceRecordRepository,
                                  AssignmentService assignmentService) {
        this.studentAuthService = studentAuthService;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.assignmentService = assignmentService;
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getMonthEvents(String username, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<CalendarEventResponse> events = new ArrayList<>();
        List<Application> approved = studentAuthService.getApprovedApplications(username);

        for (Application app : approved) {
            if (app.getClassDays() == null || app.getClassDays().isBlank() || app.getClassTime() == null) continue;

            List<String> dayCodes = List.of(app.getClassDays().split(","));

            Map<LocalDate, String> statusByDate = attendanceRecordRepository
                    .findByApplicationIdOrderByClassDateDesc(app.getId()).stream()
                    .collect(java.util.stream.Collectors.toMap(AttendanceRecord::getClassDate, AttendanceRecord::getStatus, (a, b) -> a));

            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                String dayCode = DAY_CODE.get(date.getDayOfWeek());
                if (!dayCodes.contains(dayCode)) continue;

                events.add(new CalendarEventResponse(date.toString(), "CLASS", app.getCourseName(), statusByDate.get(date)));
            }
        }

        List<AssignmentResponse> assignments = assignmentService.getForStudent(username);
        for (AssignmentResponse a : assignments) {
            if (a.dueDate() == null) continue;
            LocalDate due;
            try {
                due = LocalDate.parse(a.dueDate());
            } catch (Exception e) {
                continue;
            }
            if (due.isBefore(start) || due.isAfter(end)) continue;

            events.add(new CalendarEventResponse(due.toString(), "ASSIGNMENT", a.title(), a.completed() ? "완료" : "미완료"));
        }

        return events;
    }
}