// '강의 자료' 화면 전용 스크립트 — 원래 있던 디자인/동작(카드 누르면 자료 뷰어 모달,
// 좌우 화살표로 스크롤, 하단 경고 배너)은 그대로 두고, 카드 내용은 서버에서 불러와
// 관리자가 등록/수정/삭제한 대로 보여줌 + 카드가 천천히 자동으로 한 칸씩 넘어가는 기능 추가

const LANGUAGE_GROUP_LABEL = {
    korean: "한국어",
    japanese: "일본어",
    thai: "태국어",
    english: "영어",
    computer: "컴퓨터",
    other: "기타",
};

// 화면에 보여줄 언어 순서(원래 화면과 같은 순서: 한국어 → 컴퓨터, 그 외는 뒤에 붙임)
const LANGUAGE_ORDER = ["korean", "japanese", "thai", "english", "computer", "other"];

// 장식용 벤 다이어그램 원 중에 어떤 걸 채울지 — 카드 순서에 따라 돌아가며 사용
const VENN_FILL_PATTERNS = [[1], [2], [3], [1, 2], [2, 3], [1, 3]];

const LANGUAGE_MATERIAL_AUTOPLAY_MS = 3200;

function escapeHtmlForLanguage(text) {
    const div = document.createElement("div");
    div.textContent = text == null ? "" : String(text);
    return div.innerHTML;
}

// ---------- 카드 하나 ----------

function languageMaterialCardHtml(m, index) {
    const isComputer = m.language === "computer";
    const updown = index % 2 === 0 ? "material-card--up" : "material-card--down";
    const computerClass = isComputer ? " material-card--computer" : "";
    const fillClass = isComputer ? "venn-circle--fill-computer" : "venn-circle--fill";
    const linkClass = isComputer ? " material-link--computer" : "";

    const fillSet = new Set(VENN_FILL_PATTERNS[index % VENN_FILL_PATTERNS.length]);
    const circles = [1, 2, 3]
        .map((n) => `<span class="venn-circle${fillSet.has(n) ? ` ${fillClass}` : ""}" style="--vc:${n}"></span>`)
        .join("");

    const modalTitle = `${LANGUAGE_GROUP_LABEL[m.language] || ""} ${m.title || ""}`.trim();

    return `
    <a href="#" class="material-card ${updown}${computerClass}" data-file="${m.fileUrl ? escapeHtmlForLanguage(m.fileUrl) : ""}" data-title="${escapeHtmlForLanguage(modalTitle)}">
      ${m.badge ? `<span class="material-badge">${escapeHtmlForLanguage(m.badge)}</span>` : ""}
      <h3>${escapeHtmlForLanguage(m.title)}</h3>
      ${m.description ? `<p>${escapeHtmlForLanguage(m.description)}</p>` : ""}
      <div class="material-venn">${circles}</div>
      <span class="material-link${linkClass}">자료 보기 <i aria-hidden="true">&rsaquo;</i></span>
    </a>
  `;
}

function languageMaterialGroupHtml(lang, items, isFirst) {
    const labelClass = isFirst ? "material-group-label" : "material-group-label material-group-label--spaced";

    return `
    <p class="${labelClass}">${escapeHtmlForLanguage(LANGUAGE_GROUP_LABEL[lang] || lang)}</p>
    <div class="material-carousel" data-material-group="${lang}">
      <button class="carousel-arrow carousel-arrow--prev" type="button" aria-label="이전 자료">
        <svg viewBox="0 0 24 24" fill="none"><path d="M15 6l-6 6 6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>
      <div class="material-grid material-grid--scroll">
        ${items.map((m, i) => languageMaterialCardHtml(m, i)).join("")}
      </div>
      <button class="carousel-arrow carousel-arrow--next" type="button" aria-label="다음 자료">
        <svg viewBox="0 0 24 24" fill="none"><path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>
    </div>
  `;
}

// 한 줄(한 언어)의 카드가 천천히, 자동으로 한 칸씩 넘어가게 함 — 등록된 자료(있는 등급)까지만
// 가고, 끝에 도달하면 더 넘기지 않고 멈춤 (처음으로 되돌아가지 않음)
function setupCarouselAutoplay(carouselEl) {
    const track = carouselEl.querySelector(".material-grid--scroll");
    if (!track) return;

    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (prefersReducedMotion) return;

    let timer = null;

    function cardStep() {
        const first = track.children[0];
        const second = track.children[1];
        if (first && second) return second.offsetLeft - first.offsetLeft;
        return first ? first.getBoundingClientRect().width + 18 : track.clientWidth * 0.5;
    }

    function advance() {
        const maxScroll = track.scrollWidth - track.clientWidth;
        if (maxScroll <= 2) {
            window.clearInterval(timer);
            return;
        }
        if (track.scrollLeft >= maxScroll - 4) {
            // 마지막(있는 등급 끝)까지 왔으면 더 이상 넘기지 않음
            window.clearInterval(timer);
            return;
        }
        track.scrollBy({ left: cardStep(), behavior: "smooth" });
    }

    timer = window.setInterval(advance, LANGUAGE_MATERIAL_AUTOPLAY_MS);
}

async function loadLanguageMaterials() {
    const groupsEl = document.getElementById("languageMaterialsGroups");
    if (!groupsEl) return;

    try {
        const res = await fetch("/api/language-materials");
        if (!res.ok) return;
        const materials = await res.json();

        if (materials.length === 0) {
            groupsEl.innerHTML = `<p class="material-group-label">곧 다양한 자료를 보여드릴게요.</p>`;
            return;
        }

        const byLanguage = {};
        materials.forEach((m) => {
            const key = m.language || "other";
            if (!byLanguage[key]) byLanguage[key] = [];
            byLanguage[key].push(m);
        });

        const orderedKeys = [
            ...LANGUAGE_ORDER.filter((k) => byLanguage[k]),
            ...Object.keys(byLanguage).filter((k) => !LANGUAGE_ORDER.includes(k)),
        ];

        groupsEl.innerHTML = orderedKeys
            .map((lang, i) => languageMaterialGroupHtml(lang, byLanguage[lang], i === 0))
            .join("");

        groupsEl.querySelectorAll("[data-material-group]").forEach((carouselEl) => {
            setupCarouselAutoplay(carouselEl);
        });
    } catch (err) {
        console.error(err);
        groupsEl.innerHTML = `<p class="material-group-label">자료를 불러오지 못했어요.</p>`;
    }
}

// ---------- 자료 뷰어 모달 (원래 쓰던 그대로) ----------

// 보는 사람이 누구인지(닉네임) 한 번만 물어보고 기억해둠 — 워터마크용.
// 로그인 안 했으면 "KWZM 수강생"이라는 일반 문구로 대신함
let cachedWatermarkLabel = null;
async function getWatermarkLabel() {
    if (cachedWatermarkLabel) return cachedWatermarkLabel;
    try {
        const res = await fetch("/api/auth/me");
        if (res.ok) {
            const data = await res.json();
            if (data && data.nickname) {
                cachedWatermarkLabel = data.nickname;
                return cachedWatermarkLabel;
            }
        }
    } catch (err) {
        // 로그인 정보를 못 가져와도 워터마크 자체는 그냥 일반 문구로 보여주면 됨
    }
    cachedWatermarkLabel = "KWZM 수강생";
    return cachedWatermarkLabel;
}

function renderWatermark(label) {
    const el = document.getElementById("materialWatermark");
    if (!el) return;

    const now = new Date();
    const stamp = `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, "0")}.${String(now.getDate()).padStart(2, "0")} ${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
    const text = `${label} · 무단 캡처·배포 금지 · ${stamp}`;

    const rowsHtml = Array.from({ length: 10 })
        .map(
            () => `<div class="material-watermark-row">${Array.from({ length: 4 }).map(() => `<span>${escapeHtmlForLanguage(text)}</span>`).join("")}</div>`
        )
        .join("");

    el.innerHTML = rowsHtml;
}

// PDF를 브라우저 기본 뷰어로 열면 거기 자체 다운로드·인쇄 버튼이 그대로 보여서,
// "다운로드·인쇄 제한"이 무색해짐. 그래서 브라우저 PDF 뷰어의 툴바/사이드바를 최대한 숨기는
// 옵션을 주소에 붙여줌 (크롬·엣지 등 대부분의 브라우저가 지원, 100% 보장은 아님)
function withViewerProtectionParams(url) {
    if (!url) return url;
    // 확장자로 PDF인지 구분하지 않고 항상 붙임 — 확장자가 없는 파일(예: 서버에 .pdf 없이 저장된 경우)도
    // 놓치지 않기 위해서. PDF가 아닌 파일에는 이 옵션이 그냥 무시되니 문제 없음
    const hashParams = "toolbar=0&navpanes=0&statusbar=0&scrollbar=0";
    return url.includes("#") ? `${url}&${hashParams}` : `${url}#${hashParams}`;
}

async function openMaterialModal(fileUrl, title) {
    const modal = document.getElementById("materialModal");
    const frame = document.getElementById("materialFrame");
    const titleEl = document.getElementById("materialModalTitle");
    if (!modal || !frame) return;

    if (!fileUrl) {
        alert("아직 등록된 파일이 없어요. 선생님께 문의해주세요.");
        return;
    }

    // 모달이 헤더와 같은 화면 안쪽 깊숙이(섹션 안)에 들어있으면, 헤더가 z-index와
    // 상관없이 모달 위에 그대로 그려지는 렌더링 버그가 있었음(실제로 확인됨).
    // 그래서 열 때마다 모달을 body 바로 아래로 옮겨서, 어떤 화면(탭)에서 열리든
    // 항상 맨 위에 제대로 뜨게 함.
    if (modal.parentElement !== document.body) {
        document.body.appendChild(modal);
    }

    frame.src = withViewerProtectionParams(fileUrl);
    if (titleEl) titleEl.textContent = title || "강의 자료";

    const label = await getWatermarkLabel();
    renderWatermark(label);

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
    document.body.classList.add("modal-open");
}

function closeMaterialModal() {
    const modal = document.getElementById("materialModal");
    const frame = document.getElementById("materialFrame");
    const watermark = document.getElementById("materialWatermark");
    if (!modal) return;

    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
    document.body.style.overflow = "";
    if (frame) frame.src = ""; // 재생/로드 중단
    if (watermark) watermark.innerHTML = "";
}

document.addEventListener("click", (e) => {
    const card = e.target.closest(".material-card");
    if (card) {
        e.preventDefault();
        openMaterialModal(card.dataset.file, card.dataset.title);
        return;
    }

    if (e.target.closest("[data-modal-close]")) {
        closeMaterialModal();
        return;
    }

    const arrow = e.target.closest(".carousel-arrow");
    if (arrow) {
        const track = arrow.closest(".material-carousel")?.querySelector(".material-grid--scroll");
        if (track) {
            const amount = track.clientWidth * 0.8;
            const isPrev = arrow.classList.contains("carousel-arrow--prev");
            track.scrollBy({ left: isPrev ? -amount : amount, behavior: "smooth" });
        }
    }
});

document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeMaterialModal();
});

// 우클릭(다운로드/이미지로 저장 메뉴) 방지 — 보조 수단일 뿐, 완벽한 차단은 아님
document.addEventListener("contextmenu", (e) => {
    if (e.target.closest(".material-modal-body")) {
        e.preventDefault();
    }
});

document.addEventListener("fragments:loaded", () => {
    loadLanguageMaterials();
});