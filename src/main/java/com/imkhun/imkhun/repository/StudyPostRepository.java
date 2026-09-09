package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudyPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyPostRepository extends JpaRepository<StudyPost, Long> {

    List<StudyPost> findByTopicAndCategoryOrderByCreatedAtDesc(String topic, String category);

    // "컴퓨터" 주제는 항목(category) 구분이 없어서, 주제만으로 전체 조회
    List<StudyPost> findByTopicOrderByCreatedAtDesc(String topic);

    // 마이페이지 "내가 쓴 글"
    List<StudyPost> findByUsernameOrderByCreatedAtDesc(String username);

    // 검색 — 제목/내용에 검색어가 들어간 글
    // content는 CLOB이라 LOWER()에 바로 못 써서 CAST로 문자열로 바꿔줌
    @Query("SELECT p FROM StudyPost p WHERE "
            + "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(CAST(p.content AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "ORDER BY p.createdAt DESC")
    List<StudyPost> search(@Param("keyword") String keyword);
}