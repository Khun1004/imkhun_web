package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.ClassNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassNoteRepository extends JpaRepository<ClassNote, Long> {

    List<ClassNote> findByUsernameOrderByClassDateDescCreatedAtDesc(String username);

    Optional<ClassNote> findByAttendanceRecordIdAndUsername(Long attendanceRecordId, String username);

    List<ClassNote> findByUsernameAndAttendanceRecordIdIn(String username, List<Long> attendanceRecordIds);
}