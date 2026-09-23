package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.CompanyInfo;
import com.imkhun.imkhun.dto.CompanyInfoResponse;
import com.imkhun.imkhun.dto.UpdateCompanyInfoRequest;
import com.imkhun.imkhun.repository.CompanyInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyInfoService {

    private static final String COMPANY_DEFAULT_EYEBROW = "What is this space";
    private static final String COMPANY_DEFAULT_TITLE = "회사소개";
    private static final String COMPANY_DEFAULT_CONTENT =
            "I'm Khun / KWZM Center는 거창한 회사가 아니라, 제가 직접 학생을 만나고 직접 홈페이지까지 만들어가는 아주 작은 1인 교육 공간이에요.\n\n"
                    + "\"I'm Khun\"은 제 개인 브랜드이자 이 모든 걸 시작한 이름이고, \"KWZM Computer Training & Language Center\"는 그 안에서 학생들이 실제로 수업을 듣고 관리받는 공간의 이름이에요. 처음엔 한국어를 가르치고 싶다는 마음 하나로 시작했는데, 신청서를 종이로 받고 출석을 손으로 체크하는 게 너무 번거로워서, 아예 제가 직접 신청부터 출석·결제·숙제까지 관리할 수 있는 홈페이지를 만들어버렸어요.\n\n"
                    + "그래서 지금은 \"가르치는 사람\"이자 \"그 수업을 운영하는 시스템을 만드는 사람\"이라는 두 가지 역할을 동시에 하고 있어요. 학생 입장에서 불편했던 점을 발견하면, 그날 바로 코드를 고쳐서 다음 날 바로 반영하는 식으로 조금씩 키워가고 있는 공간이에요. 지금 이 화면도 그렇게 만들어진 것 중 하나예요.";

    private static final String BUSINESS_DEFAULT_EYEBROW = "How it works";
    private static final String BUSINESS_DEFAULT_TITLE = "사업소개";
    private static final String BUSINESS_DEFAULT_CONTENT =
            "지금은 한국어·컴퓨터 활용 수업을 직접 가르치면서, 신청부터 출석·결제·숙제 관리까지 되는 학생 전용 홈페이지(KWZM Center)를 함께 운영하고 있어요.\n\n"
                    + "수업은 \"나랑 같이 공부하기\"(실시간 수업)와 \"영상으로만 듣기\" 두 가지 방식 중에서 고를 수 있고, 처음 시작하시는 분들을 위한 무료체험 신청도 따로 마련해뒀어요. 신청이 승인되면 학생은 KWZM Center라는 전용 페이지에서 본인 시간표를 확인하고, 수업 시작 전후로 직접 출석 체크를 하고, 결제 안내를 받아 입금 확인까지 스스로 처리할 수 있어요.\n\n"
                    + "수업이 끝난 뒤에도 숙제를 확인하고 제출하거나, 단어장으로 복습하거나, 발음을 녹음해서 저한테 바로 들려줄 수도 있어요. 이 모든 기능은 학생들이 실제로 겪었던 불편함에서 하나씩 출발한 거예요. 큰 회사처럼 정해진 매뉴얼이 있는 건 아니지만, 그만큼 학생 한 명 한 명의 이야기를 듣고 빠르게 바꿔나갈 수 있다는 게 이 공간의 가장 큰 장점이라고 생각해요.";

    private final CompanyInfoRepository companyInfoRepository;

    public CompanyInfoService(CompanyInfoRepository companyInfoRepository) {
        this.companyInfoRepository = companyInfoRepository;
    }

    @Transactional
    public CompanyInfoResponse getByType(String type) {
        CompanyInfo info = companyInfoRepository.findByType(type).orElseGet(() -> createDefault(type));
        return toResponse(info);
    }

    @Transactional
    public CompanyInfoResponse update(String type, UpdateCompanyInfoRequest request) {
        if (request.title() == null || request.title().isBlank() || request.content() == null || request.content().isBlank()) {
            throw new IllegalStateException("제목과 내용을 입력해주세요.");
        }
        CompanyInfo info = companyInfoRepository.findByType(type).orElseGet(() -> createDefault(type));
        info.update(request.eyebrow(), request.title(), request.content());
        return toResponse(companyInfoRepository.save(info));
    }

    private CompanyInfo createDefault(String type) {
        CompanyInfo info;
        if ("BUSINESS".equals(type)) {
            info = CompanyInfo.create("BUSINESS", BUSINESS_DEFAULT_EYEBROW, BUSINESS_DEFAULT_TITLE, BUSINESS_DEFAULT_CONTENT);
        } else {
            info = CompanyInfo.create("COMPANY", COMPANY_DEFAULT_EYEBROW, COMPANY_DEFAULT_TITLE, COMPANY_DEFAULT_CONTENT);
        }
        return companyInfoRepository.save(info);
    }

    private CompanyInfoResponse toResponse(CompanyInfo info) {
        return new CompanyInfoResponse(info.getId(), info.getType(), info.getEyebrow(), info.getTitle(), info.getContent());
    }
}