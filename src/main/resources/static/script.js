// data-src가 지정된 섹션은 별도 HTML 파일(조각)을 불러와서 채워 넣음
// 예: <section id="about" data-src="sections/about/aboutme.html"></section>
//
// admin.html처럼, 불러온 조각 "안에" 또 다른 data-src(예: 언어별 하위 조각)가 있을 수 있어서
// 새로 생긴 data-src가 없어질 때까지 반복해서 불러옴
async function loadFragments() {
    const targets = document.querySelectorAll("[data-src]:not([data-loaded])");
    if (targets.length === 0) return;

    await Promise.all(
        Array.from(targets).map(async (el) => {
            try {
                const res = await fetch(el.dataset.src);
                if (!res.ok) throw new Error("불러오기 실패");
                el.innerHTML = await res.text();
                el.setAttribute("data-loaded", "true");
            } catch (err) {
                el.innerHTML = '<p class="section-eyebrow">오류</p><h2>내용을 불러오지 못했어요</h2>';
                console.error(err);
            }
        })
    );

    // 방금 불러온 내용 안에 새로운 data-src가 또 있을 수 있으니 한 번 더 확인
    await loadFragments();
}

loadFragments().then(() => {
    setupRevealAnimations();
    document.dispatchEvent(new Event("fragments:loaded"));
});

// 스크롤하면서 화면에 들어오는 요소를 서서히 나타나게 함 (.reveal 클래스)
function setupRevealAnimations() {
    const revealEls = document.querySelectorAll(".reveal");
    if (!revealEls.length) return;

    const observer = new IntersectionObserver(
        (entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                    observer.unobserve(entry.target);
                }
            });
        },
        { threshold: 0.15 }
    );

    revealEls.forEach((el) => observer.observe(el));
}

// 헤더 / 푸터를 고정했기 때문에, 실제 높이만큼 body에 여백을 줘서
// 콘텐츠가 가려지지 않게 함 (화면 크기에 따라 높이가 달라질 수 있어 매번 재계산)
function syncFixedOffsets() {
    const header = document.getElementById("siteHeader");
    const footer = document.getElementById("siteFooter");
    const root = document.documentElement;

    if (header) root.style.setProperty("--header-h", `${header.offsetHeight}px`);
    if (footer) root.style.setProperty("--footer-h", `${footer.offsetHeight}px`);
}

window.addEventListener("load", syncFixedOffsets);
window.addEventListener("resize", syncFixedOffsets);
syncFixedOffsets();

// 공부 / 가르침 버튼은 지금은 별도 화면 없이 눌러도 동작 안 함
// (나중에 각각 다른 화면으로 연결할 예정)

// 서브 메뉴 (자기소개 / 취미 / 언어 / 리뷰 / 강의신청) 전환 —
// 누른 항목의 화면만 보이고 나머지는 숨김
const subnavLinks = document.querySelectorAll(".subnav-link");
const contentPanels = document.querySelectorAll(".content-panel");

subnavLinks.forEach((link) => {
    link.addEventListener("click", (e) => {
        e.preventDefault();

        subnavLinks.forEach((l) => {
            l.classList.remove("active");
            l.setAttribute("aria-selected", "false");
        });
        contentPanels.forEach((p) => p.classList.remove("active"));

        link.classList.add("active");
        link.setAttribute("aria-selected", "true");
        document.getElementById(link.dataset.panel).classList.add("active");
    });
});

// 채팅 버튼 — 누르면 메신저 카드 메뉴 열고 닫기
const chatFab = document.getElementById("chatFab");
const chatMenu = document.getElementById("chatMenu");
const chatClose = document.getElementById("chatClose");

function closeChatMenu() {
    chatMenu.classList.remove("open");
    chatFab.setAttribute("aria-expanded", "false");
    chatMenu.setAttribute("aria-hidden", "true");
}

function openChatMenu() {
    chatMenu.classList.add("open");
    chatFab.setAttribute("aria-expanded", "true");
    chatMenu.setAttribute("aria-hidden", "false");
}

if (chatFab && chatMenu) {
    chatFab.addEventListener("click", () => {
        chatMenu.classList.contains("open") ? closeChatMenu() : openChatMenu();
    });

    if (chatClose) {
        chatClose.addEventListener("click", (e) => {
            e.stopPropagation();
            closeChatMenu();
        });
    }

    // 메뉴 바깥을 누르면 닫힘
    document.addEventListener("click", (e) => {
        const isInsideWidget = e.target.closest(".chat-widget");
        if (!isInsideWidget && chatMenu.classList.contains("open")) {
            closeChatMenu();
        }
    });

    // Esc 키로도 닫기
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && chatMenu.classList.contains("open")) {
            closeChatMenu();
        }
    });
}

// "회사소개 / 사업소개" 오른쪽 독 — 출석체크 독이랑 같은 방식(탭 누르면 옆에서 슬라이드로 펼쳐짐)
(function setupInfoDock() {
    const dock = document.getElementById("infoDock");
    const tab = document.getElementById("infoDockTab");
    const closeBtn = document.getElementById("infoDockClose");
    if (!dock || !tab) return;

    const openDock = () => dock.classList.add("is-open");
    const closeDock = () => dock.classList.remove("is-open");

    tab.addEventListener("click", () => {
        dock.classList.contains("is-open") ? closeDock() : openDock();
    });
    closeBtn?.addEventListener("click", closeDock);

    document.addEventListener("click", (e) => {
        if (dock.classList.contains("is-open") && !e.target.closest(".info-dock")) {
            closeDock();
        }
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && dock.classList.contains("is-open")) closeDock();
    });
})();

// "회사소개" / "사업소개" 내용을 서버에서 불러와서 채워줌 (관리자가 수정하면 여기도 같이 바뀜)
function escapeHtmlForSite(text) {
    const div = document.createElement("div");
    div.textContent = text == null ? "" : String(text);
    return div.innerHTML;
}

async function loadCompanyInfoSection(type, eyebrowId, titleId, bodyId) {
    const eyebrowEl = document.getElementById(eyebrowId);
    const titleEl = document.getElementById(titleId);
    const bodyEl = document.getElementById(bodyId);
    if (!bodyEl) return;

    try {
        const res = await fetch(`/api/company-info/${type}`);
        if (!res.ok) return;
        const data = await res.json();

        if (eyebrowEl && data.eyebrow) eyebrowEl.textContent = data.eyebrow;
        if (titleEl && data.title) titleEl.textContent = data.title;

        const paragraphs = (data.content || "").split(/\n\s*\n/).filter((p) => p.trim());
        bodyEl.innerHTML = paragraphs.map((p) => `<p>${escapeHtmlForSite(p.trim())}</p>`).join("");
    } catch (err) {
        console.error(err);
        bodyEl.innerHTML = `<p>내용을 불러오지 못했어요.</p>`;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    loadCompanyInfoSection("COMPANY", "infoDockCompanyEyebrow", "infoDockCompanyTitle", "infoDockCompanyBody");
    loadCompanyInfoSection("BUSINESS", "infoDockBusinessEyebrow", "infoDockBusinessTitle", "infoDockBusinessBody");
});

// "미래 계획" 페이지 — 서버에서 인트로/카드/CTA를 불러와서 그려줌 (관리자가 카드를 등록/수정/삭제하면 그대로 반영됨)
const FUTUREPLAN_ICONS = [
    '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3 2 8l10 5 10-5-10-5Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M6 10.5V16c0 1.5 2.7 3 6 3s6-1.5 6-3v-5.5" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M22 8v6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>',
    '<svg viewBox="0 0 24 24" fill="none"><path d="M9 8 4 12l5 4M15 8l5 4-5 4M13 5l-2 14" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    '<svg viewBox="0 0 24 24" fill="none"><circle cx="7" cy="6" r="2.5" stroke="currentColor" stroke-width="1.6"/><circle cx="17" cy="6" r="2.5" stroke="currentColor" stroke-width="1.6"/><path d="M2.5 19c.8-3.6 2.6-5.5 4.5-5.5s3.7 1.9 4.5 5.5M12.5 19c.8-3.6 2.6-5.5 4.5-5.5s3.7 1.9 4.5 5.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>',
];
const FUTUREPLAN_TAG_COLORS = ["rose", "teal", "gold", "code", "sky"];

async function loadFuturePlan() {
    const introEl = document.getElementById("futureplanIntro");
    const timelineEl = document.getElementById("futureplanTimeline");
    const ctaEl = document.getElementById("futureplanCta");
    if (!introEl || !timelineEl) return;

    try {
        const res = await fetch("/api/future-plan");
        if (!res.ok) return;
        const data = await res.json();

        introEl.innerHTML = (data.introText || "")
            .split(/\n\s*\n/)
            .filter((p) => p.trim())
            .map((p) => `<p>${escapeHtmlForSite(p.trim())}</p>`)
            .join("");

        let tagCounter = 0;
        timelineEl.innerHTML = (data.items || [])
            .map((item, index) => {
                const icon = FUTUREPLAN_ICONS[index % FUTUREPLAN_ICONS.length];
                const paragraphs = (item.content || "")
                    .split(/\n\s*\n/)
                    .filter((p) => p.trim())
                    .map((p) => `<p>${escapeHtmlForSite(p.trim())}</p>`)
                    .join("");
                const tags = (item.tags || "")
                    .split(",")
                    .map((t) => t.trim())
                    .filter(Boolean)
                    .map((t) => {
                        const color = FUTUREPLAN_TAG_COLORS[tagCounter++ % FUTUREPLAN_TAG_COLORS.length];
                        return `<span class="futureplan-tag futureplan-tag--${color}">${escapeHtmlForSite(t)}</span>`;
                    })
                    .join("");

                return `
          <div class="futureplan-card">
            <div class="futureplan-card-head">
              <span class="futureplan-icon" aria-hidden="true">${icon}</span>
              <div>
                <p class="futureplan-card-eyebrow">Plan ${String(index + 1).padStart(2, "0")}</p>
                <h3>${escapeHtmlForSite(item.title)}</h3>
              </div>
            </div>
            ${paragraphs}
            <div class="futureplan-tags">${tags}</div>
          </div>
        `;
            })
            .join("");

        if (ctaEl) {
            ctaEl.innerHTML = `
        <p class="futureplan-cta-title">${escapeHtmlForSite(data.ctaTitle || "")}</p>
        <p>${escapeHtmlForSite(data.ctaText || "")}</p>
      `;
        }
    } catch (err) {
        console.error(err);
        introEl.innerHTML = `<p>내용을 불러오지 못했어요.</p>`;
    }
}

document.addEventListener("fragments:loaded", () => {
    loadFuturePlan();
});