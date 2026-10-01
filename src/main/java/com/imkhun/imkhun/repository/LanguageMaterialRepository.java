package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.LanguageMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LanguageMaterialRepository extends JpaRepository<LanguageMaterial, Long> {

    List<LanguageMaterial> findAllByOrderByLanguageAscSortOrderAsc();
}