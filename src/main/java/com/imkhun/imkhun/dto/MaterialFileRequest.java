package com.imkhun.imkhun.dto;

public record MaterialFileRequest(String fileName, String fileType, String fileData,
                                  String linkUrl, String textContent) {
}