package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudyGroupGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyGroupGoalRepository extends JpaRepository<StudyGroupGoal, Long> {

    List<StudyGroupGoal> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    void deleteByGroupId(Long groupId);
}