package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.AttendanceRecord;
import com.imkhun.imkhun.domain.ClassNote;
import com.imkhun.imkhun.dto.ClassNoteResponse;
import com.imkhun.imkhun.dto.CreateClassNoteRequest;
import com.imkhun.imkhun.dto.RecentClassForNoteResponse;
import com.imkhun.imkhun.repository.AttendanceRecordRepository;
import com.imkhun.imkhun.repository.ClassNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 학생이 수업 하나하나에 대해 남기는 "수업 후 미니 노트"를 관리하는 서비스
@Service
public class ClassNoteService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ATTENDED_STATUSES = Set.of("PRESENT", "LATE", "MAKEUP");
    private static final int MAX_RECENT_CLASSES = 20;

    private final ClassNoteRepository classNoteRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentAuthService studentAuthService;

    public ClassNoteService(ClassNoteRepository classNoteRepository, AttendanceRecordRepository attendanceRecordRepository,
                            StudentAuthService studentAuthService) {
        this.classNoteRepository = classNoteRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.studentAuthService = studentAuthService;
    }

    @Transactional
    public List<ClassNoteResponse> getNotesForStudent(String username) {
        return classNoteRepository.findByUsernameOrderByClassDateDescCreatedAtDesc(username).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<RecentClassForNoteResponse> getRecentClassesForNote(String username) {
        List<Application> applications = studentAuthService.getApprovedApplications(username);
        Map<Long, String> courseNameByApplicationId = applications.stream()
                .collect(java.util.stream.Collectors.toMap(Application::getId, Application::getCourseName, (a, b) -> a));

        List<AttendanceRecord> records = applications.stream()
                .flatMap(app -> attendanceRecordRepository.findByApplicationIdOrderByClassDateDesc(app.getId()).stream())
                .filter(r -> ATTENDED_STATUSES.contains(r.getStatus()))
                .sorted(Comparator.comparing(AttendanceRecord::getClassDate).reversed())
                .limit(MAX_RECENT_CLASSES)
                .toList();

        List<Long> recordIds = records.stream().map(AttendanceRecord::getId).toList();
        Map<Long, ClassNote> existingNoteByRecordId = classNoteRepository.findByUsernameAndAttendanceRecordIdIn(username, recordIds).stream()
                .collect(java.util.stream.Collectors.toMap(ClassNote::getAttendanceRecordId, n -> n));

        return records.stream()
                .map(r -> {
                    ClassNote existing = existingNoteByRecordId.get(r.getId());
                    return new RecentClassForNoteResponse(
                            r.getId(),
                            courseNameByApplicationId.getOrDefault(r.getApplicationId(), "수업"),
                            r.getClassDate().toString(),
                            existing != null,
                            existing != null ? existing.getId() : null,
                            existing != null ? existing.getContent() : null
                    );
                })
                .toList();
    }

    @Transactional
    public ClassNoteResponse saveNote(String username, CreateClassNoteRequest request) {
        if (request.attendanceRecordId() == null) {
            throw new IllegalStateException("어떤 수업에 대한 노트인지 선택해주세요.");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalStateException("노트 내용을 입력해주세요.");
        }
        if (request.content().length() > 1000) {
            throw new IllegalStateException("노트는 1000자 이내로 적어주세요.");
        }

        AttendanceRecord record = attendanceRecordRepository.findById(request.attendanceRecordId())
                .orElseThrow(() -> new IllegalStateException("수업 기록을 찾을 수 없어요."));

        List<Application> applications = studentAuthService.getApprovedApplications(username);
        Application matchedApplication = applications.stream()
                .filter(app -> app.getId().equals(record.getApplicationId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("본인의 수업 기록에만 노트를 남길 수 있어요."));

        ClassNote note = classNoteRepository.findByAttendanceRecordIdAndUsername(record.getId(), username)
                .map(existing -> {
                    existing.updateContent(request.content().trim());
                    return existing;
                })
                .orElseGet(() -> ClassNote.create(username, record.getId(), matchedApplication.getCourseName(),
                        record.getClassDate(), request.content().trim()));

        return toResponse(classNoteRepository.save(note));
    }

    @Transactional
    public void deleteNote(Long id, String username) {
        ClassNote note = classNoteRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("노트를 찾을 수 없어요."));
        if (!note.getUsername().equals(username)) {
            throw new IllegalStateException("본인의 노트만 삭제할 수 있어요.");
        }
        classNoteRepository.deleteById(id);
    }

    private ClassNoteResponse toResponse(ClassNote note) {
        return new ClassNoteResponse(
                note.getId(), note.getAttendanceRecordId(), note.getCourseName(), note.getClassDate().toString(),
                note.getContent(), note.getCreatedAt().format(DATETIME_FORMAT), note.getUpdatedAt().format(DATETIME_FORMAT)
        );
    }
}