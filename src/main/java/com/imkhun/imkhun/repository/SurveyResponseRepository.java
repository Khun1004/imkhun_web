package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {

    Optional<SurveyResponse> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);

    List<SurveyResponse> findAllByOrderByCreatedAtDesc();
}