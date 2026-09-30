package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudyGroupGoalAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyGroupGoalAchievementRepository extends JpaRepository<StudyGroupGoalAchievement, Long> {

    List<StudyGroupGoalAchievement> findByGoalId(Long goalId);

    Optional<StudyGroupGoalAchievement> findByGoalIdAndUsername(Long goalId, String username);

    void deleteByGoalId(Long goalId);
}