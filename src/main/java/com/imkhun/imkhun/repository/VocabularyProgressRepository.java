package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.VocabularyProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VocabularyProgressRepository extends JpaRepository<VocabularyProgress, Long> {

    Optional<VocabularyProgress> findByUsernameAndWordId(String username, Long wordId);

    List<VocabularyProgress> findByUsername(String username);
}