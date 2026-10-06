package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생이 "온라인 영상" 자료를 열어봤는지 기록함 — 재생을 끝까지 봤는지까지는 알 수 없고,
// 자료를 "열었다"는 것만 확인할 수 있어요 (학생 한 명당 자료 하나에 기록 한 개, 다시 열면 시간만 갱신)
@Entity
@Table(name = "material_views")
public class MaterialView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(nullable = false)
    private String username;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();

    protected MaterialView() {
        // JPA 기본 생성자
    }

    public static MaterialView create(Long materialId, String username) {
        MaterialView view = new MaterialView();
        view.materialId = materialId;
        view.username = username;
        return view;
    }

    public void touch() {
        this.viewedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }
}