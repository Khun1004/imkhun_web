package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {

    List<StudyMaterial> findByLanguageAndCategoryAndScopeOrderByCreatedAtDesc(String language, String category, String scope);

    List<StudyMaterial> findByLanguageAndScopeOrderByCreatedAtDesc(String language, String scope);

    // 한국어 KWZM 자료 — 관리자가 등급 하나를 골라서 볼 때 (정확히 그 등급만)
    List<StudyMaterial> findByLanguageAndCategoryAndScopeAndLevelOrderByCreatedAtDesc(String language, String category, String scope, String level);

    // 한국어 KWZM 자료 — 학생이 속한 등급(들) 안에 있는 것만 (항목 구분 없이, "내 수강 정보"용)
    List<StudyMaterial> findByLanguageAndScopeAndLevelInOrderByCreatedAtDesc(String language, String scope, Collection<String> levels);

    // 한국어 KWZM 자료 — 학생이 속한 등급(들) 안에 있는 것만 (항목별로)
    List<StudyMaterial> findByLanguageAndCategoryAndScopeAndLevelInOrderByCreatedAtDesc(String language, String category, String scope, Collection<String> levels);

    // 학생 홈 화면 "최근 등록된 자료" — 승인받은 언어들 중에서 최근 것부터 (KWZM 자료만)
    List<StudyMaterial> findByLanguageInAndScopeOrderByCreatedAtDesc(Collection<String> languages, String scope);

    // 검색 — 제목/설명에 검색어가 들어간 자료 (같은 scope 안에서만)
    // description은 CLOB이라 LOWER()에 바로 못 써서 CAST로 문자열로 바꿔줌
    @Query("SELECT m FROM StudyMaterial m WHERE m.scope = :scope AND "
            + "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(CAST(m.description AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "ORDER BY m.createdAt DESC")
    List<StudyMaterial> searchByScope(@Param("scope") String scope, @Param("keyword") String keyword);
}