package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudentLevelRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentLevelRecordRepository extends JpaRepository<StudentLevelRecord, Long> {

    List<StudentLevelRecord> findByApplicationIdOrderByRecordedDateDescCreatedAtDesc(Long applicationId);
}