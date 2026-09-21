package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.VocabularyQuizResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VocabularyQuizResultRepository extends JpaRepository<VocabularyQuizResult, Long> {

    Optional<VocabularyQuizResult> findTopBySetIdAndUsernameOrderByCompletedAtDesc(Long setId, String username);

    List<VocabularyQuizResult> findByUsername(String username);

    void deleteBySetId(Long setId);
}