package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentQuestionRepository extends JpaRepository<StudentQuestion, Long> {

    List<StudentQuestion> findByUsernameOrderByCreatedAtDesc(String username);

    List<StudentQuestion> findAllByOrderByCreatedAtDesc();
}