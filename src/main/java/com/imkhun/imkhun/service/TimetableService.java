package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.TimetableEntry;
import com.imkhun.imkhun.dto.CreateTimetableEntryRequest;
import com.imkhun.imkhun.dto.TimetableEntryResponse;
import com.imkhun.imkhun.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class TimetableService {

    private final TimetableEntryRepository timetableEntryRepository;
    private static final Set<String> VALID_DAYS = Set.of("MON", "TUE", "WED", "THU", "FRI");
    private static final Set<String> VALID_COLOR_TYPES = Set.of("korean", "computer", "other");

    public TimetableService(TimetableEntryRepository timetableEntryRepository) {
        this.timetableEntryRepository = timetableEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> getAllEntries() {
        return timetableEntryRepository.findAllByOrderByStartTimeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TimetableEntryResponse createEntry(CreateTimetableEntryRequest request) {
        validate(request);
        TimetableEntry saved = timetableEntryRepository.save(TimetableEntry.create(
                request.day(), request.startTime(), request.endTime(), request.courseName(), request.colorType()));
        return toResponse(saved);
    }

    @Transactional
    public TimetableEntryResponse updateEntry(Long id, CreateTimetableEntryRequest request) {
        validate(request);
        TimetableEntry entry = timetableEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("시간표 항목을 찾을 수 없어요."));
        entry.update(request.day(), request.startTime(), request.endTime(), request.courseName(), request.colorType());
        return toResponse(timetableEntryRepository.save(entry));
    }

    @Transactional
    public void deleteEntry(Long id) {
        if (!timetableEntryRepository.existsById(id)) {
            throw new IllegalStateException("시간표 항목을 찾을 수 없어요.");
        }
        timetableEntryRepository.deleteById(id);
    }

    private void validate(CreateTimetableEntryRequest request) {
        if (request.day() == null || !VALID_DAYS.contains(request.day())) {
            throw new IllegalStateException("올바른 요일을 선택해주세요.");
        }
        if (request.startTime() == null || request.startTime().isBlank()
                || request.endTime() == null || request.endTime().isBlank()) {
            throw new IllegalStateException("시작·종료 시간을 입력해주세요.");
        }
        if (request.courseName() == null || request.courseName().isBlank()) {
            throw new IllegalStateException("과목명을 입력해주세요.");
        }
        if (request.colorType() == null || !VALID_COLOR_TYPES.contains(request.colorType())) {
            throw new IllegalStateException("올바른 색상 구분을 선택해주세요.");
        }
    }

    private TimetableEntryResponse toResponse(TimetableEntry entry) {
        return new TimetableEntryResponse(entry.getId(), entry.getDay(), entry.getStartTime(),
                entry.getEndTime(), entry.getCourseName(), entry.getColorType());
    }
}