package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.StudyNote;
import com.imkhun.imkhun.dto.NoteResponse;
import com.imkhun.imkhun.repository.StudyNoteRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Set;

@Service
public class StudyNoteService {

    private final StudyNoteRepository studyNoteRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final Set<String> VALID_LANGUAGES = Set.of("korean", "japanese", "thai", "english");

    public StudyNoteService(StudyNoteRepository studyNoteRepository) {
        this.studyNoteRepository = studyNoteRepository;
    }

    public NoteResponse getNote(String language) {
        validateLanguage(language);
        return studyNoteRepository.findByLanguage(language)
                .map(note -> new NoteResponse(note.getLanguage(), note.getContent(), note.getUpdatedAt().format(DATE_FORMAT)))
                .orElse(new NoteResponse(language, "", null));
    }

    public NoteResponse saveNote(String language, String content) {
        validateLanguage(language);
        StudyNote note = studyNoteRepository.findByLanguage(language)
                .orElseGet(() -> StudyNote.create(language, content));
        note.updateContent(content);
        StudyNote saved = studyNoteRepository.save(note);
        return new NoteResponse(saved.getLanguage(), saved.getContent(), saved.getUpdatedAt().format(DATE_FORMAT));
    }

    private void validateLanguage(String language) {
        if (!VALID_LANGUAGES.contains(language)) {
            throw new IllegalStateException("올바르지 않은 언어예요.");
        }
    }
}