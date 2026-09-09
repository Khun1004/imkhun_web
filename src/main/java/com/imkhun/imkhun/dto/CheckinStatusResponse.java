package com.imkhun.imkhun.dto;

import java.util.List;

public record CheckinStatusResponse(boolean hasClassToday, List<TodayAttendanceEntryResponse> checkableNow) {
}