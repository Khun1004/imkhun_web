package com.imkhun.imkhun.dto;

public record UpdateApplicationPaymentRequest(String paymentMethod, String amount, String amountReason,
                                              String materialGuide, String classGuide) {
}