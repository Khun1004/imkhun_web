package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.LessonNote;
import com.imkhun.imkhun.dto.CreateLessonNoteRequest;
import com.imkhun.imkhun.dto.LessonNoteResponse;
import com.imkhun.imkhun.repository.LessonNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LessonNoteService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LessonNoteRepository lessonNoteRepository;

    public LessonNoteService(LessonNoteRepository lessonNoteRepository) {
        this.lessonNoteRepository = lessonNoteRepository;
    }

    @Transactional
    public LessonNoteResponse addNote(Long applicationId, CreateLessonNoteRequest request) {
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalStateException("수업 내용을 입력해주세요.");
        }

        LocalDate classDate = null;
        if (request.classDate() != null && !request.classDate().isBlank()) {
            try {
                classDate = LocalDate.parse(request.classDate());
            } catch (Exception e) {
                throw new IllegalStateException("날짜 형식이 올바르지 않아요.");
            }
        }

        LessonNote saved = lessonNoteRepository.save(LessonNote.create(applicationId, classDate, request.content()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LessonNoteResponse> getNotesForApplication(Long applicationId) {
        return lessonNoteRepository.findByApplicationIdOrderByClassDateDescCreatedAtDesc(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteNote(Long id) {
        lessonNoteRepository.deleteById(id);
    }

    private LessonNoteResponse toResponse(LessonNote note) {
        return new LessonNoteResponse(
                note.getId(),
                note.getClassDate() != null ? note.getClassDate().toString() : null,
                note.getContent(),
                note.getCreatedAt().format(DATETIME_FORMAT)
        );
    }
}