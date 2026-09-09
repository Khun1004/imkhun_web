package com.imkhun.imkhun.dto;

public record AdminApplicationResponse(Long id, String nickname, String email, String studyType, String courseName,
                                       String contact, String memo, String status, String createdAt,
                                       String studentNumber, boolean hasPaymentInfo, String paymentMethod,
                                       String amount, String amountReason, String materialGuide, String classGuide,
                                       boolean paymentConfirmedByStudent, boolean paymentConfirmedByAdmin,
                                       String paymentConfirmedByStudentAt, String receiptImage,
                                       String classDays, String classTime) {
}