package com.imkhun.imkhun.dto;

import java.util.List;

public record TodayTodoResponse(List<TodayAttendanceEntryResponse> todayClasses,
                                List<PendingPaymentTodoResponse> pendingPayments,
                                List<ClassChangeRequestResponse> pendingClassChanges,
                                long unreadNotifications) {
}