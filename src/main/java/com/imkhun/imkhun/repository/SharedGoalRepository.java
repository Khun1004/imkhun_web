package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.SharedGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharedGoalRepository extends JpaRepository<SharedGoal, Long> {

    List<SharedGoal> findByCreatorUsernameOrPartnerUsernameOrderByCreatedAtDesc(String creatorUsername, String partnerUsername);
}