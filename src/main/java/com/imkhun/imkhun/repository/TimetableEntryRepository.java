package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    List<TimetableEntry> findAllByOrderByStartTimeAsc();
}