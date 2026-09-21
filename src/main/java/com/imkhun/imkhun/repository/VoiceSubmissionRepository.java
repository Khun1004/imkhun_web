package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.VoiceSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoiceSubmissionRepository extends JpaRepository<VoiceSubmission, Long> {

    List<VoiceSubmission> findByUsernameOrderByCreatedAtDesc(String username);

    List<VoiceSubmission> findAllByOrderByCreatedAtDesc();
}