package com.imkhun.imkhun.dto;

import java.util.List;

// 그룹 이름 + 처음에 같이 초대할 친구 username 목록 (본인은 자동으로 그룹장이 됨)
public record CreateStudyGroupRequest(String name, List<String> inviteUsernames) {
}