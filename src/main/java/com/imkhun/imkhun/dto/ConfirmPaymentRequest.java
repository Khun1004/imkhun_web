package com.imkhun.imkhun.dto;

// receiptImage는 선택이에요 (base64 데이터 URI) — 없어도 입금 확인은 가능해요
public record ConfirmPaymentRequest(String receiptImage) {
}