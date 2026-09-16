package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.ClassChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassChangeRequestRepository extends JpaRepository<ClassChangeRequest, Long> {

    List<ClassChangeRequest> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);

    List<ClassChangeRequest> findByApplicationIdInOrderByCreatedAtDesc(List<Long> applicationIds);

    List<ClassChangeRequest> findByStatusOrderByCreatedAtAsc(String status);
}