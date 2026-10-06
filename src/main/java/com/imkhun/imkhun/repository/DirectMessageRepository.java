package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    List<DirectMessage> findByUsernameOrderByCreatedAtAsc(String username);

    // 학생별 대화방 목록(관리자 "학생 메시지" 화면)을 만들 때, 메시지가 있던 학생 username들을 중복 없이 뽑아냄
    @org.springframework.data.jpa.repository.Query("select distinct m.username from DirectMessage m")
    List<String> findDistinctUsernames();

    long countByUsernameAndSenderTypeAndReadByAdminFalse(String username, String senderType);

    long countByUsernameAndSenderTypeAndReadByStudentFalse(String username, String senderType);

    long countBySenderTypeAndReadByAdminFalse(String senderType);
}