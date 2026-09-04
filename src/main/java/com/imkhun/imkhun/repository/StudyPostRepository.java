package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudyPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyPostRepository extends JpaRepository<StudyPost, Long> {

    List<StudyPost> findByTopicAndCategoryOrderByCreatedAtDesc(String topic, String category);

    // "컴퓨터" 주제는 항목(category) 구분이 없어서, 주제만으로 전체 조회
    List<StudyPost> findByTopicOrderByCreatedAtDesc(String topic);

    // 마이페이지 "내가 쓴 글"
    List<StudyPost> findByUsernameOrderByCreatedAtDesc(String username);
}