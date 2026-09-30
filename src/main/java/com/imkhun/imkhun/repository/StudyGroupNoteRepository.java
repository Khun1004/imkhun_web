package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudyGroupNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyGroupNoteRepository extends JpaRepository<StudyGroupNote, Long> {

    List<StudyGroupNote> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    void deleteByGroupId(Long groupId);
}