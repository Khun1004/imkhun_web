package com.imkhun.imkhun.dto;

import java.util.List;

// myStatus: 로그인한 내가 이 그룹에서 어떤 상태인지 (INVITED면 화면에서 수락/거절 버튼을 보여줌)
public record StudyGroupResponse(Long id, String name, String creatorUsername, String creatorNickname,
                                 List<StudyGroupMemberResponse> members, String myStatus,
                                 boolean isCreator, String createdAt) {
}