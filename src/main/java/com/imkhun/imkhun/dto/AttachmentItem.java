package com.imkhun.imkhun.dto;

// data는 "data:image/png;base64,...." 형식의 데이터 URI
public record AttachmentItem(String data, String name) {
}