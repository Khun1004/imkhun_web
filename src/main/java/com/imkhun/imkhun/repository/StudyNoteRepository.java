package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudyNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyNoteRepository extends JpaRepository<StudyNote, Long> {

    Optional<StudyNote> findByLanguage(String language);
}