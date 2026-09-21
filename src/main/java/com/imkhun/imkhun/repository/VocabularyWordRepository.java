package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.VocabularyWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VocabularyWordRepository extends JpaRepository<VocabularyWord, Long> {

    List<VocabularyWord> findBySetIdOrderByCreatedAtAsc(Long setId);

    long countBySetId(Long setId);

    void deleteBySetId(Long setId);
}