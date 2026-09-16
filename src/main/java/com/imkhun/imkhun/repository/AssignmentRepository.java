package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByApplicationIdOrderByDueDateAscCreatedAtDesc(Long applicationId);

    // 학생 마이페이지 "내 숙제" — 여러 강의(Application)에 걸친 숙제를 한번에 모아서 보여줄 때 씀
    List<Assignment> findByApplicationIdInOrderByCompletedAscDueDateAsc(Collection<Long> applicationIds);
}