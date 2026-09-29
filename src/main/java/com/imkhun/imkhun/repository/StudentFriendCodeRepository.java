package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudentFriendCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentFriendCodeRepository extends JpaRepository<StudentFriendCode, Long> {

    Optional<StudentFriendCode> findByUsername(String username);

    Optional<StudentFriendCode> findByCode(String code);

    boolean existsByCode(String code);
}