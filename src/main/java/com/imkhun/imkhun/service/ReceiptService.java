package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import com.imkhun.imkhun.domain.User;
import com.imkhun.imkhun.repository.ApplicationRepository;
import com.imkhun.imkhun.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public ReceiptService(ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    // 학생용 — 본인 신청 내역이 맞는지 확인하고 나서 영수증을 만들어줌
    public String buildReceiptHtmlForStudent(Long applicationId, String username) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));
        if (!application.getUsername().equals(username)) {
            throw new IllegalStateException("본인의 신청 내역만 확인할 수 있어요.");
        }
        return buildReceiptHtml(applicationId);
    }

    // 결제 확인이 끝난 신청만 영수증을 볼 수 있음. 본인 확인은 컨트롤러에서 처리함.
    public String buildReceiptHtml(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException("신청 내역을 찾을 수 없어요."));

        if (application.getPaymentConfirmedByAdminAt() == null) {
            throw new IllegalStateException("아직 결제 확인이 완료되지 않았어요.");
        }

        String nickname = userRepository.findByUsername(application.getUsername())
                .map(User::getNickname)
                .orElse(application.getUsername());

        String receiptNumber = "IK-" + application.getId() + "-"
                + application.getPaymentConfirmedByAdminAt().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String issuedDate = application.getPaymentConfirmedByAdminAt().format(DATE_FORMAT);
        String amount = application.getAmount() != null && !application.getAmount().isBlank()
                ? application.getAmount() : "-";
        String paymentMethod = application.getPaymentMethod() != null && !application.getPaymentMethod().isBlank()
                ? application.getPaymentMethod() : "-";
        String amountReason = application.getAmountReason() != null && !application.getAmountReason().isBlank()
                ? application.getAmountReason() : "";

        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                <meta charset="UTF-8">
                <title>결제 영수증 - %s</title>
                <style>
                    * { box-sizing: border-box; }
                    body {
                        font-family: "Noto Sans KR", "Malgun Gothic", sans-serif;
                        background: #f0ede6;
                        margin: 0;
                        padding: 40px 16px;
                        color: #221d2b;
                    }
                    .receipt {
                        max-width: 640px;
                        margin: 0 auto;
                        background: #fff;
                        border-radius: 16px;
                        box-shadow: 0 20px 50px -20px rgba(23,20,15,0.25);
                        overflow: hidden;
                    }
                    .receipt-head {
                        background: linear-gradient(135deg, #16386b, #2f9aa0);
                        color: #fff;
                        padding: 36px 40px;
                    }
                    .receipt-head .brand {
                        font-family: "Noto Serif KR", serif;
                        font-size: 22px;
                        font-weight: 800;
                        margin: 0 0 6px;
                    }
                    .receipt-head .sub {
                        font-size: 13px;
                        opacity: 0.85;
                        margin: 0;
                    }
                    .receipt-title {
                        text-align: center;
                        font-family: "Noto Serif KR", serif;
                        font-size: 26px;
                        font-weight: 800;
                        letter-spacing: 0.2em;
                        margin: 32px 0 4px;
                    }
                    .receipt-number {
                        text-align: center;
                        font-size: 12px;
                        color: #7a7368;
                        font-family: "JetBrains Mono", monospace;
                        margin-bottom: 32px;
                    }
                    .receipt-table {
                        width: calc(100%% - 80px);
                        margin: 0 40px 32px;
                        border-collapse: collapse;
                    }
                    .receipt-table tr {
                        border-bottom: 1px solid #eee7da;
                    }
                    .receipt-table td {
                        padding: 14px 4px;
                        font-size: 14px;
                    }
                    .receipt-table td:first-child {
                        color: #7a7368;
                        font-weight: 700;
                        width: 130px;
                    }
                    .receipt-table td:last-child {
                        text-align: right;
                        font-weight: 700;
                    }
                    .receipt-amount-row td {
                        font-size: 20px;
                        color: #0a5548;
                        padding-top: 20px;
                    }
                    .receipt-footer {
                        text-align: center;
                        font-size: 12px;
                        color: #a39c8f;
                        padding: 0 40px 36px;
                    }
                    .print-btn {
                        display: block;
                        margin: 24px auto 0;
                        padding: 12px 28px;
                        border: none;
                        border-radius: 999px;
                        background: #0a5548;
                        color: #fff;
                        font-size: 14px;
                        font-weight: 700;
                        cursor: pointer;
                    }
                    @media print {
                        body { background: #fff; padding: 0; }
                        .receipt { box-shadow: none; border-radius: 0; }
                        .print-btn { display: none; }
                    }
                </style>
                </head>
                <body>
                <div class="receipt">
                    <div class="receipt-head">
                        <p class="brand">I'm Khun · KWZM Center</p>
                        <p class="sub">Computer Training &amp; Language Center</p>
                    </div>
                    <p class="receipt-title">결제 영수증</p>
                    <p class="receipt-number">영수증 번호 %s</p>
                    <table class="receipt-table">
                        <tr><td>받는 분</td><td>%s 님</td></tr>
                        <tr><td>강의명</td><td>%s</td></tr>
                        <tr><td>학생번호</td><td>%s</td></tr>
                        <tr><td>결제 수단</td><td>%s</td></tr>
                        <tr><td>결제 확인일</td><td>%s</td></tr>
                        %s
                        <tr class="receipt-amount-row"><td>결제 금액</td><td>%s</td></tr>
                    </table>
                    <p class="receipt-footer">이 영수증은 I'm Khun / KWZM Center에서 자동 발급됐어요.</p>
                </div>
                <button class="print-btn" onclick="window.print()">인쇄 / PDF로 저장</button>
                </body>
                </html>
                """.formatted(
                nickname, receiptNumber, nickname, application.getCourseName(),
                application.getStudentNumber() != null ? application.getStudentNumber() : "-",
                paymentMethod, issuedDate,
                amountReason.isBlank() ? "" : "<tr><td>비고</td><td>" + amountReason + "</td></tr>",
                amount
        );
    }
}