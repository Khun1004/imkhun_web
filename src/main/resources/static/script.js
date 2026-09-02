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