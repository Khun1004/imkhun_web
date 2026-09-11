package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.AttendanceRecord;
import com.imkhun.imkhun.dto.AttendanceHistoryEntryResponse;
import com.imkhun.imkhun.dto.AttendanceRecordResponse;
import com.imkhun.imkhun.dto.AttendanceSummaryResponse;
import com.imkhun.imkhun.dto.CheckinStatusResponse;
import com.imkhun.imkhun.dto.CreateAttendanceRecordRequest;
import com.imkhun.imkhun.dto.StudentScheduleEntryResponse;
import com.imkhun.imkhun.dto.TodayAttendanceEntryResponse;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.AttendanceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final Set<String> VALID_STATUSES = Set.of("PRESENT", "LATE", "ABSENT", "MAKEUP");

    // 요일 코드(MON~SUN) ↔ 자바의 DayOfWeek
    private static final Map<DayOfWeek, String> DAY_CODE = Map.of(
            DayOfWeek.MONDAY, "MON", DayOfWeek.TUESDAY, "TUE", DayOfWeek.WEDNESDAY, "WED",
            DayOfWeek.THURSDAY, "THU", DayOfWeek.FRIDAY, "FRI", DayOfWeek.SATURDAY, "SAT", DayOfWeek.SUNDAY, "SUN"
    );

    // 학생이 스스로 "출석하기"를 누를 수 있는 시간 범위 (수업 시작 30분 전 ~ 수업 시작 2시간 후)
    private static final long CHECK_IN_OPEN_MINUTES_BEFORE = 30;
    private static final long LATE_CUTOFF_MINUTES_AFTER = 15;
    private static final long CHECK_IN_CLOSE_MINUTES_AFTER = 120;

    public AttendanceService(AttendanceRecordRepository attendanceRecordRepository,
                             ApplicationRepository applicationRepository, NotificationService notificationService) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    // ---------- 개별 학생 출석부 (관리자가 학생 하나 클릭해서 보는 화면) ----------

    @Transactional
    public AttendanceSummaryResponse addRecord(Long applicationId, CreateAttendanceRecordRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));

        if (request.status() == null || !VALID_STATUSES.contains(request.status())) {
            throw new IllegalStateException("올바른 출석 상태를 선택해주세요.");
        }
        LocalDate classDate;
        try {
            classDate = LocalDate.parse(request.classDate());
        } catch (Exception e) {
            throw new IllegalStateException("날짜 형식이 올바르지 않아요.");
        }

        attendanceRecordRepository.save(AttendanceRecord.create(applicationId, classDate, request.status(), request.note()));

        if ("ABSENT".equals(request.status())) {
            notificationService.notifyStudent(application.getUsername(), "ATTENDANCE_ABSENT",
                    application.getCourseName() + " " + classDate.format(DATE_FORMAT) + " 수업이 결석으로 기록됐어요.", null);
            checkConsecutiveAbsences(application);
        }

        return getSummary(applicationId);
    }

    @Transactional
    public void deleteRecord(Long recordId) {
        if (!attendanceRecordRepository.existsById(recordId)) {
            throw new IllegalStateException("출석 기록을 찾을 수 없어요.");
        }
        attendanceRecordRepository.deleteById(recordId);
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getSummary(Long applicationId) {
        long present = attendanceRecordRepository.countByApplicationIdAndStatus(applicationId, "PRESENT");
        long absent = attendanceRecordRepository.countByApplicationIdAndStatus(applicationId, "ABSENT");
        long makeup = attendanceRecordRepository.countByApplicationIdAndStatus(applicationId, "MAKEUP");

        List<AttendanceRecordResponse> records = attendanceRecordRepository.findByApplicationIdOrderByClassDateDesc(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();

        return new AttendanceSummaryResponse(present, absent, makeup, records);
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getSummaryForStudent(Long applicationId, String username) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        if (!application.getUsername().equals(username)) {
            throw new IllegalStateException("본인 신청 내역만 확인할 수 있어요.");
        }
        return getSummary(applicationId);
    }

    // ---------- 오늘 출석 체크 (관리자 - 그날 수업 있는 학생 전체를 한 화면에서) ----------

    @Transactional(readOnly = true)
    public List<TodayAttendanceEntryResponse> getRosterForDate(LocalDate date) {
        String dayCode = DAY_CODE.get(date.getDayOfWeek());
        List<Application> scheduled = applicationRepository.findByStatusAndClassDaysIsNotNull("APPROVED").stream()
                .filter(a -> a.getClassDays() != null && List.of(a.getClassDays().split(",")).contains(dayCode))
                .toList();

        List<Long> applicationIds = scheduled.stream().map(Application::getId).toList();
        Map<Long, AttendanceRecord> recordByApplicationId = attendanceRecordRepository
                .findByApplicationIdInAndClassDate(applicationIds, date)
                .stream()
                .collect(Collectors.toMap(AttendanceRecord::getApplicationId, r -> r));

        return scheduled.stream()
                .map(app -> {
                    AttendanceRecord record = recordByApplicationId.get(app.getId());
                    return new TodayAttendanceEntryResponse(
                            app.getId(), app.getUsername(), app.getCourseName(), app.getClassTime(),
                            record != null ? record.getStatus() : null,
                            record != null ? record.getId() : null,
                            record != null && record.isCheckedInByStudent()
                    );
                })
                .sorted((a, b) -> {
                    String t1 = a.classTime() != null ? a.classTime() : "";
                    String t2 = b.classTime() != null ? b.classTime() : "";
                    return t1.compareTo(t2);
                })
                .toList();
    }

    // 관리자 - 특정 학생의 그날 출석 상태를 직접 지정/변경 (없으면 새로 만들고, 있으면 덮어씀)
    @Transactional
    public void setStatusForDate(Long applicationId, LocalDate date, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalStateException("올바른 출석 상태를 선택해주세요.");
        }
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));

        AttendanceRecord existing = attendanceRecordRepository.findByApplicationIdAndClassDate(applicationId, date).orElse(null);
        if (existing != null) {
            existing.updateStatus(status);
            attendanceRecordRepository.save(existing);
        } else {
            attendanceRecordRepository.save(AttendanceRecord.create(applicationId, date, status, null));
        }

        if ("ABSENT".equals(status)) {
            notificationService.notifyStudent(application.getUsername(), "ATTENDANCE_ABSENT",
                    application.getCourseName() + " " + date.format(DATE_FORMAT) + " 수업이 결석으로 기록됐어요.", null);
            checkConsecutiveAbsences(application);
        }
    }

    // 이 강의(신청)에서 최근 기록부터 몇 번 연속으로 결석인지 세어서, 기준(2회) 이상이면 관리자에게 알림
    private static final int CONSECUTIVE_ABSENCE_ALERT_THRESHOLD = 2;

    private void checkConsecutiveAbsences(Application application) {
        List<AttendanceRecord> records = attendanceRecordRepository
                .findByApplicationIdOrderByClassDateDesc(application.getId());

        int consecutive = 0;
        for (AttendanceRecord record : records) {
            if ("ABSENT".equals(record.getStatus())) {
                consecutive++;
            } else {
                break;
            }
        }

        if (consecutive >= CONSECUTIVE_ABSENCE_ALERT_THRESHOLD) {
            notificationService.notifyAdmin("CONSECUTIVE_ABSENCE",
                    application.getUsername() + "님이 " + application.getCourseName() + " 강의에서 "
                            + consecutive + "회 연속 결석했어요. 확인이 필요해요.", null);
        }
    }

    // 관리자 - "나머지 전부 결석 처리" 버튼. 그날 아직 기록이 없는 학생만 골라서 결석으로 채워줌
    @Transactional
    public int markRestAbsent(LocalDate date) {
        List<TodayAttendanceEntryResponse> roster = getRosterForDate(date);
        int count = 0;
        for (TodayAttendanceEntryResponse entry : roster) {
            if (entry.status() == null) {
                setStatusForDate(entry.applicationId(), date, "ABSENT");
                count++;
            }
        }
        return count;
    }

    // 관리자 - "출석 관리 내역" 화면. 전체 학생의 전체 출석 기록을 최신순으로 보여줌
    @Transactional(readOnly = true)
    public List<AttendanceHistoryEntryResponse> getAllHistoryForAdmin() {
        List<AttendanceRecord> records = attendanceRecordRepository.findAllByOrderByClassDateDesc();

        List<Long> applicationIds = records.stream().map(AttendanceRecord::getApplicationId).distinct().toList();
        Map<Long, Application> applicationById = applicationRepository.findAllById(applicationIds).stream()
                .collect(Collectors.toMap(Application::getId, a -> a));

        return records.stream()
                .map(r -> {
                    Application app = applicationById.get(r.getApplicationId());
                    return new AttendanceHistoryEntryResponse(
                            r.getId(),
                            app != null ? app.getUsername() : "-",
                            app != null ? app.getCourseName() : "-",
                            r.getClassDate().format(DATE_FORMAT),
                            r.getStatus(),
                            r.isCheckedInByStudent()
                    );
                })
                .toList();
    }

    // ---------- 학생 스스로 출석 체크 ----------

    @Transactional
    public String checkInSelf(Long applicationId, String username) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        if (!application.getUsername().equals(username)) {
            throw new IllegalStateException("본인 강의만 출석 체크할 수 있어요.");
        }
        if (application.getClassDays() == null || application.getClassTime() == null) {
            throw new IllegalStateException("아직 수업 시간이 등록되지 않았어요. 선생님께 문의해주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        String todayCode = DAY_CODE.get(now.getDayOfWeek());
        if (!List.of(application.getClassDays().split(",")).contains(todayCode)) {
            throw new IllegalStateException("오늘은 이 강의의 수업 요일이 아니에요.");
        }

        LocalTime classTime;
        try {
            classTime = LocalTime.parse(application.getClassTime());
        } catch (Exception e) {
            throw new IllegalStateException("수업 시간 정보가 올바르지 않아요. 선생님께 문의해주세요.");
        }
        LocalDateTime classStart = LocalDateTime.of(now.toLocalDate(), classTime);
        long minutesFromStart = java.time.Duration.between(classStart, now).toMinutes();

        if (minutesFromStart < -CHECK_IN_OPEN_MINUTES_BEFORE) {
            throw new IllegalStateException("아직 출석 체크 시간이 아니에요. 수업 시작 30분 전부터 가능해요.");
        }
        if (minutesFromStart > CHECK_IN_CLOSE_MINUTES_AFTER) {
            throw new IllegalStateException("출석 체크 시간이 지났어요. 선생님께 문의해주세요.");
        }

        LocalDate today = now.toLocalDate();
        if (attendanceRecordRepository.findByApplicationIdAndClassDate(applicationId, today).isPresent()) {
            throw new IllegalStateException("오늘 출석은 이미 처리됐어요.");
        }

        String status = minutesFromStart <= LATE_CUTOFF_MINUTES_AFTER ? "PRESENT" : "LATE";
        attendanceRecordRepository.save(AttendanceRecord.createByStudent(applicationId, today, status));
        return status;
    }

    // 학생 화면에 "지금 출석 체크 가능한 강의"가 있는지 알려줌 (버튼을 보여줄지 판단용)
    @Transactional(readOnly = true)
    public List<TodayAttendanceEntryResponse> getCheckInOptionsForStudent(String username) {
        return getCheckinStatusForStudent(username).checkableNow();
    }

    // 학생의 전체 시간표 (요일 상관없이 등록된 모든 강의) — 출석 체크 패널에 같이 보여줌
    @Transactional(readOnly = true)
    public List<StudentScheduleEntryResponse> getWeeklyScheduleForStudent(String username) {
        return applicationRepository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .filter(a -> "APPROVED".equals(a.getStatus()))
                .filter(a -> a.getClassDays() != null && !a.getClassDays().isBlank()
                        && a.getClassTime() != null && !a.getClassTime().isBlank())
                .map(a -> new StudentScheduleEntryResponse(a.getCourseName(), a.getClassDays(), a.getClassTime()))
                .toList();
    }

    // 오늘 수업이 아예 없는지 / 있지만 아직·이미 체크 시간이 아닌지 구분해서 알려줌
    @Transactional(readOnly = true)
    public CheckinStatusResponse getCheckinStatusForStudent(String username) {
        LocalDateTime now = LocalDateTime.now();
        String todayCode = DAY_CODE.get(now.getDayOfWeek());
        LocalDate today = now.toLocalDate();

        List<Application> scheduledToday = applicationRepository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .filter(a -> "APPROVED".equals(a.getStatus()))
                .filter(a -> a.getClassDays() != null && a.getClassTime() != null)
                .filter(a -> List.of(a.getClassDays().split(",")).contains(todayCode))
                .toList();

        boolean hasClassToday = !scheduledToday.isEmpty();

        List<TodayAttendanceEntryResponse> checkableNow = scheduledToday.stream()
                .filter(a -> {
                    try {
                        LocalTime classTime = LocalTime.parse(a.getClassTime());
                        LocalDateTime classStart = LocalDateTime.of(today, classTime);
                        long minutesFromStart = java.time.Duration.between(classStart, now).toMinutes();
                        return minutesFromStart >= -CHECK_IN_OPEN_MINUTES_BEFORE && minutesFromStart <= CHECK_IN_CLOSE_MINUTES_AFTER;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .filter(a -> attendanceRecordRepository.findByApplicationIdAndClassDate(a.getId(), today).isEmpty())
                .map(a -> new TodayAttendanceEntryResponse(a.getId(), null, a.getCourseName(), a.getClassTime(), null, null, false))
                .toList();

        return new CheckinStatusResponse(hasClassToday, checkableNow);
    }

    private AttendanceRecordResponse toResponse(AttendanceRecord record) {
        return new AttendanceRecordResponse(record.getId(), record.getClassDate().format(DATE_FORMAT),
                record.getStatus(), record.getNote());
    }
}