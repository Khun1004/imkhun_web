package com.imkhun.imkhun.dto;

// 같은 자료를 보는 학생들끼리는 서로 닉네임이 보임(완전 익명은 아님) — 내가 쓴 질문인지(mine)도 같이 내려줘서
// 화면에서 내 질문을 다르게 표시할 수 있게 함
public record MaterialQuestionResponse(Long id, Long materialId, String nickname, String questionText,
                                       String answerText, String createdAt, String answeredAt, boolean mine) {
}