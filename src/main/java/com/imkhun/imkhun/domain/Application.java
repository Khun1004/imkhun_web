package com.imkhun.imkhun.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신청한 사용자 아이디 (User.username)
    @Column(nullable = false)
    private String username;

    // "TOGETHER"(나랑 같이 공부하기) 또는 "VIDEO"(영상으로만 듣기)
    @Column(nullable = false)
    private String studyType;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false)
    private String contact;

    @Column
    private String memo;

    // "PENDING"(승인대기) 또는 "APPROVED"(승인완료) — 지금은 항상 PENDING으로 시작
    @Column(nullable = false)
    private String status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 승인될 때 자동으로 생성됨 (예: 2026_Japanese_Level1_01) — 승인 전엔 null
    @Column
    private String studentNumber;

    // ---- 결제 안내 (관리자가 승인된 학생에게 등록해줌) — 하나라도 등록되면 학생 마이페이지에 "결제 확인" 버튼이 떠요 ----
    @Column(name = "payment_method", columnDefinition = "CLOB")
    private String paymentMethod;

    @Column
    private String amount;

    @Column(name = "amount_reason", columnDefinition = "CLOB")
    private String amountReason;

    @Column(name = "material_guide", columnDefinition = "CLOB")
    private String materialGuide;

    @Column(name = "class_guide", columnDefinition = "CLOB")
    private String classGuide;

    // 학생이 "입금했어요" 눌렀을 때 (null이면 아직 안 눌렀다는 뜻)
    @Column(name = "payment_confirmed_by_student_at")
    private LocalDateTime paymentConfirmedByStudentAt;

    // 관리자가 입금을 확인해줬을 때
    @Column(name = "payment_confirmed_by_admin_at")
    private LocalDateTime paymentConfirmedByAdminAt;

    // 학생이 "입금했어요" 누를 때 같이 첨부한 영수증 이미지 (base64 데이터 URI) — 없으면 null
    @Column(name = "receipt_image", columnDefinition = "CLOB")
    private String receiptImage;

    // 출석 체크에 쓰는 수업 요일·시간. classDays는 "TUE,WED"처럼 쉼표로 구분된 요일 코드,
    // classTime은 "10:00"처럼 자유 형식. 둘 다 관리자가 나중에 채워줌 (null이면 아직 미설정)
    @Column(name = "class_days")
    private String classDays;

    @Column(name = "class_time")
    private String classTime;

    protected Application() {
        // JPA 기본 생성자
    }

    public static Application create(String username, String studyType, String courseName,
                                     String contact, String memo) {
        Application application = new Application();
        application.username = username;
        application.studyType = studyType;
        application.courseName = courseName;
        application.contact = contact;
        application.memo = memo;
        application.status = "PENDING";
        return application;
    }

    public void changeStatus(String status) {
        this.status = status;
    }

    public void assignStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    // 관리자가 강의(과목/학습방식)를 바꿔줄 때 씀 — 학생은 직접 바꿀 수 없음
    public void changeCourse(String studyType, String courseName) {
        this.studyType = studyType;
        this.courseName = courseName;
    }

    public void updatePaymentInfo(String paymentMethod, String amount, String amountReason,
                                  String materialGuide, String classGuide) {
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.amountReason = amountReason;
        this.materialGuide = materialGuide;
        this.classGuide = classGuide;
    }

    // 5개 항목 중 하나라도 채워져 있으면 "결제 안내가 등록됐다"고 봐요
    public boolean hasPaymentInfo() {
        return paymentMethod != null || amount != null || amountReason != null
                || materialGuide != null || classGuide != null;
    }

    public void confirmPaymentByStudent(String receiptImage) {
        this.paymentConfirmedByStudentAt = LocalDateTime.now();
        this.receiptImage = receiptImage;
    }

    public void confirmPaymentByAdmin() {
        this.paymentConfirmedByAdminAt = LocalDateTime.now();
    }

    public void updateSchedule(String classDays, String classTime) {
        this.classDays = classDays;
        this.classTime = classTime;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getStudyType() {
        return studyType;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getContact() {
        return contact;
    }

    public String getMemo() {
        return memo;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getAmount() {
        return amount;
    }

    public String getAmountReason() {
        return amountReason;
    }

    public String getMaterialGuide() {
        return materialGuide;
    }

    public String getClassGuide() {
        return classGuide;
    }

    public LocalDateTime getPaymentConfirmedByStudentAt() {
        return paymentConfirmedByStudentAt;
    }

    public LocalDateTime getPaymentConfirmedByAdminAt() {
        return paymentConfirmedByAdminAt;
    }

    public String getReceiptImage() {
        return receiptImage;
    }

    public String getClassDays() {
        return classDays;
    }

    public String getClassTime() {
        return classTime;
    }
}