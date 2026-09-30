package com.imkhun.imkhun.repository;

import com.imkhun.imkhun.domain.StudyGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyGroupMemberRepository extends JpaRepository<StudyGroupMember, Long> {

    List<StudyGroupMember> findByGroupId(Long groupId);

    List<StudyGroupMember> findByUsername(String username);

    Optional<StudyGroupMember> findByGroupIdAndUsername(Long groupId, String username);

    void deleteByGroupId(Long groupId);
}