package com.imkhun.imkhun.dto;

import java.util.List;

public record DashboardResponse(long pendingApplicationsCount, long paymentPendingConfirmCount,
                                long newApplicationsThisMonth, long unreadNotificationsCount,
                                List<PostResponse> recentPosts) {
}