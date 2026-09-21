package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.LearningGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningGoalRepository extends JpaRepository<LearningGoal, Long> {

    List<LearningGoal> findByUsernameOrderByCreatedAtDesc(String username);
}