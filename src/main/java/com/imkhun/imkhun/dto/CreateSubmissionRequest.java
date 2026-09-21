package com.imkhun.imkhun.dto;

import java.util.List;

public record CreateSubmissionRequest(String textAnswer, List<AttachmentItem> attachments) {
}