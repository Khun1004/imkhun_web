package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// "미래 계획" 페이지에 나오는 계획 카드 하나 (예: "Plan 01 · 함께할 선생님을 찾습니다")
// 관리자가 자유롭게 등록/수정/삭제/순서 변경 가능
@Entity
@Table(name = "future_plan_items")
public class FuturePlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private String title;

    // 문단은 빈 줄(\n\n)로 구분해서 하나의 텍스트로 저장함
    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    // 화면에 알약 모양으로 나오는 태그들 — 쉼표로 구분해서 하나의 문자열로 저장 (예: "언어 선생님,컴퓨터·디자인 선생님")
    @Column(nullable = false)
    private String tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected FuturePlanItem() {
        // JPA 기본 생성자
    }

    public static FuturePlanItem create(int sortOrder, String title, String content, String tags) {
        FuturePlanItem item = new FuturePlanItem();
        item.sortOrder = sortOrder;
        item.title = title;
        item.content = content;
        item.tags = tags;
        return item;
    }

    public void update(int sortOrder, String title, String content, String tags) {
        this.sortOrder = sortOrder;
        this.title = title;
        this.content = content;
        this.tags = tags;
    }

    public Long getId() {
        return id;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getTags() {
        return tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}