package com.imkhun.imkhun.dto;

import java.util.List;

public record FriendBadgesResponse(String username, String nickname, List<BadgeResponse> badges) {
}