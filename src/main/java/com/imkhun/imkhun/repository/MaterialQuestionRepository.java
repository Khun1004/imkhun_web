package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.MaterialQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialQuestionRepository extends JpaRepository<MaterialQuestion, Long> {

    List<MaterialQuestion> findByMaterialIdOrderByCreatedAtAsc(Long materialId);

    long countByMaterialId(Long materialId);

    // 관리자 자료 목록 화면에서, 자료마다 "아직 답변 안 한 질문이 몇 개 있는지" 한 번에 보여주기 위한 집계
    @Query("SELECT q.materialId, COUNT(q) FROM MaterialQuestion q WHERE q.materialId IN :materialIds AND q.answerText IS NULL GROUP BY q.materialId")
    List<Object[]> countUnansweredGroupedByMaterialId(@Param("materialIds") List<Long> materialIds);
}