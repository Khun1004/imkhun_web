package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findByApplicationIdOrderByClassDateDesc(Long applicationId);

    long countByApplicationIdAndStatus(Long applicationId, String status);

    Optional<AttendanceRecord> findByApplicationIdAndClassDate(Long applicationId, LocalDate classDate);

    List<AttendanceRecord> findByApplicationIdInAndClassDate(Collection<Long> applicationIds, LocalDate classDate);
}