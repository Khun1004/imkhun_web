package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {

    List<StudyMaterial> findByLanguageAndCategoryAndScopeOrderByCreatedAtDesc(String language, String category, String scope);

    // 학생 홈 화면 "최근 등록된 자료" — 승인받은 언어들 중에서 최근 것부터 (KWZM 자료만)
    List<StudyMaterial> findByLanguageInAndScopeOrderByCreatedAtDesc(Collection<String> languages, String scope);
}