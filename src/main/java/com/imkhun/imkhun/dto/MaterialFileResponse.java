package com.imkhun.imkhun.dto;

public record MaterialFileResponse(String fileName, String fileType, String fileData,
                                   String linkUrl, String textContent) {
}