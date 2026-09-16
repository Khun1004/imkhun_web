package com.imkhun.imkhun.dto;

// month는 "2026-09" 형식
public record MonthlyRevenueResponse(String month, long totalRevenue, long paymentCount) {
}