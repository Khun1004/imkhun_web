package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUsernameAOrUsernameB(String usernameA, String usernameB);

    Optional<Friendship> findByUsernameAAndUsernameB(String usernameA, String usernameB);
}