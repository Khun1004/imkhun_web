package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.StudentLevelRecord;
import com.imkhun.imkhun.dto.CreateLevelRecordRequest;
import com.imkhun.imkhun.dto.LevelRecordResponse;
import com.imkhun.imkhun.repository.StudentLevelRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StudentLevelService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StudentLevelRecordRepository studentLevelRecordRepository;

    public StudentLevelService(StudentLevelRecordRepository studentLevelRecordRepository) {
        this.studentLevelRecordRepository = studentLevelRecordRepository;
    }

    @Transactional
    public LevelRecordResponse addRecord(Long applicationId, CreateLevelRecordRequest request) {
        if (request.level() == null || request.level().isBlank()) {
            throw new IllegalStateException("레벨을 입력해주세요.");
        }

        LocalDate recordedDate = null;
        if (request.recordedDate() != null && !request.recordedDate().isBlank()) {
            try {
                recordedDate = LocalDate.parse(request.recordedDate());
            } catch (Exception e) {
                throw new IllegalStateException("날짜 형식이 올바르지 않아요.");
            }
        }

        StudentLevelRecord saved = studentLevelRecordRepository.save(
                StudentLevelRecord.create(applicationId, request.level(), request.note(), recordedDate));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LevelRecordResponse> getRecordsForApplication(Long applicationId) {
        return studentLevelRecordRepository.findByApplicationIdOrderByRecordedDateDescCreatedAtDesc(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteRecord(Long id) {
        studentLevelRecordRepository.deleteById(id);
    }

    private LevelRecordResponse toResponse(StudentLevelRecord record) {
        return new LevelRecordResponse(
                record.getId(),
                record.getLevel(),
                record.getNote(),
                record.getRecordedDate() != null ? record.getRecordedDate().toString() : null,
                record.getCreatedAt().format(DATETIME_FORMAT)
        );
    }
}