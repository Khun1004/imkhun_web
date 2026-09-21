package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    Optional<AssignmentSubmission> findByAssignmentId(Long assignmentId);

    List<AssignmentSubmission> findAllByOrderBySubmittedAtDesc();

    void deleteByAssignmentId(Long assignmentId);
}