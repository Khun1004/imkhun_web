package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.FuturePlanItem;
import com.imkhun.imkhun.domain.FuturePlanSection;
import com.imkhun.imkhun.dto.*;
import com.imkhun.imkhun.repository.FuturePlanItemRepository;
import com.imkhun.imkhun.repository.FuturePlanSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FuturePlanService {

    private static final String DEFAULT_INTRO =
            "지금은 저 혼자 학생들을 만나고 있지만, 여기서 멈추고 싶지는 않아요. 한 사람의 수업으로는 닿을 수 있는 학생 수도, 가르칠 수 있는 과목의 폭도 어쩔 수 없이 한계가 있으니까요. 앞으로 몇 년 안에 이 공간을 언어와 컴퓨터 수업을 넘어서, 더 많은 선생님과 더 많은 배움이 오가는 곳으로 천천히 키워나가려고 해요.\n\n"
                    + "거창한 사업 계획서는 아니고, 지금 제가 진심으로 그리고 있는 세 가지 방향이에요. 하나씩 소개해드릴게요.";

    private static final String DEFAULT_CTA_TITLE = "세 가지 모두, 아직은 계획 단계예요";
    private static final String DEFAULT_CTA_TEXT =
            "하지만 하나씩 차근차근 현실로 만들어가려고 해요. 선생님으로 함께하고 싶으시거나, 웹 개발을 배우고 싶으시거나, 개발자로 힘을 보태고 싶으시다면 — 고객센터로 편하게 연락해주세요. 작은 관심이라도 정말 큰 힘이 돼요.";

    private static final String[] DEFAULT_ITEM_TITLES = {
            "함께할 선생님을 찾습니다",
            "홈페이지 만드는 법도 직접 가르쳐드려요",
            "이 홈페이지, 같이 키워갈 개발자를 구합니다"
    };

    private static final String[] DEFAULT_ITEM_CONTENTS = {
            "지금까지는 한국어와 컴퓨터 활용 수업을 저 혼자 맡아왔지만, 언제까지나 그럴 수는 없다는 걸 잘 알고 있어요. 일본어·태국어·영어처럼 제가 직접 가르치기 어려운 언어는 물론이고, 요리·디자인·악기·자격증 강의처럼 완전히 다른 분야까지, 이 공간 안에서 자기만의 수업을 열어보고 싶은 선생님을 모실 계획이에요.\n\n"
                    + "거창한 자격증이나 정해진 학력이 없어도 괜찮아요. 누군가에게 진심으로 무언가를 가르쳐본 경험이 있고, 그 경험을 나누고 싶은 마음이 있다면 그걸로 충분하다고 생각해요. 학생을 구하는 부담이나 신청·결제 관리처럼 번거로운 일들은 이미 만들어둔 KWZM Center 시스템이 대신해줄 거예요. 선생님은 오직 가르치는 일에만 집중하시면 됩니다.",

            "지금 보고 계신 이 사이트도, 학생들이 쓰는 KWZM Center도 전부 제가 하나하나 배워가며 만든 것들이에요. 처음엔 HTML 태그 하나 치는 것도 낯설고 어려웠지만, 지금은 React로 화면을 만들고 Spring Boot로 서버까지 직접 다루고 있어요. 그 경험을 저 혼자만 갖고 있기보다, 저처럼 \"내 손으로 뭔가를 만들어보고 싶다\"고 생각하는 분들에게 그대로 나눠드리고 싶어요.\n\n"
                    + "HTML과 CSS로 화면에 글자와 색을 입히는 아주 기초적인 단계부터 시작해서, JavaScript로 화면을 실제로 움직여보고, React로 지금 이 사이트 같은 실제 서비스를 만들고, 나중에는 React Native로 모바일 앱까지 만들어보는 과정을 차근차근 준비하고 있어요. 컴퓨터공학을 전공하지 않고, 자격증 하나 없이 시작한 사람이 어디까지 갈 수 있는지, 제가 걸어온 길 그대로를 보여드릴게요.",

            "지금 보고 계신 이 사이트와 KWZM Center는 아직 저 혼자 만들어가는 중이라 부족한 점이 많아요. 결제 시스템을 더 매끄럽게 다듬고, 학생들이 더 편하게 쓸 수 있는 기능을 하나씩 붙이고, 언젠가는 모바일 앱으로도 확장하는 일들이 남아있어요. 이 모든 걸 혼자 해내기보다, 함께 고민하고 함께 코드를 짤 개발자를 찾고 있어요.\n\n"
                    + "화려한 경력보다, 작은 프로젝트라도 처음부터 끝까지 완성해본 경험과 배우면서 함께 성장하고 싶은 마음을 더 소중하게 생각해요. 프론트엔드든 백엔드든, 둘 다든 상관없어요. 이 사이트가 커가는 과정에 초기 멤버로 함께하고 싶으신 분이 있다면, 언제든 편하게 말을 걸어주세요."
    };

    private static final String[] DEFAULT_ITEM_TAGS = {
            "언어 선생님,컴퓨터·디자인 선생님,그 외 다양한 재능",
            "HTML,CSS,JavaScript,React,React Native",
            "프론트엔드,백엔드,풀스택 모두 환영"
    };

    private final FuturePlanSectionRepository futurePlanSectionRepository;
    private final FuturePlanItemRepository futurePlanItemRepository;

    public FuturePlanService(FuturePlanSectionRepository futurePlanSectionRepository,
                             FuturePlanItemRepository futurePlanItemRepository) {
        this.futurePlanSectionRepository = futurePlanSectionRepository;
        this.futurePlanItemRepository = futurePlanItemRepository;
    }

    @Transactional
    public FuturePlanResponse getFuturePlan() {
        FuturePlanSection section = getOrCreateSection();
        List<FuturePlanItemResponse> items = getOrCreateItems();
        return new FuturePlanResponse(section.getIntroText(), section.getCtaTitle(), section.getCtaText(), items);
    }

    @Transactional
    public void updateSection(UpdateFuturePlanSectionRequest request) {
        if (request.introText() == null || request.introText().isBlank()) {
            throw new IllegalStateException("인트로 문단을 입력해주세요.");
        }
        FuturePlanSection section = getOrCreateSection();
        section.update(request.introText(), request.ctaTitle(), request.ctaText());
        futurePlanSectionRepository.save(section);
    }

    @Transactional
    public FuturePlanItemResponse createItem(CreateFuturePlanItemRequest request) {
        validateItem(request.title(), request.content(), request.tags());
        FuturePlanItem saved = futurePlanItemRepository.save(
                FuturePlanItem.create(request.sortOrder(), request.title(), request.content(), request.tags()));
        return toItemResponse(saved);
    }

    @Transactional
    public FuturePlanItemResponse updateItem(Long id, CreateFuturePlanItemRequest request) {
        validateItem(request.title(), request.content(), request.tags());
        FuturePlanItem item = futurePlanItemRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("계획 카드를 찾을 수 없어요."));
        item.update(request.sortOrder(), request.title(), request.content(), request.tags());
        return toItemResponse(futurePlanItemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long id) {
        futurePlanItemRepository.deleteById(id);
    }

    private void validateItem(String title, String content, String tags) {
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            throw new IllegalStateException("제목과 내용을 입력해주세요.");
        }
    }

    private FuturePlanSection getOrCreateSection() {
        List<FuturePlanSection> all = futurePlanSectionRepository.findAll();
        if (!all.isEmpty()) return all.get(0);
        return futurePlanSectionRepository.save(FuturePlanSection.create(DEFAULT_INTRO, DEFAULT_CTA_TITLE, DEFAULT_CTA_TEXT));
    }

    private List<FuturePlanItemResponse> getOrCreateItems() {
        List<FuturePlanItem> existing = futurePlanItemRepository.findAllByOrderBySortOrderAsc();
        if (existing.isEmpty()) {
            for (int i = 0; i < DEFAULT_ITEM_TITLES.length; i++) {
                futurePlanItemRepository.save(
                        FuturePlanItem.create(i + 1, DEFAULT_ITEM_TITLES[i], DEFAULT_ITEM_CONTENTS[i], DEFAULT_ITEM_TAGS[i]));
            }
            existing = futurePlanItemRepository.findAllByOrderBySortOrderAsc();
        }
        return existing.stream().map(this::toItemResponse).toList();
    }

    private FuturePlanItemResponse toItemResponse(FuturePlanItem item) {
        return new FuturePlanItemResponse(item.getId(), item.getSortOrder(), item.getTitle(), item.getContent(), item.getTags());
    }
}