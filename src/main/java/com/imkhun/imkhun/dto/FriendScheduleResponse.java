package com.imkhun.imkhun.dto;

import java.util.List;

public record FriendScheduleResponse(String username, String nickname, List<StudentScheduleEntryResponse> schedule) {
}