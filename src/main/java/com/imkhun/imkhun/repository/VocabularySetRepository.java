package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.VocabularySet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VocabularySetRepository extends JpaRepository<VocabularySet, Long> {

    List<VocabularySet> findByLanguageOrderByCreatedAtAsc(String language);
}