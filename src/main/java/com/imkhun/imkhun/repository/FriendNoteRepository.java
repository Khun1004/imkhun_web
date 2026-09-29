package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.FriendNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendNoteRepository extends JpaRepository<FriendNote, Long> {

    List<FriendNote> findByReceiverUsernameOrderByCreatedAtDesc(String receiverUsername);
}