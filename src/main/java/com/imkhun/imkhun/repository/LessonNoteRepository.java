package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.LessonNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonNoteRepository extends JpaRepository<LessonNote, Long> {

    List<LessonNote> findByApplicationIdOrderByClassDateDescCreatedAtDesc(Long applicationId);
}