// '나만의 공부 화면' (관리자 전용) 독립 페이지 전체 로직

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
                el.innerHTML = '<p class="admin-empty-text">내용을 불러오지 못했어요</p>';
                console.error(err);
            }
        })
    );

    await loadFragments();
}

loadFragments().then(() => {
    document.dispatchEvent(new Event("fragments:loaded"));
});

function showAdminLoginPage() {
    const loginPage = document.getElementById("adminLoginPage");
    const screen = document.getElementById("adminScreen");
    if (loginPage) loginPage.hidden = false;
    if (screen) screen.hidden = true;
}

function showAdminScreen() {
    const loginPage = document.getElementById("adminLoginPage");
    const screen = document.getElementById("adminScreen");
    if (loginPage) loginPage.hidden = true;
    if (screen) screen.hidden = false;
    window.scrollTo({ top: 0, behavior: "instant" });
    autoSelectFirstMaterialsForActiveTab();
    updateHeroContent("dashboard");
    loadDashboard();
    loadAttendanceToday();
}

// 상단 배너에 탭마다 다른 제목/설명을 보여줘요.
const HERO_CONTENT = {
    dashboard: {
        eyebrow: "한눈에 보기",
        title: "대시보드",
        desc: "오늘 확인해야 할 것들을 한 화면에서 볼 수 있어요.",
    },
    personal: {
        eyebrow: "개인 보관함",
        title: "개인용 자료",
        desc: "나만 볼 수 있는 공부 자료를 언어와 항목별로 정리해요.",
    },
    kwzm: {
        eyebrow: "KWZM 학생 공간",
        title: "KWZM 학생 자료",
        desc: "학생들에게 공유할 자료를 언어별로 관리하고 초대해요.",
    },
    video: {
        eyebrow: "KWZM 학생 공간",
        title: "온라인 영상",
        desc: "학생들에게 공유할 영상 자료를 관리하고 초대해요.",
    },
    trial: {
        eyebrow: "무료체험",
        title: "무료체험 자료",
        desc: "누구나 볼 수 있는 체험용 자료를 관리해요.",
    },
    questions: {
        eyebrow: "KWZM 학생 공간",
        title: "학생 질문함",
        desc: "학생들이 익명으로 보낸 질문에 답변해요.",
    },
    notices: {
        eyebrow: "IMKhun 공개 사이트",
        title: "공지사항",
        desc: "공지 글, 강의 시간표, FAQ를 관리해요.",
    },
    students: {
        eyebrow: "학생 관리",
        title: "학생 관리",
        desc: "신청 현황을 확인하고 학생을 승인해요.",
    },
    my: {
        eyebrow: "마이페이지",
        title: "마이",
        desc: "내 정보와 결제 계좌를 관리해요.",
    },
};

function updateHeroContent(key) {
    const content = HERO_CONTENT[key] || HERO_CONTENT.personal;
    const eyebrowEl = document.getElementById("adminHeroEyebrow");
    const titleEl = document.getElementById("adminHeroTitle");
    const descEl = document.getElementById("adminHeroDesc");
    if (eyebrowEl) eyebrowEl.textContent = content.eyebrow;
    if (titleEl) titleEl.textContent = content.title;
    if (descEl) descEl.textContent = content.desc;
}

// 배경 사진 3장을 천천히 돌려가며 보여주는 슬라이드예요.
let heroSlideIndex = 0;
let heroSlideTimer = null;

function goToHeroSlide(index) {
    const slides = document.querySelectorAll("#adminHeroSlides .admin-hero-slide");
    const dots = document.querySelectorAll("#adminHeroDots .admin-hero-dot");
    if (!slides.length) return;

    heroSlideIndex = (index + slides.length) % slides.length;
    slides.forEach((s, i) => s.classList.toggle("is-active", i === heroSlideIndex));
    dots.forEach((d, i) => d.classList.toggle("is-active", i === heroSlideIndex));
}

function initHeroSlideshow() {
    const slides = document.querySelectorAll("#adminHeroSlides .admin-hero-slide");
    if (slides.length <= 1) return;

    document.querySelectorAll("#adminHeroDots .admin-hero-dot").forEach((dot) => {
        dot.addEventListener("click", () => {
            goToHeroSlide(Number(dot.dataset.slideIndex));
            restartHeroSlideshow();
        });
    });

    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (!prefersReducedMotion) {
        restartHeroSlideshow();
    }
}

function restartHeroSlideshow() {
    if (heroSlideTimer) clearInterval(heroSlideTimer);
    heroSlideTimer = setInterval(() => goToHeroSlide(heroSlideIndex + 1), 5000);
}

// 언어를 먼저 고르지 않아도, 탭을 열면 첫 번째 언어(와 첫 번째 항목)의 자료가 바로 보이도록 해요.
function autoSelectFirstMaterials(prefix, scope) {
    const langContainer = document.getElementById(`${prefix}LanguagePills`);
    const firstLangPill = langContainer?.querySelector(".admin-pill");
    if (!firstLangPill) return;

    langContainer.querySelectorAll(".admin-pill").forEach((p) => p.classList.remove("active"));
    firstLangPill.classList.add("active");
    const language = firstLangPill.dataset.lang;

    if (scope === "VIDEO" || scope === "TRIAL") {
        loadMaterials(language, scope, scope);
        return;
    }

    renderCategoryPills(prefix, scope, language);

    if (language !== "other") {
        const catContainer = document.getElementById(`${prefix}CategoryPills`);
        const firstCatPill = catContainer?.querySelector(".admin-pill");
        firstCatPill?.click();
    }
}

function autoSelectFirstMaterialsForActiveTab() {
    const activeTab = document.querySelector(".admin-maintab.active");
    const key = activeTab?.dataset.mainTab;
    if (key !== "personal" && key !== "kwzm" && key !== "video" && key !== "trial") return;

    const scope = key === "kwzm" ? "KWZM" : key === "video" ? "VIDEO" : key === "trial" ? "TRIAL" : "PERSONAL";
    const prefix = prefixForScope(scope);
    const langContainer = document.getElementById(`${prefix}LanguagePills`);
    if (langContainer && !langContainer.querySelector(".admin-pill.active")) {
        autoSelectFirstMaterials(prefix, scope);
    }
}

async function checkAdminSessionOnLoad() {
    try {
        const res = await fetch("/api/admin/check");
        const data = await res.json();
        if (data.isAdmin) {
            showAdminScreen();
            loadAdminNotifUnreadCount();
            loadAdminMe();
            if (!adminNotifPollTimer) {
                adminNotifPollTimer = setInterval(loadAdminNotifUnreadCount, 30000);
            }
        } else {
            showAdminLoginPage();
        }
    } catch (err) {
        console.error(err);
        showAdminLoginPage();
    }
}

async function logoutAdmin() {
    try {
        await fetch("/api/admin/logout", { method: "POST" });
    } catch (err) {
        console.error(err);
    } finally {
        showAdminLoginPage();
    }
}

const ICON_COPY = `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="9" y="9" width="11" height="11" rx="2" stroke="currentColor" stroke-width="1.6"/><path d="M6 15H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v1" stroke="currentColor" stroke-width="1.6"/></svg>`;
const ICON_CHECK = `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

let adminNotifPollTimer = null;
const ADMIN_NOTIF_TYPE_ICON = {
    NEW_APPLICATION: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M6 4h9l4 4v12H6V4Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M9 12h6M9 16h6M9 8h3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`,
    NEW_POST: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 5h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H9l-4.5 3.5a.5.5 0 0 1-.8-.4V17H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`,
    NEW_REVIEW: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m12 3 2.6 5.9 6.4.6-4.8 4.3 1.4 6.3L12 17l-5.6 3.1 1.4-6.3-4.8-4.3 6.4-.6L12 3Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`,
    CONSECUTIVE_ABSENCE: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 9v4M12 16.5h.01" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M10.3 3.9 2.4 18.1a1.5 1.5 0 0 0 1.3 2.2h16.6a1.5 1.5 0 0 0 1.3-2.2L13.7 3.9a1.5 1.5 0 0 0-2.6 0Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`,
    CLASS_CHANGE_REQUEST: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2" stroke="currentColor" stroke-width="1.6"/><path d="M8 3v4M16 3v4M3 10h18" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/><path d="M9 15l2 2 4-4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
};
const ADMIN_NOTIF_ICON_DEFAULT = `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6"/><path d="M12 8v5M12 16h.01" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`;

const CATEGORY_LABEL = { GRAMMAR: "문법", READING: "읽기", WRITING: "쓰기", SPEAKING: "말하기", OTHER: "기타" };
const COMPUTER_CATEGORY_LABEL = { BASIC: "Basic", WORD: "Word", EXCEL: "Excel", POWERPOINT: "PowerPoint", PAGEMAKER: "PageMaker", PHOTOSHOP: "Photoshop" };
const LANGUAGE_LABEL = { korean: "한국어", japanese: "일본어", thai: "태국어", english: "영어", computer: "컴퓨터", video: "영상", other: "기타" };

// 언어에 따라 어떤 항목(카테고리) 라벨 세트를 쓸지 골라줌 — 언어는 문법/읽기/..., 컴퓨터는 Basic/Word/...
function categoryLabelSetFor(language) {
    return language === "computer" ? COMPUTER_CATEGORY_LABEL : CATEGORY_LABEL;
}

function escapeHtmlForAdminMaterial(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
}

let currentMaterialScope = "PERSONAL";
let editingMaterialScope = "PERSONAL";

function materialsApiBase(scope) {
    if (scope === "KWZM") return "/api/admin/kwzm-materials";
    if (scope === "VIDEO") return "/api/admin/video-materials";
    if (scope === "TRIAL") return "/api/admin/trial-materials";
    return "/api/admin/materials";
}

function renderCategoryPills(prefix, scope, language) {
    const catEl = document.getElementById(`${prefix}CategoryPills`);
    const viewEl = document.getElementById(`${prefix}MaterialsView`);
    if (!catEl) return;

    if (language === "other") {
        catEl.hidden = true;
        catEl.innerHTML = "";
        loadMaterials(language, "OTHER", scope);
        return;
    }

    catEl.hidden = false;
    if (viewEl) viewEl.hidden = true;
    catEl.innerHTML = "";

    Object.entries(categoryLabelSetFor(language)).forEach(([key, label]) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "admin-pill";
        btn.textContent = label;
        btn.addEventListener("click", () => {
            catEl.querySelectorAll(".admin-pill").forEach((p) => p.classList.remove("active"));
            btn.classList.add("active");
            loadMaterials(language, key, scope);
        });
        catEl.appendChild(btn);
    });
}

function prefixForScope(scope) {
    if (scope === "KWZM") return "kwzm";
    if (scope === "VIDEO") return "video";
    if (scope === "TRIAL") return "trial";
    return "personal";
}

function scopeLabel(scope) {
    if (scope === "KWZM") return "KWZM 학생용";
    if (scope === "VIDEO") return "온라인 영상";
    if (scope === "TRIAL") return "무료체험";
    return "개인용";
}

function inviteBtnIdForScope(scope) {
    return scope === "VIDEO" ? "videoInviteBtn" : "adminInviteBtn";
}

async function loadMaterials(language, category, scope) {
    currentMaterialScope = scope || "PERSONAL";
    const prefix = prefixForScope(currentMaterialScope);
    const emptyText = document.getElementById(`${prefix}MaterialsEmpty`);
    const list = document.getElementById(`${prefix}MaterialsList`);
    const heading = document.getElementById(`${prefix}MaterialsHeading`);
    const view = document.getElementById(`${prefix}MaterialsView`);
    const inviteBtn = document.getElementById(inviteBtnIdForScope(currentMaterialScope));
    if (!list) return;

    if (view) view.hidden = false;
    heading.textContent = (currentMaterialScope === "VIDEO" || currentMaterialScope === "TRIAL")
        ? `${LANGUAGE_LABEL[language] || language}`
        : `${LANGUAGE_LABEL[language] || language} · ${categoryLabelSetFor(language)[category] || category}`;

    if (inviteBtn) {
        if (currentMaterialScope === "KWZM" || currentMaterialScope === "VIDEO") {
            inviteBtn.hidden = false;
            currentInviteLanguage = language;
            currentInviteScope = currentMaterialScope;
            updateInviteCountBadge();
        } else {
            inviteBtn.hidden = true;
        }
    }

    try {
        const res = await fetch(`${materialsApiBase(currentMaterialScope)}?language=${language}&category=${category}`);
        if (!res.ok) return;
        const materials = await res.json();

        list.innerHTML = "";
        emptyText.hidden = materials.length > 0;

        materials.forEach((m) => {
            const item = document.createElement("div");
            item.className = "admin-material-item";
            item.dataset.materialId = m.id;

            const files = m.files || [];
            const first = files[0];
            const firstIsImage = first && first.fileType && first.fileType.startsWith("image/");
            const firstIsLink = first && first.linkUrl;
            const firstIsText = first && first.textContent && !first.linkUrl && !first.fileData;

            const ICON_LINK = `<svg viewBox="0 0 24 24" fill="none"><path d="M9.5 14.5l5-5M8 10l-1.5 1.5a3.5 3.5 0 0 0 5 5L13 15M16 14l1.5-1.5a3.5 3.5 0 0 0-5-5L11 9" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
            const ICON_TEXT = `<svg viewBox="0 0 24 24" fill="none"><path d="M6 4h9l4 4v12H6V4Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M9 12h6M9 16h6M9 8h3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`;
            const ICON_FILE = `<svg viewBox="0 0 24 24" fill="none"><path d="M6 4h9l4 4v12H6V4Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M15 4v4h4" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`;
            const ICON_NONE = `<svg viewBox="0 0 24 24" fill="none"><rect x="4" y="5" width="16" height="14" rx="2" stroke="currentColor" stroke-width="1.6"/></svg>`;

            const fileExt = (first?.fileName || "").split(".").pop()?.toUpperCase().slice(0, 4) || "FILE";

            const thumbHtml = firstIsImage
                ? `<img src="${first.fileData}" alt="">`
                : firstIsLink
                    ? `<div class="admin-material-thumb-icon">${ICON_LINK}<span>링크</span></div>`
                    : firstIsText
                        ? `<div class="admin-material-thumb-icon">${ICON_TEXT}<span>글</span></div>`
                        : first
                            ? `<div class="admin-material-thumb-icon">${ICON_FILE}<span>${escapeHtmlForAdminMaterial(fileExt)}</span></div>`
                            : `<div class="admin-material-thumb-icon">${ICON_NONE}<span>없음</span></div>`;

            const countBadgeHtml = files.length > 1
                ? `<span class="admin-material-count-badge">+${files.length - 1}</span>`
                : "";

            // 등록된 파일들을 종류별로 모아 배지로 보여줘요 (이미지 / 파일 / 링크 / 글)
            const typeLabels = [];
            if (files.some((f) => f.fileType && f.fileType.startsWith("image/"))) typeLabels.push("이미지");
            if (files.some((f) => f.fileData && !(f.fileType && f.fileType.startsWith("image/")))) typeLabels.push("파일");
            if (files.some((f) => f.linkUrl)) typeLabels.push("링크");
            if (files.some((f) => f.textContent && !f.linkUrl && !f.fileData)) typeLabels.push("글");
            const typeBadgeHtml = typeLabels.length
                ? typeLabels.map((t) => `<span class="admin-material-type-badge" data-type="${t}">${t}</span>`).join("")
                : `<span class="admin-material-type-badge" data-type="없음">없음</span>`;

            const descriptionHtml = m.description
                ? escapeHtmlForAdminMaterial(m.description)
                : `<span class="admin-material-desc-empty">설명 없음</span>`;

            item.innerHTML = `
        <div class="admin-material-thumb" ${files.length ? "data-view-btn" : ""}>${thumbHtml}${countBadgeHtml}</div>
        <div class="admin-material-main">
          <p class="admin-material-title">${escapeHtmlForAdminMaterial(m.title)}</p>
          <p class="admin-material-desc">${descriptionHtml}</p>
        </div>
        <div class="admin-material-types">${typeBadgeHtml}</div>
        <p class="admin-material-date">${m.createdAt}</p>
        <div class="admin-material-actions">
          ${files.length ? `<button type="button" class="admin-material-action-btn" data-view-btn><svg viewBox="0 0 24 24" fill="none" width="13" height="13"><path d="M2 12s3.8-7 10-7 10 7 10 7-3.8 7-10 7-10-7-10-7Z" stroke="currentColor" stroke-width="1.6"/><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.6"/></svg>보기</button>` : ""}
          <button type="button" class="admin-material-action-btn" data-edit-btn><svg viewBox="0 0 24 24" fill="none" width="13" height="13"><path d="M4 20l1-4L16 5l3 3L8 19l-4 1Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>수정</button>
          <button type="button" class="admin-material-action-btn admin-material-action-btn--danger" data-delete-btn><svg viewBox="0 0 24 24" fill="none" width="13" height="13"><path d="M5 7h14M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2m2 0-1 13a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1L6 7" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>삭제</button>
        </div>
      `;
            item._materialData = m;
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

function openFileInNewTab(fileUrlOrDataUri) {
    // 이제 fileData 자리엔 대부분 "/uploads/xxx.png" 같은 실제 주소가 들어있어서 그냥 바로 열면 됨.
    // 예전에 base64로 저장해둔 자료만 예외적으로 디코딩해서 열어줌 (하위 호환)
    if (!fileUrlOrDataUri) {
        alert("파일을 여는 데 실패했어요. 다운로드해서 확인해주세요.");
        return;
    }

    if (!fileUrlOrDataUri.startsWith("data:")) {
        window.open(fileUrlOrDataUri, "_blank");
        return;
    }

    try {
        const [header, base64] = fileUrlOrDataUri.split(",");
        const mimeMatch = header.match(/data:(.*?);base64/);
        const mimeType = mimeMatch ? mimeMatch[1] : "application/octet-stream";

        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }

        const blob = new Blob([bytes], { type: mimeType });
        const blobUrl = URL.createObjectURL(blob);
        window.open(blobUrl, "_blank");
    } catch (err) {
        console.error(err);
        alert("파일을 여는 데 실패했어요. 다운로드해서 확인해주세요.");
    }
}

function openTextInNewTab(text, title) {
    const escapeForHtml = (s) =>
        (s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

    const html = `<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>${escapeForHtml(title || "글")}</title>
<style>
  body { font-family: "Noto Sans KR", sans-serif; max-width: 720px; margin: 60px auto; padding: 0 24px 60px; line-height: 1.9; color: #222; white-space: pre-wrap; }
  h1 { font-size: 20px; margin-bottom: 24px; }
</style>
</head>
<body>
<h1>${escapeForHtml(title || "")}</h1>
${escapeForHtml(text)}
</body>
</html>`;

    const blob = new Blob([html], { type: "text/html" });
    const blobUrl = URL.createObjectURL(blob);
    window.open(blobUrl, "_blank");
}

function handleViewMaterial(material) {
    const files = material.files || [];
    if (files.length === 0) return;

    const allImages = files.every((f) => f.fileType && f.fileType.startsWith("image/"));
    if (allImages) {
        openLightbox(files);
        return;
    }

    const first = files[0];
    if (first.linkUrl) {
        window.open(first.linkUrl, "_blank");
    } else if (first.textContent && !first.fileData) {
        openTextInNewTab(first.textContent, material.title);
    } else if (first.fileData) {
        openFileInNewTab(first.fileData);
    }
}

let lightboxFiles = [];
let lightboxIndex = 0;

function openLightbox(files) {
    lightboxFiles = files || [];
    lightboxIndex = 0;
    if (lightboxFiles.length === 0) return;

    const lightbox = document.getElementById("adminLightbox");
    if (!lightbox) return;
    lightbox.classList.add("open");
    lightbox.setAttribute("aria-hidden", "false");
    renderLightboxSlide();
}

function renderLightboxSlide() {
    const file = lightboxFiles[lightboxIndex];
    const img = document.getElementById("adminLightboxImg");
    const countEl = document.getElementById("adminLightboxCount");
    const prevBtn = document.getElementById("adminLightboxPrev");
    const nextBtn = document.getElementById("adminLightboxNext");
    if (!file || !img || !countEl || !prevBtn || !nextBtn) return;

    img.src = file.fileData;
    countEl.textContent = lightboxFiles.length > 1 ? `${lightboxIndex + 1} / ${lightboxFiles.length}` : "";
    prevBtn.hidden = lightboxFiles.length <= 1;
    nextBtn.hidden = lightboxFiles.length <= 1;
}

function showLightboxPrev() {
    if (lightboxFiles.length <= 1) return;
    lightboxIndex = (lightboxIndex - 1 + lightboxFiles.length) % lightboxFiles.length;
    renderLightboxSlide();
}

function showLightboxNext() {
    if (lightboxFiles.length <= 1) return;
    lightboxIndex = (lightboxIndex + 1) % lightboxFiles.length;
    renderLightboxSlide();
}

function closeLightbox() {
    const lightbox = document.getElementById("adminLightbox");
    if (!lightbox) return;
    lightbox.classList.remove("open");
    lightbox.setAttribute("aria-hidden", "true");
}

let editingExistingFiles = [];

function renderExistingFiles() {
    const container = document.getElementById("registerExistingFiles");
    if (!container) return;

    container.innerHTML = "";
    editingExistingFiles.forEach((f, index) => {
        const isImage = f.fileType && f.fileType.startsWith("image/");

        const wrap = document.createElement("div");
        wrap.className = "admin-existing-file-wrap";

        const chip = document.createElement("div");
        chip.className = "admin-existing-file-chip";
        chip.innerHTML = isImage
            ? `<img src="${f.fileData}" alt="">`
            : `<span>${(f.fileName || "파일").slice(0, 6)}</span>`;

        const removeBtn = document.createElement("button");
        removeBtn.type = "button";
        removeBtn.className = "admin-existing-file-remove";
        removeBtn.setAttribute("aria-label", "이 파일 삭제");
        removeBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg>';
        removeBtn.addEventListener("click", () => {
            editingExistingFiles.splice(index, 1);
            renderExistingFiles();
        });

        wrap.appendChild(chip);
        wrap.appendChild(removeBtn);
        container.appendChild(wrap);
    });
}

let currentInviteLanguage = null;
let currentInviteScope = "KWZM";

function openInviteModal(language) {
    currentInviteLanguage = language;
    const modal = document.getElementById("adminInviteModal");
    if (!modal) return;
    document.getElementById("inviteModalTitle").textContent = `학생 초대 · ${LANGUAGE_LABEL[language] || language} (${inviteContentType() === "VIDEO" ? "영상" : "자료"})`;
    document.getElementById("inviteStudentNumberInput").value = "";
    document.getElementById("inviteError").hidden = true;
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    loadInvitedStudents();
}

function closeInviteModal() {
    const modal = document.getElementById("adminInviteModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");

    const bulkField = document.getElementById("bulkInviteField");
    const toggleBtn = document.getElementById("bulkInviteToggleBtn");
    const bulkTextarea = document.getElementById("bulkInviteTextarea");
    const bulkResult = document.getElementById("bulkInviteResult");
    if (bulkField) bulkField.hidden = true;
    if (toggleBtn) toggleBtn.textContent = "여러 명 한 번에 초대하기";
    if (bulkTextarea) bulkTextarea.value = "";
    if (bulkResult) bulkResult.textContent = "";
}

function inviteContentType() {
    return currentInviteScope === "VIDEO" ? "VIDEO" : "MATERIAL";
}

// ---- 공지사항 관리 ----

let adminNoticesCache = [];

async function loadAdminNotices() {
    const list = document.getElementById("adminNoticeList");
    const emptyText = document.getElementById("adminNoticesEmpty");
    if (!list) return;

    try {
        const res = await fetch("/api/admin/notices");
        if (!res.ok) return;
        const notices = await res.json();
        adminNoticesCache = notices;

        list.innerHTML = "";
        if (emptyText) emptyText.hidden = notices.length > 0;

        notices.forEach((n) => {
            const item = document.createElement("div");
            item.className = "admin-notice-item";
            const statusBadge = n.isPublished
                ? ""
                : `<span class="admin-notice-item-scheduled-tag">예약됨 · ${(n.scheduledAt || "").replace("T", " ")}</span>`;
            item.innerHTML = `
        <div class="admin-notice-item-head">
          <p class="admin-notice-item-title">${escapeHtmlForAdmin(n.title)}</p>
          <span class="admin-notice-item-date">${n.createdAt}</span>
        </div>
        ${statusBadge}
        <p class="admin-notice-item-content">${escapeHtmlForAdmin(n.content)}</p>
        <div class="admin-notice-item-actions">
          <button type="button" class="admin-material-action-btn" data-edit-notice-id="${n.id}">수정</button>
          <button type="button" class="admin-material-action-btn admin-material-action-btn--danger" data-delete-notice-id="${n.id}">삭제</button>
        </div>
      `;
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

function openNoticeModal(noticeId) {
    const modal = document.getElementById("noticeModal");
    if (!modal) return;

    const notice = noticeId ? adminNoticesCache.find((n) => String(n.id) === String(noticeId)) : null;

    document.getElementById("noticeModalTitle").textContent = notice ? "공지 수정" : "새 공지 작성";
    document.getElementById("noticeSaveBtn").textContent = notice ? "수정하기" : "등록하기";
    document.getElementById("noticeEditingId").value = notice ? notice.id : "";
    document.getElementById("noticeTitleInput").value = notice ? notice.title : "";
    document.getElementById("noticeContentInput").value = notice ? notice.content : "";
    document.getElementById("noticeScheduledAtInput").value = notice && notice.scheduledAt ? notice.scheduledAt : "";
    document.getElementById("noticeError").hidden = true;

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closeNoticeModal() {
    const modal = document.getElementById("noticeModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

async function submitNotice() {
    const editingId = document.getElementById("noticeEditingId").value;
    const title = document.getElementById("noticeTitleInput").value.trim();
    const content = document.getElementById("noticeContentInput").value.trim();
    const scheduledAt = document.getElementById("noticeScheduledAtInput").value;
    const errorEl = document.getElementById("noticeError");
    const saveBtn = document.getElementById("noticeSaveBtn");
    const isEditing = !!editingId;

    if (!title || !content) {
        errorEl.textContent = "제목과 내용을 모두 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = isEditing ? "수정하는 중..." : "등록하는 중...";

    try {
        const url = isEditing ? `/api/admin/notices/${editingId}` : "/api/admin/notices";
        const method = isEditing ? "PUT" : "POST";
        const res = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ title, content, scheduledAt: scheduledAt || null }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "저장에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        closeNoticeModal();
        loadAdminNotices();
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = isEditing ? "수정하기" : "등록하기";
    }
}

async function deleteNotice(id) {
    if (!confirm("이 공지를 삭제할까요? 되돌릴 수 없어요.")) return;

    try {
        const res = await fetch(`/api/admin/notices/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadAdminNotices();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

async function loadAdminEvents() {
    const list = document.getElementById("adminEventList");
    const emptyText = document.getElementById("adminEventsEmpty");
    if (!list) return;

    try {
        const res = await fetch("/api/admin/events");
        if (!res.ok) return;
        const events = await res.json();

        list.innerHTML = "";
        if (emptyText) emptyText.hidden = events.length > 0;

        events.forEach((e) => {
            const item = document.createElement("div");
            item.className = "admin-notice-item";
            item.innerHTML = `
        <div class="admin-notice-item-head">
          <p class="admin-notice-item-title">${escapeHtmlForAdmin(e.title)}</p>
          <span class="admin-notice-item-date">${e.eventDate}</span>
        </div>
        ${e.content ? `<p class="admin-notice-item-content">${escapeHtmlForAdmin(e.content)}</p>` : ""}
        <div class="admin-notice-item-actions">
          <button type="button" class="admin-material-action-btn admin-material-action-btn--danger" data-delete-event-id="${e.id}">삭제</button>
        </div>
      `;
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

function openEventModal() {
    const modal = document.getElementById("eventModal");
    if (!modal) return;
    document.getElementById("eventTitleInput").value = "";
    document.getElementById("eventDateInput").value = "";
    document.getElementById("eventContentInput").value = "";
    document.getElementById("eventError").hidden = true;
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closeEventModal() {
    const modal = document.getElementById("eventModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

async function submitEvent() {
    const title = document.getElementById("eventTitleInput").value.trim();
    const eventDate = document.getElementById("eventDateInput").value;
    const content = document.getElementById("eventContentInput").value.trim();
    const errorEl = document.getElementById("eventError");
    const saveBtn = document.getElementById("eventSaveBtn");

    if (!title || !eventDate) {
        errorEl.textContent = "제목과 행사 날짜를 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "등록하는 중...";

    try {
        const res = await fetch("/api/admin/events", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ title, content, eventDate }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "등록에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        closeEventModal();
        loadAdminEvents();
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "등록하기";
    }
}

async function deleteEvent(id) {
    if (!confirm("이 이벤트를 삭제할까요? 되돌릴 수 없어요.")) return;

    try {
        const res = await fetch(`/api/admin/events/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadAdminEvents();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

// ---- 회사소개 / 사업소개 / 미래 계획 관리 ----

async function loadCompanyInfoAdmin() {
    try {
        const [companyRes, businessRes] = await Promise.all([
            fetch("/api/company-info/COMPANY"),
            fetch("/api/company-info/BUSINESS"),
        ]);
        if (companyRes.ok) {
            const data = await companyRes.json();
            document.getElementById("companyEyebrowInput").value = data.eyebrow || "";
            document.getElementById("companyTitleInput").value = data.title || "";
            document.getElementById("companyContentInput").value = data.content || "";
        }
        if (businessRes.ok) {
            const data = await businessRes.json();
            document.getElementById("businessEyebrowInput").value = data.eyebrow || "";
            document.getElementById("businessTitleInput").value = data.title || "";
            document.getElementById("businessContentInput").value = data.content || "";
        }
    } catch (err) {
        console.error(err);
    }
}

async function submitCompanyInfo(type) {
    const prefix = type === "COMPANY" ? "company" : "business";
    const eyebrow = document.getElementById(`${prefix}EyebrowInput`).value.trim();
    const title = document.getElementById(`${prefix}TitleInput`).value.trim();
    const content = document.getElementById(`${prefix}ContentInput`).value.trim();
    const errorEl = document.getElementById(`${prefix}InfoError`);
    const saveBtn = document.getElementById(`${prefix}InfoSaveBtn`);

    if (!title || !content) {
        errorEl.textContent = "제목과 내용을 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "저장하는 중...";

    try {
        const res = await fetch(`/api/admin/company-info/${type}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ eyebrow, title, content }),
        });
        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "저장에 실패했어요.";
            errorEl.hidden = false;
            return;
        }
        alert("저장했어요.");
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = type === "COMPANY" ? "회사소개 저장" : "사업소개 저장";
    }
}

let futurePlanItemsCache = [];

async function loadFuturePlanAdmin() {
    try {
        const res = await fetch("/api/future-plan");
        if (!res.ok) return;
        const data = await res.json();

        document.getElementById("futureplanIntroInput").value = data.introText || "";
        document.getElementById("futureplanCtaTitleInput").value = data.ctaTitle || "";
        document.getElementById("futureplanCtaTextInput").value = data.ctaText || "";

        futurePlanItemsCache = data.items || [];
        renderFuturePlanItemsAdmin();
    } catch (err) {
        console.error(err);
    }
}

function renderFuturePlanItemsAdmin() {
    const listEl = document.getElementById("futureplanItemList");
    const emptyEl = document.getElementById("futureplanItemsEmpty");
    if (!listEl) return;

    listEl.innerHTML = "";
    if (emptyEl) emptyEl.hidden = futurePlanItemsCache.length > 0;

    futurePlanItemsCache.forEach((item) => {
        const row = document.createElement("div");
        row.className = "admin-notice-item";
        row.innerHTML = `
      <div class="admin-notice-item-head">
        <p class="admin-notice-item-title">${item.sortOrder}. ${escapeHtmlForAdmin(item.title)}</p>
        <span class="admin-notice-item-date">${escapeHtmlForAdmin(item.tags || "")}</span>
      </div>
      <p class="admin-notice-item-content">${escapeHtmlForAdmin(item.content.slice(0, 80))}${item.content.length > 80 ? "..." : ""}</p>
      <div class="admin-notice-item-actions">
        <button type="button" class="admin-material-action-btn" data-edit-futureplan-item="${item.id}">수정</button>
        <button type="button" class="admin-material-action-btn admin-material-action-btn--danger" data-delete-futureplan-item="${item.id}">삭제</button>
      </div>
    `;
        listEl.appendChild(row);
    });
}

async function submitFuturePlanSection() {
    const introText = document.getElementById("futureplanIntroInput").value.trim();
    const ctaTitle = document.getElementById("futureplanCtaTitleInput").value.trim();
    const ctaText = document.getElementById("futureplanCtaTextInput").value.trim();
    const errorEl = document.getElementById("futureplanSectionError");
    const saveBtn = document.getElementById("futureplanSectionSaveBtn");

    if (!introText) {
        errorEl.textContent = "인트로 문단을 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "저장하는 중...";

    try {
        const res = await fetch("/api/admin/future-plan/section", {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ introText, ctaTitle, ctaText }),
        });
        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "저장에 실패했어요.";
            errorEl.hidden = false;
            return;
        }
        alert("저장했어요.");
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "인트로/마무리 문구 저장";
    }
}

function openFuturePlanItemModal(itemId) {
    const modal = document.getElementById("futureplanItemModal");
    if (!modal) return;
    const item = itemId ? futurePlanItemsCache.find((i) => i.id === Number(itemId)) : null;

    modal.dataset.editingId = item ? item.id : "";
    document.getElementById("futureplanItemModalTitle").textContent = item ? "계획 카드 수정" : "새 계획 카드 등록";
    document.getElementById("futureplanItemOrderInput").value = item ? item.sortOrder : futurePlanItemsCache.length + 1;
    document.getElementById("futureplanItemTitleInput").value = item ? item.title : "";
    document.getElementById("futureplanItemContentInput").value = item ? item.content : "";
    document.getElementById("futureplanItemTagsInput").value = item ? item.tags : "";
    document.getElementById("futureplanItemError").hidden = true;

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closeFuturePlanItemModal() {
    const modal = document.getElementById("futureplanItemModal");
    modal?.classList.remove("open");
    modal?.setAttribute("aria-hidden", "true");
}

async function submitFuturePlanItem() {
    const modal = document.getElementById("futureplanItemModal");
    const editingId = modal?.dataset.editingId;
    const sortOrder = Number(document.getElementById("futureplanItemOrderInput").value) || 1;
    const title = document.getElementById("futureplanItemTitleInput").value.trim();
    const content = document.getElementById("futureplanItemContentInput").value.trim();
    const tags = document.getElementById("futureplanItemTagsInput").value.trim();
    const errorEl = document.getElementById("futureplanItemError");
    const saveBtn = document.getElementById("futureplanItemSaveBtn");

    if (!title || !content) {
        errorEl.textContent = "제목과 내용을 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "저장하는 중...";

    try {
        const url = editingId ? `/api/admin/future-plan/items/${editingId}` : "/api/admin/future-plan/items";
        const method = editingId ? "PUT" : "POST";
        const res = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ sortOrder, title, content, tags }),
        });
        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "저장에 실패했어요.";
            errorEl.hidden = false;
            return;
        }
        closeFuturePlanItemModal();
        loadFuturePlanAdmin();
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "등록하기";
    }
}

async function deleteFuturePlanItem(id) {
    if (!confirm("이 계획 카드를 삭제할까요? 되돌릴 수 없어요.")) return;
    try {
        const res = await fetch(`/api/admin/future-plan/items/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadFuturePlanAdmin();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

// ---- 강의 시간표 관리 ----

let adminTimetableCache = [];
const TIMETABLE_DAY_LABEL = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금", SAT: "토", SUN: "일" };
const TIMETABLE_COLOR_LABEL = { korean: "한국어", computer: "컴퓨터", other: "기타" };

async function loadAdminTimetable() {
    const list = document.getElementById("adminTimetableList");
    const emptyText = document.getElementById("adminTimetableEmpty");
    if (!list) return;

    try {
        const res = await fetch("/api/admin/timetable");
        if (!res.ok) return;
        const entries = await res.json();
        adminTimetableCache = entries;

        list.innerHTML = "";
        if (emptyText) emptyText.hidden = entries.length > 0;

        entries.forEach((entry) => {
            const item = document.createElement("div");
            item.className = "admin-timetable-item";
            item.innerHTML = `
        <span class="admin-timetable-day admin-timetable-day--${entry.colorType}">${TIMETABLE_DAY_LABEL[entry.day] || entry.day}</span>
        <span class="admin-timetable-time">${escapeHtmlForAdmin(entry.startTime)} - ${escapeHtmlForAdmin(entry.endTime)}</span>
        <span class="admin-timetable-course">${escapeHtmlForAdmin(entry.courseName)}</span>
        <span class="admin-timetable-color-tag">${TIMETABLE_COLOR_LABEL[entry.colorType] || entry.colorType}</span>
        <div class="admin-timetable-item-actions">
          <button type="button" class="admin-material-action-btn" data-edit-timetable-id="${entry.id}">수정</button>
          <button type="button" class="admin-material-action-btn admin-material-action-btn--danger" data-delete-timetable-id="${entry.id}">삭제</button>
        </div>
      `;
            list.appendChild(item);
        });

        renderAdminTimetableCalendar(entries);
    } catch (err) {
        console.error(err);
    }
}

const TIMETABLE_CALENDAR_DAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];

function renderAdminTimetableCalendar(entries) {
    const calendar = document.getElementById("adminTimetableCalendar");
    if (!calendar) return;

    calendar.innerHTML = "";
    TIMETABLE_CALENDAR_DAYS.forEach((day) => {
        const dayEntries = entries
            .filter((e) => e.day === day)
            .sort((a, b) => (a.startTime || "").localeCompare(b.startTime || ""));

        const col = document.createElement("div");
        col.className = "admin-timetable-cal-col";
        col.innerHTML = `
      <p class="admin-timetable-cal-day">${TIMETABLE_DAY_LABEL[day] || day}</p>
      <div class="admin-timetable-cal-blocks">
        ${dayEntries.length === 0
            ? `<p class="admin-timetable-cal-empty">-</p>`
            : dayEntries.map((e) => `
              <button type="button" class="admin-timetable-cal-block admin-timetable-cal-block--${e.colorType}" data-edit-timetable-id="${e.id}">
                <span class="admin-timetable-cal-block-time">${escapeHtmlForAdmin(e.startTime)}</span>
                <span class="admin-timetable-cal-block-course">${escapeHtmlForAdmin(e.courseName)}</span>
              </button>
            `).join("")}
      </div>
    `;
        calendar.appendChild(col);
    });
}


function openTimetableModal(entryId) {
    const modal = document.getElementById("timetableModal");
    if (!modal) return;

    const entry = entryId ? adminTimetableCache.find((e) => String(e.id) === String(entryId)) : null;

    document.getElementById("timetableModalTitle").textContent = entry ? "시간표 항목 수정" : "시간표 항목 추가";
    document.getElementById("timetableSaveBtn").textContent = entry ? "수정하기" : "추가하기";
    document.getElementById("timetableEditingId").value = entry ? entry.id : "";
    document.getElementById("timetableDaySelect").value = entry ? entry.day : "MON";
    document.getElementById("timetableStartInput").value = entry ? entry.startTime : "";
    document.getElementById("timetableEndInput").value = entry ? entry.endTime : "";
    document.getElementById("timetableCourseInput").value = entry ? entry.courseName : "";
    const colorRadio = document.querySelector(`input[name="timetableColorType"][value="${entry ? entry.colorType : "korean"}"]`);
    if (colorRadio) colorRadio.checked = true;
    document.getElementById("timetableError").hidden = true;

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closeTimetableModal() {
    const modal = document.getElementById("timetableModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

async function submitTimetableEntry() {
    const editingId = document.getElementById("timetableEditingId").value;
    const day = document.getElementById("timetableDaySelect").value;
    const startTime = document.getElementById("timetableStartInput").value.trim();
    const endTime = document.getElementById("timetableEndInput").value.trim();
    const courseName = document.getElementById("timetableCourseInput").value.trim();
    const colorType = document.querySelector('input[name="timetableColorType"]:checked')?.value;
    const errorEl = document.getElementById("timetableError");
    const saveBtn = document.getElementById("timetableSaveBtn");
    const isEditing = !!editingId;

    if (!startTime || !endTime || !courseName) {
        errorEl.textContent = "시간과 과목명을 모두 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = isEditing ? "수정하는 중..." : "추가하는 중...";

    try {
        const url = isEditing ? `/api/admin/timetable/${editingId}` : "/api/admin/timetable";
        const method = isEditing ? "PUT" : "POST";
        const res = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ day, startTime, endTime, courseName, colorType }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "저장에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        closeTimetableModal();
        loadAdminTimetable();
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = isEditing ? "수정하기" : "추가하기";
    }
}

async function deleteTimetableEntry(id) {
    if (!confirm("이 시간표 항목을 삭제할까요?")) return;

    try {
        const res = await fetch(`/api/admin/timetable/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadAdminTimetable();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

// ---- FAQ 관리 ----

let adminFaqsCache = [];

async function loadAdminFaqs() {
    const list = document.getElementById("adminFaqList");
    const emptyText = document.getElementById("adminFaqEmpty");
    if (!list) return;

    try {
        const res = await fetch("/api/admin/faqs");
        if (!res.ok) return;
        const faqs = await res.json();
        adminFaqsCache = faqs;

        list.innerHTML = "";
        if (emptyText) emptyText.hidden = faqs.length > 0;

        faqs.forEach((faq) => {
            const item = document.createElement("div");
            item.className = "admin-faq-item";
            item.innerHTML = `
        <p class="admin-faq-item-question">${escapeHtmlForAdmin(faq.question)}</p>
        <p class="admin-faq-item-answer">${escapeHtmlForAdmin(faq.answer)}</p>
        <div class="admin-faq-item-actions">
          <button type="button" class="admin-material-action-btn" data-edit-faq-id="${faq.id}">수정</button>
          <button type="button" class="admin-material-action-btn admin-material-action-btn--danger" data-delete-faq-id="${faq.id}">삭제</button>
        </div>
      `;
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

function openFaqModal(faqId) {
    const modal = document.getElementById("faqModal");
    if (!modal) return;

    const faq = faqId ? adminFaqsCache.find((f) => String(f.id) === String(faqId)) : null;

    document.getElementById("faqModalTitle").textContent = faq ? "질문 수정" : "질문 추가";
    document.getElementById("faqSaveBtn").textContent = faq ? "수정하기" : "추가하기";
    document.getElementById("faqEditingId").value = faq ? faq.id : "";
    document.getElementById("faqQuestionInput").value = faq ? faq.question : "";
    document.getElementById("faqAnswerInput").value = faq ? faq.answer : "";
    document.getElementById("faqError").hidden = true;

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closeFaqModal() {
    const modal = document.getElementById("faqModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

async function submitFaq() {
    const editingId = document.getElementById("faqEditingId").value;
    const question = document.getElementById("faqQuestionInput").value.trim();
    const answer = document.getElementById("faqAnswerInput").value.trim();
    const errorEl = document.getElementById("faqError");
    const saveBtn = document.getElementById("faqSaveBtn");
    const isEditing = !!editingId;

    if (!question || !answer) {
        errorEl.textContent = "질문과 답변을 모두 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = isEditing ? "수정하는 중..." : "추가하는 중...";

    try {
        const url = isEditing ? `/api/admin/faqs/${editingId}` : "/api/admin/faqs";
        const method = isEditing ? "PUT" : "POST";
        const res = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ question, answer }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "저장에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        closeFaqModal();
        loadAdminFaqs();
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = isEditing ? "수정하기" : "추가하기";
    }
}

async function deleteFaq(id) {
    if (!confirm("이 질문을 삭제할까요?")) return;

    try {
        const res = await fetch(`/api/admin/faqs/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadAdminFaqs();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

// ---- 출석부 (관리자 전용) ----

const ATTENDANCE_STATUS_LABEL = { PRESENT: "출석", ABSENT: "결석", MAKEUP: "보강" };

function openAttendanceModal(applicationId, courseName) {
    const modal = document.getElementById("attendanceModal");
    if (!modal) return;

    document.getElementById("attendanceApplicationId").value = applicationId;
    document.getElementById("attendanceModalTitle").textContent = `출석부 · ${courseName}`;
    document.getElementById("attendanceDateInput").value = new Date().toISOString().slice(0, 10);
    document.getElementById("attendanceStatusSelect").value = "PRESENT";
    document.getElementById("attendanceNoteInput").value = "";
    document.getElementById("attendanceError").hidden = true;

    const lessonDateInput = document.getElementById("lessonNoteDateInput");
    if (lessonDateInput) lessonDateInput.value = new Date().toISOString().slice(0, 10);
    const lessonContentInput = document.getElementById("lessonNoteContentInput");
    if (lessonContentInput) lessonContentInput.value = "";
    const lessonError = document.getElementById("lessonNoteError");
    if (lessonError) lessonError.hidden = true;

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    loadAttendanceForModal(applicationId);
    loadLessonNotes(applicationId);

    const levelInput = document.getElementById("levelRecordLevelInput");
    if (levelInput) levelInput.value = "";
    const levelDateInput = document.getElementById("levelRecordDateInput");
    if (levelDateInput) levelDateInput.value = new Date().toISOString().slice(0, 10);
    const levelNoteInput = document.getElementById("levelRecordNoteInput");
    if (levelNoteInput) levelNoteInput.value = "";
    const levelError = document.getElementById("levelRecordError");
    if (levelError) levelError.hidden = true;
    loadLevelRecords(applicationId);

    const assignmentTitleInput = document.getElementById("assignmentTitleInput");
    if (assignmentTitleInput) assignmentTitleInput.value = "";
    const assignmentDescInput = document.getElementById("assignmentDescInput");
    if (assignmentDescInput) assignmentDescInput.value = "";
    const assignmentDueDateInput = document.getElementById("assignmentDueDateInput");
    if (assignmentDueDateInput) assignmentDueDateInput.value = "";
    const assignmentError = document.getElementById("assignmentError");
    if (assignmentError) assignmentError.hidden = true;
    loadAssignmentsForModal(applicationId);
    loadClassChangeRequestsForModal(applicationId);
}

function closeAttendanceModal() {
    const modal = document.getElementById("attendanceModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

const CLASSCHANGE_STATUS_LABEL = { PENDING: "대기중", APPROVED: "승인됨", REJECTED: "거절됨" };
const CLASSCHANGE_TYPE_LABEL = { CANCEL: "취소", RESCHEDULE: "변경" };

async function loadClassChangeRequestsForModal(applicationId) {
    const listEl = document.getElementById("classChangeList");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/class-change-requests`);
        if (!res.ok) {
            listEl.innerHTML = `<p class="admin-note-hint">불러오지 못했어요.</p>`;
            return;
        }
        const requests = await res.json();

        listEl.innerHTML = "";
        if (requests.length === 0) {
            listEl.innerHTML = `<p class="admin-note-hint">아직 보낸 요청이 없어요.</p>`;
            return;
        }

        requests.forEach((r) => {
            const row = document.createElement("div");
            row.className = "admin-lesson-note-row";
            row.innerHTML = `
        <div class="admin-lesson-note-row-head">
          <span class="admin-assignment-row-status admin-assignment-row-status--${r.status === "APPROVED" ? "done" : "pending"}">${CLASSCHANGE_STATUS_LABEL[r.status] || r.status}</span>
        </div>
        <p class="admin-lesson-note-row-content"><strong>${CLASSCHANGE_TYPE_LABEL[r.requestType] || r.requestType}</strong> · ${r.classDate}${r.requestedDate ? ` → ${r.requestedDate}` : ""}</p>
        ${r.reason ? `<p class="admin-lesson-note-row-content">${escapeHtmlForAdmin(r.reason)}</p>` : ""}
        ${r.adminReply ? `<p class="admin-lesson-note-row-content">내 답변: ${escapeHtmlForAdmin(r.adminReply)}</p>` : ""}
        ${r.status === "PENDING" ? `
          <div class="admin-note-footer" style="margin-top: 8px;">
            <span></span>
            <div style="display:flex; gap:6px;">
              <button type="button" class="admin-material-action-btn" data-respond-classchange-id="${r.id}" data-respond-approved="false">거절</button>
              <button type="button" class="admin-note-save-btn" data-respond-classchange-id="${r.id}" data-respond-approved="true">승인</button>
            </div>
          </div>
        ` : ""}
      `;
            listEl.appendChild(row);
        });
    } catch (err) {
        console.error(err);
        listEl.innerHTML = `<p class="admin-note-hint">서버에 연결할 수 없어요.</p>`;
    }
}

async function respondToClassChangeRequest(id, approved) {
    const applicationId = document.getElementById("attendanceApplicationId").value;
    let adminReply = "";
    if (approved) {
        adminReply = prompt("학생한테 남길 메시지가 있으면 적어주세요 (선택, 비워두고 확인 눌러도 돼요).") || "";
    } else {
        if (!confirm("이 요청을 거절할까요?")) return;
        adminReply = prompt("거절 사유를 적어주세요 (선택).") || "";
    }

    try {
        const res = await fetch(`/api/admin/class-change-requests/${id}/respond`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ approved, adminReply }),
        });
        if (!res.ok) {
            alert((await res.text()) || "처리에 실패했어요.");
            return;
        }
        loadClassChangeRequestsForModal(applicationId);
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

async function loadAttendanceForModal(applicationId) {
    const summaryEl = document.getElementById("attendanceSummary");
    const listEl = document.getElementById("attendanceList");
    if (!summaryEl || !listEl) return;

    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/attendance`);
        if (!res.ok) return;
        const data = await res.json();

        const total = data.presentCount + data.absentCount + data.makeupCount;
        const rate = total > 0 ? Math.round((data.presentCount / total) * 100) : 0;
        summaryEl.innerHTML = `
      <div class="admin-attendance-stat"><span class="admin-attendance-stat-value">${data.presentCount}</span><span>출석</span></div>
      <div class="admin-attendance-stat"><span class="admin-attendance-stat-value">${data.absentCount}</span><span>결석</span></div>
      <div class="admin-attendance-stat"><span class="admin-attendance-stat-value">${data.makeupCount}</span><span>보강</span></div>
      <div class="admin-attendance-stat admin-attendance-stat--rate"><span class="admin-attendance-stat-value">${rate}%</span><span>출석률</span></div>
    `;

        listEl.innerHTML = "";
        if (data.records.length === 0) {
            listEl.innerHTML = `<p class="admin-note-hint">아직 기록이 없어요.</p>`;
            return;
        }
        data.records.forEach((r) => {
            const row = document.createElement("div");
            row.className = "admin-attendance-row";
            row.innerHTML = `
        <span class="admin-attendance-row-date">${r.classDate}</span>
        <span class="admin-attendance-row-status admin-attendance-row-status--${r.status.toLowerCase()}">${ATTENDANCE_STATUS_LABEL[r.status] || r.status}</span>
        <span class="admin-attendance-row-note">${escapeHtmlForAdmin(r.note || "")}</span>
        <button type="button" class="admin-attendance-row-delete" data-delete-attendance-id="${r.id}" aria-label="삭제">
          <svg viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
        </button>
      `;
            listEl.appendChild(row);
        });
    } catch (err) {
        console.error(err);
    }
}

async function loadLevelRecords(applicationId) {
    const listEl = document.getElementById("levelRecordList");
    const currentEl = document.getElementById("adminLevelCurrent");
    const currentValueEl = document.getElementById("adminLevelCurrentValue");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/level-records`);
        if (!res.ok) {
            listEl.innerHTML = `<p class="admin-note-hint">불러오지 못했어요.</p>`;
            return;
        }
        const records = await res.json();

        if (currentEl) {
            if (records.length > 0) {
                currentEl.hidden = false;
                if (currentValueEl) currentValueEl.textContent = records[0].level;
            } else {
                currentEl.hidden = true;
            }
        }

        listEl.innerHTML = "";
        if (records.length === 0) {
            listEl.innerHTML = `<p class="admin-note-hint">아직 기록한 레벨이 없어요.</p>`;
            return;
        }

        records.forEach((r) => {
            const row = document.createElement("div");
            row.className = "admin-lesson-note-row";
            row.innerHTML = `
        <div class="admin-lesson-note-row-head">
          <span class="admin-lesson-note-row-date">${r.recordedDate || "날짜 없음"} · <strong>${escapeHtmlForAdmin(r.level)}</strong></span>
          <button type="button" class="admin-attendance-row-delete" data-delete-level-record-id="${r.id}" aria-label="삭제">
            <svg viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
          </button>
        </div>
        ${r.note ? `<p class="admin-lesson-note-row-content">${escapeHtmlForAdmin(r.note)}</p>` : ""}
      `;
            listEl.appendChild(row);
        });
    } catch (err) {
        console.error(err);
        listEl.innerHTML = `<p class="admin-note-hint">서버에 연결할 수 없어요.</p>`;
    }
}

async function submitLevelRecord() {
    const applicationId = document.getElementById("attendanceApplicationId").value;
    const level = document.getElementById("levelRecordLevelInput").value.trim();
    const recordedDate = document.getElementById("levelRecordDateInput").value;
    const note = document.getElementById("levelRecordNoteInput").value.trim();
    const errorEl = document.getElementById("levelRecordError");
    const saveBtn = document.getElementById("levelRecordSaveBtn");

    if (!level) {
        errorEl.textContent = "레벨을 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "추가하는 중...";

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/level-records`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ level, note, recordedDate: recordedDate || null }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "추가에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        document.getElementById("levelRecordLevelInput").value = "";
        document.getElementById("levelRecordNoteInput").value = "";
        loadLevelRecords(applicationId);
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "기록 추가하기";
    }
}

async function deleteLevelRecord(id) {
    if (!confirm("이 레벨 기록을 삭제할까요?")) return;
    const applicationId = document.getElementById("attendanceApplicationId").value;
    try {
        const res = await fetch(`/api/admin/level-records/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadLevelRecords(applicationId);
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

async function loadLessonNotes(applicationId) {
    const listEl = document.getElementById("lessonNoteList");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/lesson-notes`);
        if (!res.ok) {
            listEl.innerHTML = `<p class="admin-note-hint">불러오지 못했어요.</p>`;
            return;
        }
        const notes = await res.json();

        listEl.innerHTML = "";
        if (notes.length === 0) {
            listEl.innerHTML = `<p class="admin-note-hint">아직 남긴 노트가 없어요.</p>`;
            return;
        }

        notes.forEach((n) => {
            const row = document.createElement("div");
            row.className = "admin-lesson-note-row";
            row.innerHTML = `
        <div class="admin-lesson-note-row-head">
          <span class="admin-lesson-note-row-date">${n.classDate || "날짜 없음"}</span>
          <button type="button" class="admin-attendance-row-delete" data-delete-lesson-note-id="${n.id}" aria-label="삭제">
            <svg viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
          </button>
        </div>
        <p class="admin-lesson-note-row-content">${escapeHtmlForAdmin(n.content)}</p>
      `;
            listEl.appendChild(row);
        });
    } catch (err) {
        console.error(err);
        listEl.innerHTML = `<p class="admin-note-hint">서버에 연결할 수 없어요.</p>`;
    }
}

async function submitLessonNote() {
    const applicationId = document.getElementById("attendanceApplicationId").value;
    const classDate = document.getElementById("lessonNoteDateInput").value;
    const content = document.getElementById("lessonNoteContentInput").value.trim();
    const errorEl = document.getElementById("lessonNoteError");
    const saveBtn = document.getElementById("lessonNoteSaveBtn");

    if (!content) {
        errorEl.textContent = "수업 내용을 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "추가하는 중...";

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/lesson-notes`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ classDate: classDate || null, content }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "추가에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        document.getElementById("lessonNoteContentInput").value = "";
        loadLessonNotes(applicationId);
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "노트 추가하기";
    }
}

async function deleteLessonNote(id) {
    if (!confirm("이 노트를 삭제할까요?")) return;
    const applicationId = document.getElementById("attendanceApplicationId").value;
    try {
        const res = await fetch(`/api/admin/lesson-notes/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadLessonNotes(applicationId);
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

async function loadAssignmentsForModal(applicationId) {
    const listEl = document.getElementById("assignmentList");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/assignments`);
        if (!res.ok) {
            listEl.innerHTML = `<p class="admin-note-hint">불러오지 못했어요.</p>`;
            return;
        }
        const assignments = await res.json();

        listEl.innerHTML = "";
        if (assignments.length === 0) {
            listEl.innerHTML = `<p class="admin-note-hint">아직 낸 숙제가 없어요.</p>`;
            return;
        }

        assignments.forEach((a) => {
            const row = document.createElement("div");
            row.className = "admin-lesson-note-row";
            row.innerHTML = `
        <div class="admin-lesson-note-row-head">
          <span class="admin-assignment-row-status admin-assignment-row-status--${a.completed ? "done" : "pending"}">${a.completed ? "완료" : "진행중"}</span>
          <button type="button" class="admin-attendance-row-delete" data-delete-assignment-id="${a.id}" aria-label="삭제">
            <svg viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
          </button>
        </div>
        <p class="admin-lesson-note-row-content"><strong>${escapeHtmlForAdmin(a.title)}</strong>${a.dueDate ? ` · 기한 ${a.dueDate}` : ""}</p>
        ${a.description ? `<p class="admin-lesson-note-row-content">${escapeHtmlForAdmin(a.description)}</p>` : ""}
      `;
            listEl.appendChild(row);
        });
    } catch (err) {
        console.error(err);
        listEl.innerHTML = `<p class="admin-note-hint">서버에 연결할 수 없어요.</p>`;
    }
}

async function submitAssignment() {
    const applicationId = document.getElementById("attendanceApplicationId").value;
    const title = document.getElementById("assignmentTitleInput").value.trim();
    const description = document.getElementById("assignmentDescInput").value.trim();
    const dueDate = document.getElementById("assignmentDueDateInput").value;
    const errorEl = document.getElementById("assignmentError");
    const saveBtn = document.getElementById("assignmentSaveBtn");

    if (!title) {
        errorEl.textContent = "숙제 제목을 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "내는 중...";

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/assignments`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ title, description: description || null, dueDate: dueDate || null }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "실패했어요.";
            errorEl.hidden = false;
            return;
        }

        document.getElementById("assignmentTitleInput").value = "";
        document.getElementById("assignmentDescInput").value = "";
        document.getElementById("assignmentDueDateInput").value = "";
        loadAssignmentsForModal(applicationId);
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "숙제 내주기";
    }
}

async function deleteAssignmentAdmin(id) {
    if (!confirm("이 숙제를 삭제할까요?")) return;
    const applicationId = document.getElementById("attendanceApplicationId").value;
    try {
        const res = await fetch(`/api/admin/assignments/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadAssignmentsForModal(applicationId);
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

async function submitAttendanceRecord() {
    const applicationId = document.getElementById("attendanceApplicationId").value;
    const classDate = document.getElementById("attendanceDateInput").value;
    const status = document.getElementById("attendanceStatusSelect").value;
    const note = document.getElementById("attendanceNoteInput").value.trim();
    const errorEl = document.getElementById("attendanceError");
    const saveBtn = document.getElementById("attendanceSaveBtn");

    if (!classDate) {
        errorEl.textContent = "날짜를 선택해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "추가하는 중...";

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/attendance`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ classDate, status, note }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "저장에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        document.getElementById("attendanceNoteInput").value = "";
        loadAttendanceForModal(applicationId);
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "추가하기";
    }
}

async function deleteAttendanceRecordAdmin(recordId) {
    if (!confirm("이 출석 기록을 삭제할까요?")) return;

    const applicationId = document.getElementById("attendanceApplicationId").value;
    try {
        const res = await fetch(`/api/admin/attendance/${recordId}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadAttendanceForModal(applicationId);
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

const SENT_FILE_CATEGORY_LABEL = { CERTIFICATE: "자격증", EXAM: "시험 자료" };

function openSendFileModal(applicationId, courseName) {
    const modal = document.getElementById("sendFileModal");
    if (!modal) return;

    document.getElementById("sendFileApplicationId").value = applicationId;
    document.getElementById("sendFileModalTitle").textContent = `파일 보내기 · ${courseName}`;
    document.getElementById("sendFileInput").value = "";
    const categoryRadio = document.querySelector('input[name="sendFileCategory"][value="CERTIFICATE"]');
    if (categoryRadio) categoryRadio.checked = true;
    document.getElementById("sendFileError").hidden = true;

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    loadSentFiles(applicationId);
}

function closeSendFileModal() {
    const modal = document.getElementById("sendFileModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

async function loadSentFiles(applicationId) {
    const listEl = document.getElementById("sentFilesList");
    const emptyEl = document.getElementById("sentFilesEmpty");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/files`);
        if (!res.ok) return;
        const files = await res.json();

        listEl.innerHTML = "";
        if (emptyEl) emptyEl.hidden = files.length > 0;

        files.forEach((f) => {
            const row = document.createElement("div");
            row.className = "admin-sent-file-row";
            row.innerHTML = `
        <span class="admin-sent-file-category">${SENT_FILE_CATEGORY_LABEL[f.category] || f.category}</span>
        <span class="admin-sent-file-name">${escapeHtmlForAdmin(f.fileName)}</span>
        <span class="admin-sent-file-date">${f.createdAt}</span>
        <button type="button" class="admin-attendance-row-delete" data-delete-sent-file-id="${f.id}" aria-label="삭제">
          <svg viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
        </button>
      `;
            listEl.appendChild(row);
        });
    } catch (err) {
        console.error(err);
    }
}

async function submitSendFile() {
    const applicationId = document.getElementById("sendFileApplicationId").value;
    const category = document.querySelector('input[name="sendFileCategory"]:checked')?.value;
    const fileInput = document.getElementById("sendFileInput");
    const errorEl = document.getElementById("sendFileError");
    const submitBtn = document.getElementById("sendFileSubmitBtn");
    const file = fileInput.files[0];

    if (!file) {
        errorEl.textContent = "파일을 선택해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    submitBtn.disabled = true;
    submitBtn.textContent = "보내는 중...";

    try {
        const uploadForm = new FormData();
        uploadForm.append("file", file);
        const uploadRes = await fetch("/api/admin/upload", { method: "POST", body: uploadForm });
        if (!uploadRes.ok) {
            errorEl.textContent = (await uploadRes.text()) || "파일 업로드에 실패했어요.";
            errorEl.hidden = false;
            return;
        }
        const uploaded = await uploadRes.json();

        const res = await fetch(`/api/admin/applications/${applicationId}/files`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ category, fileName: file.name, fileData: uploaded.url }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "전송에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        fileInput.value = "";
        loadSentFiles(applicationId);
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = "보내기";
    }
}

async function deleteSentFile(fileId) {
    if (!confirm("이 파일을 삭제할까요?")) return;

    const applicationId = document.getElementById("sendFileApplicationId").value;
    try {
        const res = await fetch(`/api/admin/files/${fileId}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadSentFiles(applicationId);
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

// ---- 오늘 출석 체크 ----

const ATTENDANCE_TODAY_STATUS_LABEL = { PRESENT: "출석", LATE: "지각", ABSENT: "결석", MAKEUP: "보강" };

function getAttendanceTodayDate() {
    const input = document.getElementById("adminCheckinDockDateInput");
    if (!input) return new Date().toISOString().slice(0, 10);
    if (!input.value) input.value = new Date().toISOString().slice(0, 10);
    return input.value;
}

let adminCalendarViewYear = null;
let adminCalendarViewMonth = null; // 0-11

function renderAdminCheckinCalendar() {
    const titleEl = document.getElementById("adminCheckinCalendarTitle");
    const gridEl = document.getElementById("adminCheckinCalendarGrid");
    if (!titleEl || !gridEl) return;

    const selectedDate = getAttendanceTodayDate(); // "yyyy-mm-dd"
    if (adminCalendarViewYear === null) {
        const [y, m] = selectedDate.split("-").map(Number);
        adminCalendarViewYear = y;
        adminCalendarViewMonth = m - 1;
    }

    const year = adminCalendarViewYear;
    const month = adminCalendarViewMonth;
    titleEl.textContent = `${year}년 ${month + 1}월`;

    const todayStr = new Date().toISOString().slice(0, 10);
    const firstDayOfWeek = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const dowKo = ["일", "월", "화", "수", "목", "금", "토"];

    const dowHtml = dowKo.map((d) => `<span class="admin-checkin-dock-calendar-dow">${d}</span>`).join("");
    const blanksHtml = Array.from({ length: firstDayOfWeek }, () => `<span class="admin-checkin-dock-calendar-day is-blank"></span>`).join("");
    const daysHtml = Array.from({ length: daysInMonth }, (_, i) => {
        const d = i + 1;
        const dateStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
        const classes = ["admin-checkin-dock-calendar-day"];
        if (dateStr === selectedDate) classes.push("is-selected");
        if (dateStr === todayStr) classes.push("is-today");
        return `<button type="button" class="${classes.join(" ")}" data-calendar-date="${dateStr}">${d}</button>`;
    }).join("");

    gridEl.innerHTML = dowHtml + blanksHtml + daysHtml;
}

async function loadAttendanceToday() {
    renderAdminCheckinCalendar();
    const list = document.getElementById("adminCheckinDockList");
    const emptyEl = document.getElementById("adminCheckinDockEmpty");
    if (!list) return;
    const date = getAttendanceTodayDate();
    list.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/attendance/today?date=${date}`);
        if (!res.ok) {
            const message = await res.text().catch(() => "");
            list.innerHTML = `<p class="admin-note-hint">불러오지 못했어요. ${escapeHtmlForAdmin(message) || "다시 시도해주세요."}</p>`;
            return;
        }
        const roster = await res.json();

        list.innerHTML = "";
        if (emptyEl) emptyEl.hidden = roster.length > 0;

        roster.forEach((entry) => {
            const row = document.createElement("div");
            row.className = "admin-attendance-today-row";
            const statusLabel = entry.status ? ATTENDANCE_TODAY_STATUS_LABEL[entry.status] || entry.status : "미체크";
            row.innerHTML = `
        <span class="admin-attendance-today-time">${entry.classTime ? entry.classTime.slice(0, 5) : "-"}</span>
        <span class="admin-attendance-today-course">${escapeHtmlForAdmin(entry.courseName)}</span>
        <span class="admin-attendance-today-student"><svg viewBox="0 0 24 24" fill="none" width="13" height="13"><circle cx="12" cy="8" r="3.2" stroke="currentColor" stroke-width="1.6"/><path d="M5.5 19c0-3 3-5.5 6.5-5.5s6.5 2.5 6.5 5.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>${escapeHtmlForAdmin(entry.studentNickname || "")}</span>
        <span class="admin-attendance-today-current admin-attendance-today-current--${entry.status ? entry.status.toLowerCase() : "none"}">
          ${statusLabel}${entry.checkedInByStudent ? " · 학생 체크" : ""}
        </span>
        <div class="admin-attendance-today-actions">
          <button type="button" class="admin-attendance-today-btn admin-attendance-today-btn--present" data-mark-attendance="${entry.applicationId}" data-mark-status="PRESENT">출석</button>
          <button type="button" class="admin-attendance-today-btn admin-attendance-today-btn--late" data-mark-attendance="${entry.applicationId}" data-mark-status="LATE">지각</button>
          <button type="button" class="admin-attendance-today-btn admin-attendance-today-btn--absent" data-mark-attendance="${entry.applicationId}" data-mark-status="ABSENT">결석</button>
          <button type="button" class="admin-attendance-today-btn admin-attendance-today-btn--makeup" data-mark-attendance="${entry.applicationId}" data-mark-status="MAKEUP">보강</button>
        </div>
      `;
            list.appendChild(row);
        });
    } catch (err) {
        console.error(err);
        list.innerHTML = `<p class="admin-note-hint">서버에 연결할 수 없어요.</p>`;
    }
}

async function markTodayAttendance(applicationId, status) {
    const date = getAttendanceTodayDate();
    try {
        const res = await fetch(`/api/admin/attendance/today?date=${date}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ applicationId: Number(applicationId), status }),
        });
        if (!res.ok) {
            alert((await res.text()) || "처리에 실패했어요.");
            return;
        }
        loadAttendanceToday();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

async function markRestAbsentToday() {
    if (!confirm("아직 체크 안 된 학생을 전부 결석으로 처리할까요?")) return;
    const date = getAttendanceTodayDate();
    try {
        const res = await fetch(`/api/admin/attendance/today/mark-rest-absent?date=${date}`, { method: "POST" });
        if (!res.ok) {
            alert((await res.text()) || "처리에 실패했어요.");
            return;
        }
        const count = await res.json();
        alert(`${count}명을 결석으로 처리했어요.`);
        loadAttendanceToday();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

let attendanceHistoryRecords = [];
let attendanceHistoryFilter = "ALL";

async function loadAttendanceHistory() {
    const table = document.getElementById("attendanceHistoryTable");
    const emptyEl = document.getElementById("attendanceHistoryEmpty");
    if (!table) return;
    table.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch("/api/admin/attendance/history");
        if (!res.ok) {
            table.innerHTML = `<p class="admin-note-hint">불러오지 못했어요.</p>`;
            return;
        }
        attendanceHistoryRecords = await res.json();
        renderAttendanceHistoryTable();
    } catch (err) {
        console.error(err);
        table.innerHTML = `<p class="admin-note-hint">서버에 연결할 수 없어요.</p>`;
    }
}

function renderAttendanceHistoryTable() {
    const table = document.getElementById("attendanceHistoryTable");
    const emptyEl = document.getElementById("attendanceHistoryEmpty");
    if (!table) return;

    const records = attendanceHistoryFilter === "ALL"
        ? attendanceHistoryRecords
        : attendanceHistoryRecords.filter((r) => r.status === attendanceHistoryFilter
            || (attendanceHistoryFilter === "ABSENT" && r.status === "ABSENT_PENDING"));

    table.innerHTML = "";
    if (emptyEl) emptyEl.hidden = records.length > 0;
    if (records.length === 0) return;

    const header = document.createElement("div");
    header.className = "admin-attendance-history-row admin-attendance-history-row--head";
    header.innerHTML = `
      <span>날짜</span>
      <span>강의</span>
      <span>학생</span>
      <span>상태</span>
    `;
    table.appendChild(header);

    records.forEach((r) => {
        const row = document.createElement("div");
        row.className = "admin-attendance-history-row";
        row.innerHTML = `
        <span class="admin-attendance-history-date">${r.classDate}</span>
        <span class="admin-attendance-history-course">${escapeHtmlForAdmin(r.courseName)}</span>
        <span class="admin-attendance-history-student">${escapeHtmlForAdmin(r.studentUsername)}${r.checkedInByStudent ? " · 학생 체크" : ""}</span>
        <select class="admin-attendance-history-status-select admin-attendance-today-current--${r.status ? r.status.toLowerCase() : "none"}"
                data-history-record-id="${r.id}" data-history-application-id="${r.applicationId}" data-history-date="${r.classDate}">
          ${r.status === "ABSENT_PENDING" ? `<option value="ABSENT_PENDING" selected disabled>결석 예정</option>` : ""}
          <option value="PRESENT" ${r.status === "PRESENT" ? "selected" : ""}>출석</option>
          <option value="LATE" ${r.status === "LATE" ? "selected" : ""}>지각</option>
          <option value="ABSENT" ${r.status === "ABSENT" ? "selected" : ""}>결석</option>
          <option value="MAKEUP" ${r.status === "MAKEUP" ? "selected" : ""}>보강</option>
        </select>
      `;
        table.appendChild(row);
    });
}

async function changeAttendanceHistoryStatus(selectEl) {
    const applicationId = selectEl.dataset.historyApplicationId;
    const date = selectEl.dataset.historyDate;
    const newStatus = selectEl.value;
    selectEl.disabled = true;

    try {
        const res = await fetch(`/api/admin/attendance/today?date=${date}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ applicationId: Number(applicationId), status: newStatus }),
        });
        if (!res.ok) {
            alert((await res.text()) || "변경에 실패했어요.");
            loadAttendanceHistory();
            return;
        }
        const record = attendanceHistoryRecords.find((r) => String(r.applicationId) === applicationId && r.classDate === date);
        if (record) record.status = newStatus;
        selectEl.className = `admin-attendance-history-status-select admin-attendance-today-current--${newStatus.toLowerCase()}`;
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    } finally {
        selectEl.disabled = false;
    }
}



function openScheduleModal(applicationId, courseName, classDays, classTime, enrollmentEndDate) {
    const modal = document.getElementById("scheduleModal");
    if (!modal) return;

    document.getElementById("scheduleApplicationId").value = applicationId;
    document.getElementById("scheduleModalTitle").textContent = `수업 시간 설정 · ${courseName}`;
    document.querySelectorAll('input[name="scheduleDay"]').forEach((cb) => {
        cb.checked = classDays ? classDays.split(",").includes(cb.value) : false;
    });
    document.getElementById("scheduleTimeInput").value = classTime || "";
    document.getElementById("enrollmentEndDateInput").value = enrollmentEndDate || "";
    document.getElementById("scheduleError").hidden = true;

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closeScheduleModal() {
    const modal = document.getElementById("scheduleModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

async function submitSchedule() {
    const applicationId = document.getElementById("scheduleApplicationId").value;
    const days = [...document.querySelectorAll('input[name="scheduleDay"]:checked')].map((cb) => cb.value);
    const time = document.getElementById("scheduleTimeInput").value;
    const enrollmentEndDate = document.getElementById("enrollmentEndDateInput").value;
    const errorEl = document.getElementById("scheduleError");
    const saveBtn = document.getElementById("scheduleSaveBtn");

    if (days.length === 0 || !time) {
        errorEl.textContent = "요일을 하나 이상 고르고, 시간도 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "저장하는 중...";

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/schedule`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ classDays: days.join(","), classTime: time }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "저장에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        const endDateRes = await fetch(`/api/admin/applications/${applicationId}/enrollment-end-date`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ enrollmentEndDate: enrollmentEndDate || null }),
        });

        if (!endDateRes.ok) {
            errorEl.textContent = (await endDateRes.text()) || "수강 종료일 저장에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        closeScheduleModal();
        loadStudentList();
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "저장하기";
    }
}

async function confirmPaymentReceived(applicationId, btn) {
    if (!confirm("입금을 확인하셨나요? 학생에게 확인 알림이 가요.")) return;

    if (btn) btn.disabled = true;
    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/confirm-payment-received`, { method: "POST" });
        if (!res.ok) {
            alert((await res.text()) || "처리에 실패했어요.");
            return;
        }
        loadStudentList();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    } finally {
        if (btn) btn.disabled = false;
    }
}

// ---- 강의 변경 (관리자 전용) ----

function openCourseChangeModal(applicationId) {
    const app = studentApplicationsCache.find((a) => String(a.id) === String(applicationId));
    if (!app) return;

    const modal = document.getElementById("courseChangeModal");
    if (!modal) return;

    document.getElementById("courseChangeApplicationId").value = applicationId;
    const studyTypeRadio = document.querySelector(`input[name="courseChangeStudyType"][value="${app.studyType}"]`);
    if (studyTypeRadio) studyTypeRadio.checked = true;
    const courseSelect = document.getElementById("courseChangeSelect");
    if (courseSelect) courseSelect.value = app.courseName;
    document.getElementById("courseChangeError").hidden = true;

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closeCourseChangeModal() {
    const modal = document.getElementById("courseChangeModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

async function submitCourseChange() {
    const applicationId = document.getElementById("courseChangeApplicationId").value;
    const studyType = document.querySelector('input[name="courseChangeStudyType"]:checked')?.value;
    const courseName = document.getElementById("courseChangeSelect").value;
    const errorEl = document.getElementById("courseChangeError");
    const saveBtn = document.getElementById("courseChangeSaveBtn");

    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "변경하는 중...";

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/course`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ studyType, courseName }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "변경에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        closeCourseChangeModal();
        loadStudentList();
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "변경하기";
    }
}

// ---- 결제 안내 등록 (관리자 전용) ----

function openPaymentInfoModal(applicationId) {
    const app = studentApplicationsCache.find((a) => String(a.id) === String(applicationId));
    if (!app) return;

    const modal = document.getElementById("paymentInfoModal");
    if (!modal) return;

    document.getElementById("paymentInfoApplicationId").value = applicationId;
    document.getElementById("paymentInfoMethodInput").value = app.paymentMethod || adminPaymentDefault || "";
    document.getElementById("paymentInfoAmountInput").value = app.amount || "";
    document.getElementById("paymentInfoReasonInput").value = app.amountReason || "";
    document.getElementById("paymentInfoMaterialInput").value = app.materialGuide || "";
    document.getElementById("paymentInfoClassInput").value = app.classGuide || "";
    document.getElementById("paymentInfoError").hidden = true;

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closePaymentInfoModal() {
    const modal = document.getElementById("paymentInfoModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

async function submitPaymentInfo() {
    const applicationId = document.getElementById("paymentInfoApplicationId").value;
    const paymentMethod = document.getElementById("paymentInfoMethodInput").value.trim();
    const amount = document.getElementById("paymentInfoAmountInput").value.trim();
    const amountReason = document.getElementById("paymentInfoReasonInput").value.trim();
    const materialGuide = document.getElementById("paymentInfoMaterialInput").value.trim();
    const classGuide = document.getElementById("paymentInfoClassInput").value.trim();
    const errorEl = document.getElementById("paymentInfoError");
    const saveBtn = document.getElementById("paymentInfoSaveBtn");

    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "저장 중...";

    try {
        const res = await fetch(`/api/admin/applications/${applicationId}/payment-info`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ paymentMethod, amount, amountReason, materialGuide, classGuide }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "저장에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        closePaymentInfoModal();
        loadStudentList();
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "저장";
    }
}

async function loadInvitedStudents() {
    const listEl = document.getElementById("adminInvitedList");
    const countEl = document.getElementById("inviteCount");
    if (!listEl || !currentInviteLanguage) return;

    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/kwzm-invites?language=${currentInviteLanguage}&type=${inviteContentType()}`);
        if (!res.ok) throw new Error("불러오기 실패");
        const students = await res.json();

        if (countEl) countEl.textContent = `(${students.length}명)`;
        updateInviteCountBadge(students.length);

        if (students.length === 0) {
            listEl.innerHTML = `<p class="admin-note-hint">아직 초대된 학생이 없어요.</p>`;
            return;
        }

        listEl.innerHTML = "";
        students.forEach((s) => {
            const item = document.createElement("div");
            item.className = "admin-invited-item";
            item.innerHTML = `
        <div class="admin-invited-item-info">
          <p class="admin-invited-item-name">${escapeHtmlForAdmin(s.nickname)} · ${escapeHtmlForAdmin(s.courseName)}</p>
          <p class="admin-invited-item-number">${escapeHtmlForAdmin(s.studentNumber)}</p>
        </div>
        <button type="button" class="admin-invited-remove-btn" data-remove-student="${escapeHtmlForAdmin(s.studentNumber)}">제거</button>
      `;
            listEl.appendChild(item);
        });
    } catch (err) {
        console.error(err);
        listEl.innerHTML = `<p class="admin-note-hint">목록을 불러오지 못했어요.</p>`;
    }
}

async function inviteStudentToLanguage() {
    const input = document.getElementById("inviteStudentNumberInput");
    const errorEl = document.getElementById("inviteError");
    const studentNumber = input.value.trim();
    if (!studentNumber) {
        errorEl.textContent = "학생번호를 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;

    try {
        const res = await fetch(`/api/admin/kwzm-invites?language=${currentInviteLanguage}&type=${inviteContentType()}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ studentNumber }),
        });
        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "초대에 실패했어요.";
            errorEl.hidden = false;
            return;
        }
        input.value = "";
        loadInvitedStudents();
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    }
}

function toggleBulkInviteField() {
    const field = document.getElementById("bulkInviteField");
    const toggleBtn = document.getElementById("bulkInviteToggleBtn");
    if (!field || !toggleBtn) return;
    const willOpen = field.hidden;
    field.hidden = !willOpen;
    toggleBtn.textContent = willOpen ? "한 명씩 초대하기" : "여러 명 한 번에 초대하기";
}

// 한 명씩 순서대로 기존 초대 API를 호출해요 (성공/실패를 각각 기록해서 마지막에 요약을 보여줘요)
async function submitBulkInvite() {
    const textarea = document.getElementById("bulkInviteTextarea");
    const resultEl = document.getElementById("bulkInviteResult");
    const submitBtn = document.getElementById("bulkInviteSubmitBtn");
    if (!textarea) return;

    const studentNumbers = textarea.value
        .split(/[\n,]/)
        .map((s) => s.trim())
        .filter((s) => s.length > 0);

    if (studentNumbers.length === 0) {
        resultEl.textContent = "학생번호를 입력해주세요.";
        return;
    }

    submitBtn.disabled = true;
    let successCount = 0;
    const failed = [];

    for (const studentNumber of studentNumbers) {
        try {
            const res = await fetch(`/api/admin/kwzm-invites?language=${currentInviteLanguage}&type=${inviteContentType()}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ studentNumber }),
            });
            if (res.ok) {
                successCount++;
            } else {
                failed.push(studentNumber);
            }
        } catch (err) {
            console.error(err);
            failed.push(studentNumber);
        }
    }

    submitBtn.disabled = false;
    resultEl.textContent = failed.length === 0
        ? `${successCount}명 모두 초대됐어요.`
        : `${successCount}명 초대됨, 실패: ${failed.join(", ")}`;

    if (successCount > 0) {
        textarea.value = failed.join("\n");
        loadInvitedStudents();
    }
}

async function removeInvitedStudent(studentNumber) {
    if (!confirm("이 학생을 초대 목록에서 제거할까요?")) return;
    try {
        const res = await fetch(`/api/admin/kwzm-invites?language=${currentInviteLanguage}&type=${inviteContentType()}&studentNumber=${encodeURIComponent(studentNumber)}`, {
            method: "DELETE",
        });
        if (!res.ok) {
            alert((await res.text()) || "제거에 실패했어요.");
            return;
        }
        loadInvitedStudents();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

async function updateInviteCountBadge(knownCount) {
    const badge = document.getElementById(currentInviteScope === "VIDEO" ? "videoInviteCountBadge" : "adminInviteCountBadge");
    if (!badge) return;
    if (knownCount !== undefined) {
        badge.textContent = knownCount;
        return;
    }
    try {
        const res = await fetch(`/api/admin/kwzm-invites?language=${currentInviteLanguage}&type=${inviteContentType()}`);
        if (!res.ok) return;
        const students = await res.json();
        badge.textContent = students.length;
    } catch (err) {
        console.error(err);
    }
}

function openEditModal(material) {
    openRegisterModal();
    editingMaterialScope = material.scope || "PERSONAL";
    document.getElementById("registerModalTitle").textContent = `자료 수정 (${scopeLabel(editingMaterialScope)})`;
    document.getElementById("registerSubmitBtn").textContent = "수정하기";
    document.getElementById("registerEditingId").value = material.id;
    document.getElementById("registerLanguageSelect").value = material.language;
    document.getElementById("registerTitleInput").value = material.title;
    document.getElementById("registerDescriptionInput").value = material.description || "";
    const categoryRadioName = material.language === "computer" ? "registerComputerCategory" : "registerCategory";
    const categoryRadio = document.querySelector(`input[name="${categoryRadioName}"][value="${material.category}"]`);
    if (categoryRadio) categoryRadio.checked = true;
    document.querySelector(`input[name="registerScope"][value="${editingMaterialScope}"]`).checked = true;
    document.querySelectorAll('input[name="registerScope"]').forEach((r) => (r.disabled = true));
    updateCategoryFieldVisibility();

    const allFiles = material.files || [];
    const existingLink = allFiles.find((f) => f.linkUrl);
    const existingText = allFiles.find((f) => f.textContent && !f.linkUrl && !f.fileData);
    editingExistingFiles = allFiles.filter((f) => f !== existingLink && f !== existingText);

    document.getElementById("registerLinkInput").value = existingLink ? existingLink.linkUrl : "";
    document.getElementById("registerTextInput").value = existingText ? existingText.textContent : "";
    renderExistingFiles();
}

function updateCategoryFieldVisibility() {
    const language = document.getElementById("registerLanguageSelect").value;
    const scope = document.querySelector('input[name="registerScope"]:checked')?.value;
    const isCategoryless = scope === "VIDEO" || scope === "TRIAL";
    const categoryField = document.getElementById("registerCategoryField");
    const computerCategoryField = document.getElementById("registerComputerCategoryField");
    if (categoryField) categoryField.hidden = isCategoryless || language === "other" || language === "computer" || language === "video";
    if (computerCategoryField) computerCategoryField.hidden = isCategoryless || language !== "computer";
}

async function deleteMaterial(id, language, category, scope) {
    if (!confirm("이 자료를 삭제할까요? 되돌릴 수 없어요.")) return;

    try {
        const res = await fetch(`${materialsApiBase(scope)}/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadMaterials(language, category, scope);
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

function openRegisterModal() {
    const modal = document.getElementById("adminRegisterModal");
    if (!modal) return;
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closeRegisterModal() {
    const modal = document.getElementById("adminRegisterModal");
    const errorEl = document.getElementById("registerError");
    const fileInput = document.getElementById("registerFileInput");
    const fileNameEl = document.getElementById("registerFileName");
    const titleInput = document.getElementById("registerTitleInput");
    const descriptionInput = document.getElementById("registerDescriptionInput");
    const linkInput = document.getElementById("registerLinkInput");
    const textInput = document.getElementById("registerTextInput");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    if (errorEl) errorEl.hidden = true;
    if (fileInput) fileInput.value = "";
    if (fileNameEl) fileNameEl.textContent = "";
    if (titleInput) titleInput.value = "";
    if (descriptionInput) descriptionInput.value = "";
    if (linkInput) linkInput.value = "";
    if (textInput) textInput.value = "";
    document.getElementById("registerLanguageSelect").value = "korean";
    updateCategoryFieldVisibility();
    editingExistingFiles = [];
    renderExistingFiles();
    editingMaterialScope = currentMaterialScope;

    document.getElementById("registerModalTitle").textContent = `자료 등록 (${scopeLabel(currentMaterialScope)})`;
    document.getElementById("registerSubmitBtn").textContent = "등록하기";
    document.getElementById("registerEditingId").value = "";
}

async function submitRegisterMaterial() {
    const editingId = document.getElementById("registerEditingId").value;
    const selectedScope = document.querySelector('input[name="registerScope"]:checked')?.value || "PERSONAL";
    const language = document.getElementById("registerLanguageSelect").value;
    const category = (selectedScope === "VIDEO" || selectedScope === "TRIAL")
        ? selectedScope
        : language === "other"
            ? "OTHER"
            : language === "computer"
                ? document.querySelector('input[name="registerComputerCategory"]:checked')?.value
                : document.querySelector('input[name="registerCategory"]:checked')?.value;
    const title = document.getElementById("registerTitleInput").value.trim();
    const description = document.getElementById("registerDescriptionInput").value.trim();
    const fileInput = document.getElementById("registerFileInput");
    const linkValue = document.getElementById("registerLinkInput").value.trim();
    const textValue = document.getElementById("registerTextInput").value.trim();
    const errorEl = document.getElementById("registerError");
    const submitBtn = document.getElementById("registerSubmitBtn");
    const selectedFiles = Array.from(fileInput.files || []);
    const isEditing = !!editingId;

    if (!title) {
        errorEl.textContent = "제목을 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    if (selectedFiles.some((f) => f.size > 20 * 1024 * 1024)) {
        errorEl.textContent = "파일 하나당 용량은 20MB 이하로 올려주세요.";
        errorEl.hidden = false;
        return;
    }
    const keptNonImageCount = editingExistingFiles.filter((f) => !(f.fileType && f.fileType.startsWith("image/"))).length;
    const newNonImageFiles = selectedFiles.filter((f) => !f.type.startsWith("image/"));
    const totalNonImage = keptNonImageCount + newNonImageFiles.length;
    const totalFiles = editingExistingFiles.length + selectedFiles.length;
    if (totalNonImage > 1) {
        errorEl.textContent = "이미지가 아닌 파일은 1개만 등록할 수 있어요.";
        errorEl.hidden = false;
        return;
    }
    if (totalNonImage === 1 && totalFiles > 1) {
        errorEl.textContent = "파일과 이미지를 함께 등록할 수 없어요. 파일은 1개만 따로 등록해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    submitBtn.disabled = true;
    submitBtn.textContent = isEditing ? "수정 중..." : "등록 중...";

    // 파일을 base64로 바꿔서 DB에 그대로 넣는 대신, 서버 디스크에 저장하고
    // 짧은 URL만 돌려받아서 그걸 fileData 자리에 넣음 (화면 쪽 코드는 그대로 써도 됨)
    const uploadFileToServer = async (file) => {
        const formData = new FormData();
        formData.append("file", file);
        const res = await fetch("/api/admin/upload", { method: "POST", body: formData });
        if (!res.ok) {
            throw new Error((await res.text()) || `"${file.name}" 업로드에 실패했어요.`);
        }
        const uploaded = await res.json();
        return { fileName: file.name, fileType: file.type, fileData: uploaded.url, linkUrl: null, textContent: null };
    };

    try {
        const newFiles = await Promise.all(selectedFiles.map(uploadFileToServer));
        const files = [...editingExistingFiles, ...newFiles];

        if (linkValue) {
            files.push({ fileName: null, fileType: null, fileData: null, linkUrl: linkValue, textContent: null });
        }
        if (textValue) {
            files.push({ fileName: null, fileType: null, fileData: null, linkUrl: null, textContent: textValue });
        }

        const submitScope = isEditing ? editingMaterialScope : selectedScope;
        currentMaterialScope = submitScope;

        const url = isEditing ? `${materialsApiBase(editingMaterialScope)}/${editingId}` : `${materialsApiBase(submitScope)}`;
        const method = isEditing ? "PUT" : "POST";
        const res = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ language, category, title, description, files, assignedStudentNumbers: [] }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || (isEditing ? "수정에 실패했어요." : "등록에 실패했어요.");
            errorEl.hidden = false;
            return;
        }

        closeRegisterModal();

        const prefix = prefixForScope(submitScope);
        const activeLangPill = document.querySelector(`#${prefix}LanguagePills .admin-pill.active`);
        if (submitScope === "VIDEO" || submitScope === "TRIAL") {
            if (activeLangPill && activeLangPill.dataset.lang === language) {
                loadMaterials(language, submitScope, submitScope);
            }
            return;
        }
        const activeCatPill = document.querySelector(`#${prefix}CategoryPills .admin-pill.active`);
        const activeCategory = language === "other" ? "OTHER" : activeCatPill?.textContent && Object.entries(categoryLabelSetFor(language)).find(([, v]) => v === activeCatPill.textContent)?.[0];
        if (activeLangPill && activeLangPill.dataset.lang === language && activeCategory === category) {
            loadMaterials(language, category, submitScope);
        }
    } catch (err) {
        console.error(err);
        errorEl.textContent = err.message || "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = isEditing ? "수정하기" : "등록하기";
    }
}

function escapeHtmlForAdmin(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
}

let adminPaymentDefault = "";

async function loadAdminMe() {
    try {
        const res = await fetch("/api/admin/me");
        if (!res.ok) return;
        const data = await res.json();

        document.getElementById("adminMyUsername").textContent = data.username;
        const emailInput = document.getElementById("adminMyEmailInput");
        const phoneInput = document.getElementById("adminMyPhoneInput");
        if (emailInput) emailInput.value = data.email || "";
        if (phoneInput) phoneInput.value = data.phone || "";

        adminPaymentDefault = data.paymentInfo || "";
        const paymentTextarea = document.getElementById("adminPaymentTextarea");
        if (paymentTextarea) paymentTextarea.value = data.paymentInfo || "";
    } catch (err) {
        console.error(err);
    }
}

async function saveAdminInfo() {
    const emailInput = document.getElementById("adminMyEmailInput");
    const phoneInput = document.getElementById("adminMyPhoneInput");
    const errorEl = document.getElementById("adminInfoError");
    const updatedEl = document.getElementById("adminInfoUpdated");
    const saveBtn = document.getElementById("adminInfoSaveBtn");
    if (!emailInput || !phoneInput) return;

    const email = emailInput.value.trim();
    const phone = phoneInput.value.trim();
    if (!email || !phone) {
        errorEl.textContent = "이메일과 전화번호를 모두 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "저장 중...";

    try {
        const res = await fetch("/api/admin/info", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, phone }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "저장에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        if (updatedEl) updatedEl.textContent = "저장됐어요.";
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "저장";
    }
}

async function changeAdminPassword() {
    const currentInput = document.getElementById("adminCurrentPasswordInput");
    const newInput = document.getElementById("adminNewPasswordInput");
    const confirmInput = document.getElementById("adminNewPasswordConfirmInput");
    const errorEl = document.getElementById("adminPasswordError");
    const updatedEl = document.getElementById("adminPasswordUpdated");
    const saveBtn = document.getElementById("adminPasswordSaveBtn");
    if (!currentInput || !newInput || !confirmInput) return;

    const currentPassword = currentInput.value;
    const newPassword = newInput.value;
    const confirmPassword = confirmInput.value;

    if (!currentPassword || !newPassword || !confirmPassword) {
        errorEl.textContent = "모든 항목을 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    if (newPassword.length < 8) {
        errorEl.textContent = "새 비밀번호는 8자 이상이어야 해요.";
        errorEl.hidden = false;
        return;
    }
    if (newPassword !== confirmPassword) {
        errorEl.textContent = "새 비밀번호가 서로 달라요. 다시 확인해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "변경 중...";

    try {
        const res = await fetch("/api/admin/change-password", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ currentPassword, newPassword }),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || "비밀번호 변경에 실패했어요.";
            errorEl.hidden = false;
            return;
        }

        currentInput.value = "";
        newInput.value = "";
        confirmInput.value = "";
        if (updatedEl) updatedEl.textContent = "비밀번호가 변경됐어요.";
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "비밀번호 변경";
    }
}

async function saveAdminPayment() {
    const textarea = document.getElementById("adminPaymentTextarea");
    const updatedEl = document.getElementById("adminPaymentUpdated");
    const saveBtn = document.getElementById("adminPaymentSaveBtn");
    if (!textarea) return;

    saveBtn.disabled = true;
    saveBtn.textContent = "저장 중...";

    try {
        const res = await fetch("/api/admin/payment", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ paymentInfo: textarea.value }),
        });

        if (!res.ok) {
            alert((await res.text()) || "저장에 실패했어요.");
            return;
        }

        updatedEl.textContent = "저장됐어요.";
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "저장";
    }
}

let studentApplicationsCache = [];

function paymentStatusHtml(app) {
    if (!app.hasPaymentInfo) return "";
    if (app.paymentConfirmedByAdmin) {
        return `<span class="admin-payment-status admin-payment-status--done">${ICON_CHECK} 결제 완료</span>
      <a class="admin-receipt-view-btn" href="/api/admin/applications/${app.id}/receipt" target="_blank" rel="noopener">
        <svg viewBox="0 0 24 24" fill="none"><rect x="4" y="3" width="16" height="18" rx="2" stroke="currentColor" stroke-width="1.6"/><path d="M8 8h8M8 12h8M8 16h5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg> 영수증 발급
      </a>`;
    }
    if (app.paymentConfirmedByStudent) {
        const receiptBtn = app.receiptImage
            ? `<button type="button" class="admin-receipt-view-btn" data-view-receipt-id="${app.id}"><svg viewBox="0 0 24 24" fill="none"><rect x="4" y="3" width="16" height="18" rx="2" stroke="currentColor" stroke-width="1.6"/><path d="M8 8h8M8 12h8M8 16h5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg> 영수증 보기</button>`
            : "";
        return `${receiptBtn}<button type="button" class="admin-payment-confirm-btn" data-confirm-payment-id="${app.id}">${ICON_CHECK} 입금 확인하기 (${app.paymentConfirmedByStudentAt || ""})</button>`;
    }
    return `<span class="admin-payment-status admin-payment-status--waiting">학생 입금 대기 중</span>`;
}

async function loadStudentList() {
    const list = document.getElementById("adminStudentList");
    const emptyText = document.getElementById("adminStudentsEmpty");
    if (!list) return;

    const studyTypeLabel = { TOGETHER: "실시간으로 함께 배우기", VIDEO: "언제든 영상으로 배우기" };
    const statusLabel = { PENDING: "승인대기", APPROVED: "승인완료", WITHDRAWN: "퇴원", SUSPENDED: "휴면" };
    const statusClass = { PENDING: "mypage-badge--pending", APPROVED: "mypage-badge--approved", WITHDRAWN: "mypage-badge--withdrawn", SUSPENDED: "mypage-badge--suspended" };

    try {
        const res = await fetch("/api/admin/applications");
        if (!res.ok) return;
        const applications = await res.json();
        studentApplicationsCache = applications;

        list.innerHTML = "";
        emptyText.hidden = applications.length > 0;

        applications.forEach((app) => {
            const item = document.createElement("div");
            item.className = "admin-student-item";
            item.innerHTML = `
        <div class="admin-student-main">
          <p class="admin-student-name">${escapeHtmlForAdmin(app.nickname)} 님</p>
          ${app.memo ? `<p class="admin-student-memo">${escapeHtmlForAdmin(app.memo)}</p>` : ""}
          ${app.classDays ? `<p class="admin-student-schedule">희망: ${app.classDays.split(",").map((d) => TIMETABLE_DAY_LABEL[d] || d).join(",")} ${app.classTime ? app.classTime.slice(0, 5) : ""}</p>` : ""}
        </div>
        <p class="admin-student-email">${app.email ? `<span class="admin-student-email-text">${escapeHtmlForAdmin(app.email)}</span><button type="button" class="admin-copy-btn" data-copy-value="${escapeHtmlForAdmin(app.email)}" aria-label="이메일 복사" title="이메일 복사">${ICON_COPY}</button>` : "-"}</p>
        <p class="admin-student-course">${escapeHtmlForAdmin(app.courseName)}<span>${escapeHtmlForAdmin(studyTypeLabel[app.studyType] || app.studyType)}</span></p>
        <p class="admin-student-contact">${escapeHtmlForAdmin(app.contact)}</p>
        ${app.studentNumber ? `<span class="admin-student-number">${escapeHtmlForAdmin(app.studentNumber)}<button type="button" class="admin-copy-btn" data-copy-value="${escapeHtmlForAdmin(app.studentNumber)}" aria-label="학생번호 복사" title="학생번호 복사">${ICON_COPY}</button></span>` : `<span></span>`}
        <p class="admin-student-date">${app.createdAt}</p>
        <div class="admin-student-status">
          <span class="mypage-badge ${statusClass[app.status] || ""}">${statusLabel[app.status] || app.status}</span>
          ${app.status === "PENDING" ? `<button type="button" class="admin-approve-btn" data-approve-id="${app.id}">승인하기</button>` : ""}
        </div>
        <div class="admin-student-actions-row">
          <button type="button" class="admin-material-action-btn" data-change-course-id="${app.id}">강의 변경</button>
          ${app.status === "APPROVED" ? `<button type="button" class="admin-material-action-btn" data-payment-info-id="${app.id}">${app.hasPaymentInfo ? "결제 안내 수정" : "결제 안내 등록"}</button>` : ""}
          ${app.status === "APPROVED" ? `<button type="button" class="admin-material-action-btn" data-schedule-id="${app.id}" data-schedule-course="${escapeHtmlForAdmin(app.courseName)}" data-schedule-days="${app.classDays || ""}" data-schedule-time="${app.classTime || ""}" data-schedule-enrollment-end="${app.enrollmentEndDate || ""}">${app.classDays ? "수업 시간 확인/수정" : "수업 시간 설정"}</button>` : ""}
          ${app.status === "APPROVED" ? `<button type="button" class="admin-material-action-btn" data-attendance-id="${app.id}" data-attendance-course="${escapeHtmlForAdmin(app.courseName)}">출석부</button>` : ""}
          ${app.status === "APPROVED" ? `<button type="button" class="admin-material-action-btn" data-send-file-id="${app.id}" data-send-file-course="${escapeHtmlForAdmin(app.courseName)}">파일 보내기</button>` : ""}
          ${app.status === "APPROVED" ? `<button type="button" class="admin-material-action-btn admin-material-action-btn--danger" data-set-status-id="${app.id}" data-set-status-value="SUSPENDED">휴면 처리</button>` : ""}
          ${app.status === "APPROVED" ? `<button type="button" class="admin-material-action-btn admin-material-action-btn--danger" data-set-status-id="${app.id}" data-set-status-value="WITHDRAWN">퇴원 처리</button>` : ""}
          ${(app.status === "SUSPENDED" || app.status === "WITHDRAWN") ? `<button type="button" class="admin-material-action-btn" data-set-status-id="${app.id}" data-set-status-value="APPROVED">복구하기</button>` : ""}
          ${paymentStatusHtml(app)}
        </div>
      `;
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

const SURVEY_CATEGORY_LABEL = { contentRating: "강의 내용", teacherRating: "선생님", materialRating: "교재/자료", overallRating: "전반적" };

function renderStarsReadonly(rating) {
    return "★".repeat(rating) + "☆".repeat(5 - rating);
}

let assignmentSubmissionsData = [];
let assignmentSubmissionFilter = "ALL";

async function loadAssignmentSubmissionsAdmin() {
    const listEl = document.getElementById("adminAssignmentSubmissionsList");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch("/api/admin/assignment-submissions");
        if (!res.ok) {
            listEl.innerHTML = `<p class="admin-note-hint">불러오지 못했어요.</p>`;
            return;
        }
        assignmentSubmissionsData = await res.json();
        renderAssignmentSubmissions();
    } catch (err) {
        console.error(err);
        listEl.innerHTML = `<p class="admin-note-hint">서버에 연결할 수 없어요.</p>`;
    }
}

function renderAssignmentSubmissions() {
    const listEl = document.getElementById("adminAssignmentSubmissionsList");
    const emptyEl = document.getElementById("adminAssignmentSubmissionsEmpty");
    const countEl = document.getElementById("assignmentSubmissionCount");
    if (!listEl) return;

    const keyword = (document.getElementById("assignmentSubmissionSearch")?.value || "").trim().toLowerCase();

    let filtered = assignmentSubmissionsData;
    if (assignmentSubmissionFilter === "UNREVIEWED") filtered = filtered.filter((s) => !s.adminComment);
    if (assignmentSubmissionFilter === "REVIEWED") filtered = filtered.filter((s) => !!s.adminComment);
    if (keyword) {
        filtered = filtered.filter((s) =>
            s.studentUsername.toLowerCase().includes(keyword)
            || s.assignmentTitle.toLowerCase().includes(keyword)
            || s.courseName.toLowerCase().includes(keyword)
        );
    }

    if (countEl) {
        const unreviewedCount = assignmentSubmissionsData.filter((s) => !s.adminComment).length;
        countEl.textContent = `전체 ${assignmentSubmissionsData.length}건 · 코멘트 안 남긴 것 ${unreviewedCount}건 · 지금 ${filtered.length}건 표시 중`;
    }

    listEl.innerHTML = "";
    if (emptyEl) emptyEl.hidden = assignmentSubmissionsData.length > 0;

    if (assignmentSubmissionsData.length > 0 && filtered.length === 0) {
        listEl.innerHTML = `<p class="admin-note-hint">조건에 맞는 제출물이 없어요.</p>`;
        return;
    }

    filtered.forEach((s) => {
        const item = document.createElement("div");
        item.className = "admin-voice-item";
        item.innerHTML = `
        <div class="admin-voice-item-head">
          <span class="admin-voice-item-title">${escapeHtmlForAdmin(s.assignmentTitle)}${!s.adminComment ? ` <span class="admin-submission-unread-dot" title="아직 코멘트 안 남김"></span>` : ""}</span>
          <span class="admin-voice-item-meta">${escapeHtmlForAdmin(s.studentUsername)} · ${escapeHtmlForAdmin(s.courseName)} · ${s.submittedAt}${s.attachments && s.attachments.length ? ` · 첨부 ${s.attachments.length}개` : ""}</span>
        </div>
        <button type="button" class="admin-material-action-btn" data-view-assignment-submission="${s.id}">숙제 보기</button>
      `;
        listEl.appendChild(item);
    });
}

let assignmentViewCurrentId = null;
let assignmentViewCurrentImages = [];
let assignmentViewCurrentImageIndex = 0;

function openAssignmentViewModal(id) {
    const s = assignmentSubmissionsData.find((x) => x.id === Number(id));
    if (!s) return;
    assignmentViewCurrentId = s.id;

    document.getElementById("assignmentViewModalTitle").textContent = s.assignmentTitle;
    document.getElementById("assignmentViewMeta").textContent = `${s.studentUsername} · ${s.courseName} · ${s.submittedAt}`;
    document.getElementById("assignmentViewTextAnswer").textContent = s.textAnswer || "글로 쓴 답은 없어요.";
    document.getElementById("assignmentViewCommentInput").value = s.adminComment || "";

    const attachments = s.attachments || [];
    const images = attachments.filter((a) => a.data && a.data.startsWith("data:image"));
    const files = attachments.filter((a) => !a.data || !a.data.startsWith("data:image"));

    assignmentViewCurrentImages = images;
    assignmentViewCurrentImageIndex = 0;

    const lightboxEl = document.getElementById("assignmentViewLightbox");
    if (images.length > 0) {
        lightboxEl.hidden = false;
        renderAssignmentViewLightbox();
    } else {
        lightboxEl.hidden = true;
    }

    const fileLinksEl = document.getElementById("assignmentViewFileLinks");
    fileLinksEl.innerHTML = files
        .map((f) => `<a href="${f.data}" target="_blank" rel="noopener" class="admin-material-action-btn" style="display: inline-block; margin: 4px 6px 0 0;">${escapeHtmlForAdmin(f.name || "파일")} · 새 창에서 열기</a>`)
        .join("");

    const modal = document.getElementById("assignmentViewModal");
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closeAssignmentViewModal() {
    const modal = document.getElementById("assignmentViewModal");
    modal?.classList.remove("open");
    modal?.setAttribute("aria-hidden", "true");
}

function renderAssignmentViewLightbox() {
    const img = document.getElementById("assignmentViewLightboxImg");
    const countEl = document.getElementById("assignmentViewLightboxCount");
    const current = assignmentViewCurrentImages[assignmentViewCurrentImageIndex];
    if (!current) return;
    img.src = current.data;
    countEl.textContent = `${assignmentViewCurrentImageIndex + 1} / ${assignmentViewCurrentImages.length}`;

    const showNav = assignmentViewCurrentImages.length > 1;
    document.getElementById("assignmentViewLightboxPrev").hidden = !showNav;
    document.getElementById("assignmentViewLightboxNext").hidden = !showNav;
}

function navigateAssignmentViewLightbox(direction) {
    const total = assignmentViewCurrentImages.length;
    if (total === 0) return;
    assignmentViewCurrentImageIndex = (assignmentViewCurrentImageIndex + direction + total) % total;
    renderAssignmentViewLightbox();
}

async function saveAssignmentViewComment() {
    if (!assignmentViewCurrentId) return;
    const input = document.getElementById("assignmentViewCommentInput");
    try {
        const commentValue = input.value.trim();
        const res = await fetch(`/api/admin/assignment-submissions/${assignmentViewCurrentId}/comment`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ comment: commentValue }),
        });
        if (!res.ok) {
            alert((await res.text()) || "저장에 실패했어요.");
            return;
        }
        const target = assignmentSubmissionsData.find((s) => s.id === assignmentViewCurrentId);
        if (target) target.adminComment = commentValue;
        renderAssignmentSubmissions();
        alert("코멘트를 저장했어요.");
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

async function loadVoiceSubmissionsAdmin() {
    const listEl = document.getElementById("adminVoiceList");
    const emptyEl = document.getElementById("adminVoiceEmpty");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch("/api/admin/voice-submissions");
        if (!res.ok) {
            listEl.innerHTML = `<p class="admin-note-hint">불러오지 못했어요.</p>`;
            return;
        }
        const submissions = await res.json();

        listEl.innerHTML = "";
        if (emptyEl) emptyEl.hidden = submissions.length > 0;

        submissions.forEach((s) => {
            const item = document.createElement("div");
            item.className = "admin-voice-item";
            item.innerHTML = `
        <div class="admin-voice-item-head">
          <span class="admin-voice-item-title">${escapeHtmlForAdmin(s.title || s.courseName)}</span>
          <span class="admin-voice-item-meta">${escapeHtmlForAdmin(s.studentUsername)} · ${escapeHtmlForAdmin(s.courseName)} · ${s.createdAt}</span>
        </div>
        <audio controls src="${s.audioData}"></audio>
        <div class="admin-voice-comment-row">
          <input type="text" class="admin-voice-comment-input" id="voiceCommentInput-${s.id}" placeholder="코멘트를 입력해주세요" value="${s.adminComment ? escapeHtmlForAdmin(s.adminComment) : ""}">
          <button type="button" class="admin-material-action-btn" data-save-voice-comment="${s.id}">저장</button>
          <button type="button" class="admin-material-action-btn admin-material-action-btn--danger" data-delete-voice-id="${s.id}">삭제</button>
        </div>
      `;
            listEl.appendChild(item);
        });
    } catch (err) {
        console.error(err);
        listEl.innerHTML = `<p class="admin-note-hint">서버에 연결할 수 없어요.</p>`;
    }
}

async function saveVoiceComment(id) {
    const input = document.getElementById(`voiceCommentInput-${id}`);
    if (!input) return;
    try {
        const res = await fetch(`/api/admin/voice-submissions/${id}/comment`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ comment: input.value.trim() }),
        });
        if (!res.ok) {
            alert((await res.text()) || "저장에 실패했어요.");
            return;
        }
        alert("코멘트를 저장했어요.");
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

async function deleteVoiceSubmission(id) {
    if (!confirm("이 녹음을 삭제할까요?")) return;
    try {
        const res = await fetch(`/api/admin/voice-submissions/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadVoiceSubmissionsAdmin();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

async function loadSurveyData() {
    const summaryEl = document.getElementById("adminSurveySummary");
    const listEl = document.getElementById("adminSurveyResponseList");
    const emptyEl = document.getElementById("adminSurveyResponsesEmpty");
    if (!summaryEl || !listEl) return;

    try {
        const [summaryRes, responsesRes] = await Promise.all([
            fetch("/api/admin/survey/summary"),
            fetch("/api/admin/survey/responses"),
        ]);

        if (summaryRes.ok) {
            const s = await summaryRes.json();
            if (s.totalResponses === 0) {
                summaryEl.innerHTML = `<p class="admin-note-hint">아직 응답이 없어서 평균을 낼 수 없어요.</p>`;
            } else {
                summaryEl.innerHTML = `
          <div class="admin-survey-summary-card">
            <span class="admin-survey-summary-label">전체 응답</span>
            <span class="admin-survey-summary-value">${s.totalResponses}건</span>
          </div>
          <div class="admin-survey-summary-card">
            <span class="admin-survey-summary-label">강의 내용</span>
            <span class="admin-survey-summary-value">${s.avgContentRating.toFixed(1)}</span>
          </div>
          <div class="admin-survey-summary-card">
            <span class="admin-survey-summary-label">선생님</span>
            <span class="admin-survey-summary-value">${s.avgTeacherRating.toFixed(1)}</span>
          </div>
          <div class="admin-survey-summary-card">
            <span class="admin-survey-summary-label">교재/자료</span>
            <span class="admin-survey-summary-value">${s.avgMaterialRating.toFixed(1)}</span>
          </div>
          <div class="admin-survey-summary-card">
            <span class="admin-survey-summary-label">전반적</span>
            <span class="admin-survey-summary-value">${s.avgOverallRating.toFixed(1)}</span>
          </div>
        `;
            }
        }

        if (responsesRes.ok) {
            const responses = await responsesRes.json();
            listEl.innerHTML = "";
            if (emptyEl) emptyEl.hidden = responses.length > 0;

            responses.forEach((r) => {
                const item = document.createElement("div");
                item.className = "admin-survey-response-item";
                item.innerHTML = `
          <div class="admin-survey-response-head">
            <span class="admin-survey-response-course">${escapeHtmlForAdmin(r.courseName)}</span>
            <span class="admin-survey-response-student">${r.isAnonymous ? "익명" : escapeHtmlForAdmin(r.studentUsername)} · ${r.createdAt}</span>
          </div>
          <div class="admin-survey-response-ratings">
            <span>강의 내용 ${renderStarsReadonly(r.contentRating)}</span>
            <span>선생님 ${renderStarsReadonly(r.teacherRating)}</span>
            <span>교재/자료 ${renderStarsReadonly(r.materialRating)}</span>
            <span>전반적 ${renderStarsReadonly(r.overallRating)}</span>
          </div>
          ${r.comment ? `<p class="admin-survey-response-comment">${escapeHtmlForAdmin(r.comment)}</p>` : ""}
        `;
                listEl.appendChild(item);
            });
        }
    } catch (err) {
        console.error(err);
    }
}

async function loadAdminQuestions() {
    const list = document.getElementById("adminQuestionsList");
    const emptyText = document.getElementById("adminQuestionsEmpty");
    if (!list) return;

    try {
        const res = await fetch("/api/admin/questions");
        if (!res.ok) return;
        const questions = await res.json();

        list.innerHTML = "";
        emptyText.hidden = questions.length > 0;

        questions.forEach((q) => {
            const answerBoxHtml = q.answerText
                ? `<div class="admin-review-reply-box">
             <p class="admin-review-reply-label">내 답변 · ${q.answeredAt || ""}</p>
             <p class="admin-review-reply-text">${escapeHtmlForAdmin(q.answerText)}</p>
           </div>`
                : "";

            const item = document.createElement("div");
            item.className = "admin-review-item";
            item.innerHTML = `
        <div class="admin-review-head">
          <span class="admin-review-avatar">?</span>
          <div>
            <p class="admin-review-name">익명 질문</p>
          </div>
        </div>
        <p class="admin-review-text">${escapeHtmlForAdmin(q.questionText)}</p>
        <p class="admin-review-date">${q.createdAt}</p>
        ${answerBoxHtml}
        <div class="admin-review-reply-form">
          <textarea class="admin-review-reply-input" data-answer-input rows="1" placeholder="${q.answerText ? "답변 수정하기" : "답변 남기기"}">${q.answerText ? escapeHtmlForAdmin(q.answerText) : ""}</textarea>
          <button type="button" class="admin-review-reply-submit" data-answer-submit="${q.id}">${q.answerText ? "수정" : "등록"}</button>
        </div>
      `;
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

async function loadAdminReviews() {
    const list = document.getElementById("adminReviewList");
    const emptyText = document.getElementById("adminReviewsEmpty");
    if (!list) return;

    try {
        const res = await fetch("/api/admin/reviews");
        if (!res.ok) return;
        const reviews = await res.json();

        list.innerHTML = "";
        emptyText.hidden = reviews.length > 0;

        reviews.forEach((r) => {
            const stars = "★".repeat(r.rating) + "☆".repeat(5 - r.rating);
            const avatarHtml = r.profileImage
                ? `<img src="${r.profileImage}" alt="">`
                : escapeHtmlForAdmin((r.nickname || "?").charAt(0));

            const replyBoxHtml = r.adminReply
                ? `<div class="admin-review-reply-box">
             <p class="admin-review-reply-label">내 답글 · ${r.repliedAt || ""}</p>
             <p class="admin-review-reply-text">${escapeHtmlForAdmin(r.adminReply)}</p>
           </div>`
                : "";

            const item = document.createElement("div");
            item.className = "admin-review-item";
            item.innerHTML = `
        <div class="admin-review-head">
          <span class="admin-review-avatar">${avatarHtml}</span>
          <div>
            <p class="admin-review-name">${escapeHtmlForAdmin(r.nickname)} 님</p>
            <div class="admin-review-stars" aria-hidden="true">${stars}</div>
          </div>
          <span class="admin-review-tag">${escapeHtmlForAdmin(r.courseName)}</span>
        </div>
        <p class="admin-review-text">${escapeHtmlForAdmin(r.content)}</p>
        <p class="admin-review-date">${r.createdAt}</p>
        ${replyBoxHtml}
        <div class="admin-review-reply-form">
          <textarea class="admin-review-reply-input" data-reply-input rows="1" placeholder="${r.adminReply ? "답글 수정하기" : "답글 남기기"}">${r.adminReply ? escapeHtmlForAdmin(r.adminReply) : ""}</textarea>
          <button type="button" class="admin-review-reply-submit" data-reply-submit="${r.id}">${r.adminReply ? "수정" : "등록"}</button>
        </div>
      `;
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

const BOARD_TOPIC_LABEL_ADMIN = { korean: "한국어", japanese: "일본어", thai: "태국어", english: "영어", computer: "컴퓨터" };
const BOARD_CATEGORY_LABEL_ADMIN = { VOCAB: "어휘", GRAMMAR: "문법", WRITING: "쓰기", OTHER: "기타" };

const ICON_LIKE_ADMIN = `<svg class="admin-post-meta-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M7 11v9H4a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1h3Zm0 0 4.5-8a2 2 0 0 1 2.7 2.7L13 9h5.2a2 2 0 0 1 1.98 2.28l-1 7A2 2 0 0 1 17.2 20H10a3 3 0 0 1-3-3v-6Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`;
const ICON_DISLIKE_ADMIN = `<svg class="admin-post-meta-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M17 13V4h3a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1h-3Zm0 0-4.5 8a2 2 0 0 1-2.7-2.7L11 15H5.8a2 2 0 0 1-1.98-2.28l1-7A2 2 0 0 1 6.8 4H14a3 3 0 0 1 3 3v6Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`;
const ICON_COMMENT_ADMIN = `<svg class="admin-post-meta-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 5h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H9l-4.5 3.5a.5.5 0 0 1-.8-.4V17H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`;

async function loadAdminNotifUnreadCount() {
    const badge = document.getElementById("adminNotifBadge");
    if (!badge) return;
    try {
        const res = await fetch("/api/admin/notifications/unread-count");
        if (!res.ok) return;
        const count = await res.json();
        badge.textContent = count > 99 ? "99+" : String(count);
        badge.hidden = count === 0;
    } catch (err) {
        console.error(err);
    }
}

async function toggleAdminNotifPanel() {
    const panel = document.getElementById("adminNotifPanel");
    const btn = document.getElementById("adminNotifBtn");
    if (!panel || !btn) return;

    const willOpen = panel.hidden;
    if (willOpen) {
        const rect = btn.getBoundingClientRect();
        panel.style.top = `${rect.bottom + 10}px`;
        panel.style.right = `${window.innerWidth - rect.right}px`;
    }
    panel.hidden = !willOpen;
    btn.setAttribute("aria-expanded", willOpen ? "true" : "false");
    if (willOpen) await loadAdminNotifList();
}

function closeAdminNotifPanel() {
    const panel = document.getElementById("adminNotifPanel");
    const btn = document.getElementById("adminNotifBtn");
    if (panel) panel.hidden = true;
    if (btn) btn.setAttribute("aria-expanded", "false");
}

async function loadAdminNotifList() {
    const listEl = document.getElementById("adminNotifList");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-notif-hint">불러오는 중...</p>`;

    try {
        const res = await fetch("/api/admin/notifications");
        if (!res.ok) return;
        const notifications = await res.json();

        listEl.innerHTML = "";
        if (notifications.length === 0) {
            listEl.innerHTML = `<p class="admin-notif-hint">아직 알림이 없어요.</p>`;
            return;
        }

        notifications.forEach((n) => {
            const item = document.createElement("button");
            item.type = "button";
            item.className = n.isRead ? "admin-notif-item" : "admin-notif-item is-unread";
            item.innerHTML = `
        <span class="admin-notif-icon">${ADMIN_NOTIF_TYPE_ICON[n.type] || ADMIN_NOTIF_ICON_DEFAULT}</span>
        <span class="admin-notif-body">
          <span class="admin-notif-message">${escapeHtmlForAdmin(n.message)}</span>
          <span class="admin-notif-date">${n.createdAt}</span>
        </span>
      `;
            item.addEventListener("click", () => handleAdminNotifClick(n));
            listEl.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

async function handleAdminNotifClick(notification) {
    if (!notification.isRead) {
        try {
            await fetch(`/api/admin/notifications/${notification.id}/read`, { method: "POST" });
        } catch (err) {
            console.error(err);
        }
    }
    closeAdminNotifPanel();
    loadAdminNotifUnreadCount();
    document.querySelector('[data-main-tab="students"]')?.click();
}

async function markAllAdminNotifsRead() {
    try {
        await fetch("/api/admin/notifications/read-all", { method: "POST" });
        await loadAdminNotifList();
        loadAdminNotifUnreadCount();
    } catch (err) {
        console.error(err);
    }
}

async function sendPaymentRemindersNow() {
    const btn = document.getElementById("sendPaymentReminderBtn");
    if (!btn) return;
    btn.disabled = true;
    const originalText = btn.innerHTML;
    btn.innerHTML = "보내는 중...";

    try {
        const res = await fetch("/api/admin/payment-reminders/send-now", { method: "POST" });
        if (!res.ok) {
            alert((await res.text()) || "실패했어요.");
            return;
        }
        const count = await res.json();
        alert(count > 0 ? `${count}명에게 리마인더를 보냈어요.` : "지금 보낼 대상이 없어요. (이미 확인했거나, 아직 보낼 때가 안 됐어요)");
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
}

let vocabAdminSelectedLanguage = "korean";
let vocabAdminSelectedSetId = null;
let vocabAdminSetsData = [];
let vocabAdminCurrentWords = [];
let vocabRegisterMode = "create";
let vocabRegisterTargetSetId = null;
let vocabRegisterRowCounter = 0;

async function loadVocabAdminSets(language) {
    vocabAdminSelectedLanguage = language;
    vocabAdminSelectedSetId = null;
    document.getElementById("vocabWordManageSection").hidden = true;

    const listEl = document.getElementById("vocabSetList");
    const emptyEl = document.getElementById("vocabSetEmpty");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/vocabulary/sets?language=${language}`);
        if (!res.ok) {
            listEl.innerHTML = `<p class="admin-note-hint">불러오지 못했어요.</p>`;
            return;
        }
        const sets = await res.json();
        vocabAdminSetsData = sets;

        listEl.innerHTML = "";
        if (emptyEl) emptyEl.hidden = sets.length > 0;

        sets.forEach((s) => {
            const card = document.createElement("div");
            card.className = "admin-vocab-set-card";
            card.dataset.setId = s.id;
            card.innerHTML = `
        <div class="admin-vocab-set-card-info" data-select-vocab-set="${s.id}">
          <p class="admin-vocab-set-card-name">${escapeHtmlForAdmin(s.name)}</p>
          <p class="admin-vocab-set-card-meta">${escapeHtmlForAdmin(s.category)} · 단어 ${s.wordCount}개 · 퀴즈 ${s.quizTimeLimitMinutes}분</p>
        </div>
        <button type="button" class="admin-attendance-row-delete" data-delete-vocab-set="${s.id}" aria-label="Part 삭제">
          <svg viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
        </button>
      `;
            listEl.appendChild(card);
        });
    } catch (err) {
        console.error(err);
        listEl.innerHTML = `<p class="admin-note-hint">서버에 연결할 수 없어요.</p>`;
    }
}

function selectVocabSet(setId) {
    vocabAdminSelectedSetId = setId;
    const set = vocabAdminSetsData.find((s) => s.id === setId);
    document.querySelectorAll(".admin-vocab-set-card").forEach((c) => {
        c.classList.toggle("active", c.dataset.setId === String(setId));
    });
    document.getElementById("vocabWordManageTitle").textContent = set ? `${set.name} · 등록된 단어` : "등록된 단어";
    document.getElementById("vocabWordManageSection").hidden = false;
    loadVocabAdminWords(setId);
}

function openVocabRegisterModal(mode) {
    vocabRegisterMode = mode;
    const modal = document.getElementById("vocabRegisterModal");
    const nameInput = document.getElementById("vocabSetNameInput");
    const categoryInput = document.getElementById("vocabSetCategoryInput");
    const timeLimitInput = document.getElementById("vocabSetTimeLimitInput");
    const saveBtn = document.getElementById("vocabSetSaveBtn");
    const heading = modal.querySelector(".admin-vocab-register-head h3");

    document.getElementById("vocabSetError").hidden = true;
    document.getElementById("vocabRegisterRows").innerHTML = "";

    if (mode === "add") {
        const set = vocabAdminSetsData.find((s) => s.id === vocabAdminSelectedSetId);
        vocabRegisterTargetSetId = vocabAdminSelectedSetId;
        heading.textContent = set ? `${set.name}에 단어 추가` : "단어 추가";
        nameInput.value = set ? set.name : "";
        categoryInput.value = set ? set.category : "";
        timeLimitInput.value = set ? set.quizTimeLimitMinutes : 5;
        nameInput.disabled = true;
        categoryInput.disabled = true;
        timeLimitInput.disabled = true;
        saveBtn.textContent = "단어 추가하기";
        addVocabRegisterRow();
    } else {
        vocabRegisterTargetSetId = null;
        heading.textContent = "단어장 등록하기";
        nameInput.value = "";
        categoryInput.value = "";
        timeLimitInput.value = "5";
        nameInput.disabled = false;
        categoryInput.disabled = false;
        timeLimitInput.disabled = false;
        saveBtn.textContent = "등록하기";
        addVocabRegisterRow();
        addVocabRegisterRow();
        addVocabRegisterRow();
    }

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closeVocabRegisterModal() {
    const modal = document.getElementById("vocabRegisterModal");
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

function addVocabRegisterRow() {
    vocabRegisterRowCounter++;
    const rowsEl = document.getElementById("vocabRegisterRows");
    const row = document.createElement("div");
    row.className = "admin-vocab-register-row";
    row.innerHTML = `
      <input type="text" class="vocab-register-word-input" placeholder="단어">
      <input type="text" class="vocab-register-meaning-input" placeholder="뜻">
      <input type="text" class="vocab-register-example-input" placeholder="예문 (선택)">
      <button type="button" class="admin-attendance-row-delete" data-remove-vocab-row aria-label="줄 삭제">
        <svg viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
      </button>
    `;
    rowsEl.appendChild(row);
}

async function submitVocabRegister() {
    const errorEl = document.getElementById("vocabSetError");
    const saveBtn = document.getElementById("vocabSetSaveBtn");
    const rows = [...document.querySelectorAll("#vocabRegisterRows .admin-vocab-register-row")];
    const wordRows = rows
        .map((row) => ({
            word: row.querySelector(".vocab-register-word-input").value.trim(),
            meaning: row.querySelector(".vocab-register-meaning-input").value.trim(),
            example: row.querySelector(".vocab-register-example-input").value.trim(),
        }))
        .filter((r) => r.word && r.meaning);

    if (wordRows.length === 0) {
        errorEl.textContent = "단어를 최소 1개 이상 입력해주세요 (단어, 뜻은 필수예요).";
        errorEl.hidden = false;
        return;
    }

    let setId = vocabRegisterTargetSetId;
    errorEl.hidden = true;
    saveBtn.disabled = true;
    saveBtn.textContent = "등록하는 중...";

    try {
        if (vocabRegisterMode === "create") {
            const name = document.getElementById("vocabSetNameInput").value.trim();
            const category = document.getElementById("vocabSetCategoryInput").value.trim();
            const quizTimeLimitMinutes = Number(document.getElementById("vocabSetTimeLimitInput").value);

            if (!name || !category || !quizTimeLimitMinutes || quizTimeLimitMinutes <= 0) {
                errorEl.textContent = "Part 이름, 종류, 퀴즈 제한시간을 모두 입력해주세요.";
                errorEl.hidden = false;
                return;
            }

            const setRes = await fetch("/api/admin/vocabulary/sets", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ language: vocabAdminSelectedLanguage, name, category, quizTimeLimitMinutes }),
            });
            if (!setRes.ok) {
                errorEl.textContent = (await setRes.text()) || "Part 생성에 실패했어요.";
                errorEl.hidden = false;
                return;
            }
            const newSet = await setRes.json();
            setId = newSet.id;
        }

        for (const wordRow of wordRows) {
            await fetch("/api/admin/vocabulary/words", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ setId, word: wordRow.word, meaning: wordRow.meaning, example: wordRow.example }),
            });
        }

        closeVocabRegisterModal();
        await loadVocabAdminSets(vocabAdminSelectedLanguage);
        if (vocabRegisterMode === "add" && setId) {
            selectVocabSet(setId);
        }
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = vocabRegisterMode === "add" ? "단어 추가하기" : "등록하기";
    }
}

async function deleteVocabSet(setId) {
    if (!confirm("이 Part를 삭제할까요? 안에 있는 단어도 전부 같이 삭제돼요.")) return;
    try {
        const res = await fetch(`/api/admin/vocabulary/sets/${setId}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        if (vocabAdminSelectedSetId === Number(setId)) {
            document.getElementById("vocabWordManageSection").hidden = true;
            vocabAdminSelectedSetId = null;
        }
        loadVocabAdminSets(vocabAdminSelectedLanguage);
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

async function loadVocabAdminWords(setId) {
    const listEl = document.getElementById("vocabAdminWordList");
    const emptyEl = document.getElementById("vocabAdminEmpty");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/vocabulary/sets/${setId}/words`);
        if (!res.ok) {
            listEl.innerHTML = `<p class="admin-note-hint">불러오지 못했어요.</p>`;
            return;
        }
        const words = await res.json();
        vocabAdminCurrentWords = words;

        listEl.innerHTML = "";
        if (emptyEl) emptyEl.hidden = words.length > 0;

        words.forEach((w) => {
            const row = document.createElement("div");
            row.className = "admin-lesson-note-row";
            row.innerHTML = `
        <div class="admin-lesson-note-row-head">
          <span class="admin-lesson-note-row-date"><strong>${escapeHtmlForAdmin(w.word)}</strong> · ${escapeHtmlForAdmin(w.meaning)}</span>
          <button type="button" class="admin-attendance-row-delete" data-delete-vocab-id="${w.id}" aria-label="삭제">
            <svg viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
          </button>
        </div>
        ${w.example ? `<p class="admin-lesson-note-row-content">${escapeHtmlForAdmin(w.example)}</p>` : ""}
      `;
            listEl.appendChild(row);
        });
    } catch (err) {
        console.error(err);
        listEl.innerHTML = `<p class="admin-note-hint">서버에 연결할 수 없어요.</p>`;
    }
}

async function deleteVocabWord(id) {

    if (!confirm("이 단어를 삭제할까요?")) return;
    try {
        const res = await fetch(`/api/admin/vocabulary/words/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadVocabAdminWords(vocabAdminSelectedSetId);
        loadVocabAdminSets(vocabAdminSelectedLanguage);
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

let vocabPreviewQuizQuestions = [];
let vocabPreviewQuizCurrentIndex = 0;
let vocabPreviewQuizTimerInterval = null;
let vocabPreviewQuizSecondsLeft = 0;

function shuffleArrayForAdmin(arr) {
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
}

function startVocabQuizPreview() {
    if (vocabAdminCurrentWords.length < 2) {
        alert("퀴즈 미리보기를 하려면 단어가 2개 이상 필요해요.");
        return;
    }

    const set = vocabAdminSetsData.find((s) => s.id === vocabAdminSelectedSetId);
    const pool = shuffleArrayForAdmin(vocabAdminCurrentWords).slice(0, 10);
    const allMeanings = vocabAdminCurrentWords.map((w) => w.meaning);

    vocabPreviewQuizQuestions = pool.map((w) => {
        const distractors = shuffleArrayForAdmin(allMeanings.filter((m) => m !== w.meaning)).slice(0, 3);
        const options = shuffleArrayForAdmin([w.meaning, ...distractors]);
        return { word: w.word, correctMeaning: w.meaning, options, answered: false, correct: false, selectedOption: null };
    });
    vocabPreviewQuizCurrentIndex = 0;
    vocabPreviewQuizSecondsLeft = (set?.quizTimeLimitMinutes || 5) * 60;

    document.getElementById("vocabPreviewQuizOverlay").hidden = false;
    renderVocabPreviewQuizQuestion();

    clearInterval(vocabPreviewQuizTimerInterval);
    updateVocabPreviewQuizTimerDisplay();
    vocabPreviewQuizTimerInterval = setInterval(() => {
        vocabPreviewQuizSecondsLeft--;
        updateVocabPreviewQuizTimerDisplay();
        if (vocabPreviewQuizSecondsLeft <= 0) {
            clearInterval(vocabPreviewQuizTimerInterval);
        }
    }, 1000);
}

function updateVocabPreviewQuizTimerDisplay() {
    const m = String(Math.max(0, Math.floor(vocabPreviewQuizSecondsLeft / 60))).padStart(2, "0");
    const s = String(Math.max(0, vocabPreviewQuizSecondsLeft % 60)).padStart(2, "0");
    const timerEl = document.getElementById("vocabPreviewQuizTimer");
    if (timerEl) timerEl.textContent = `${m}:${s}`;
}

function renderVocabPreviewQuizQuestion() {
    const q = vocabPreviewQuizQuestions[vocabPreviewQuizCurrentIndex];
    if (!q) return;

    document.getElementById("vocabPreviewQuizProgress").textContent = `${vocabPreviewQuizCurrentIndex + 1} / ${vocabPreviewQuizQuestions.length}`;
    document.getElementById("vocabPreviewQuizWord").textContent = q.word;

    const optionsEl = document.getElementById("vocabPreviewQuizOptions");
    optionsEl.innerHTML = "";
    q.options.forEach((opt) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "vocab-quiz-option";
        btn.textContent = opt;
        if (q.answered) {
            btn.disabled = true;
            if (opt === q.correctMeaning) btn.classList.add("is-correct");
            if (opt === q.selectedOption && !q.correct) btn.classList.add("is-wrong");
        } else {
            btn.addEventListener("click", () => answerVocabPreviewQuizQuestion(opt));
        }
        optionsEl.appendChild(btn);
    });
}

function answerVocabPreviewQuizQuestion(selected) {
    const q = vocabPreviewQuizQuestions[vocabPreviewQuizCurrentIndex];
    if (!q || q.answered) return;

    q.answered = true;
    q.selectedOption = selected;
    q.correct = selected === q.correctMeaning;
    renderVocabPreviewQuizQuestion();

    setTimeout(() => {
        if (vocabPreviewQuizCurrentIndex < vocabPreviewQuizQuestions.length - 1) {
            vocabPreviewQuizCurrentIndex++;
            renderVocabPreviewQuizQuestion();
        } else {
            closeVocabQuizPreview();
        }
    }, 900);
}

function closeVocabQuizPreview() {
    clearInterval(vocabPreviewQuizTimerInterval);
    document.getElementById("vocabPreviewQuizOverlay").hidden = true;
}

async function loadDashboard() {
    try {
        const res = await fetch("/api/admin/dashboard");
        if (!res.ok) return;
        const data = await res.json();

        const pendingEl = document.getElementById("dashboardPendingCount");
        const paymentEl = document.getElementById("dashboardPaymentPendingCount");
        const newThisMonthEl = document.getElementById("dashboardNewThisMonthCount");
        const totalStudentsEl = document.getElementById("dashboardTotalStudentsCount");
        const unreadEl = document.getElementById("dashboardUnreadNotifCount");
        const attendanceRateEl = document.getElementById("dashboardAttendanceRate");
        if (pendingEl) pendingEl.textContent = data.pendingApplicationsCount;
        if (paymentEl) paymentEl.textContent = data.paymentPendingConfirmCount;
        if (newThisMonthEl) newThisMonthEl.textContent = data.newApplicationsThisMonth;
        if (totalStudentsEl) totalStudentsEl.textContent = data.totalStudentsCount;
        if (unreadEl) unreadEl.textContent = data.unreadNotificationsCount;
        if (attendanceRateEl) attendanceRateEl.textContent = `${data.attendanceRateThisMonth}%`;

        const topAbsentList = document.getElementById("dashboardTopAbsentList");
        const topAbsentEmpty = document.getElementById("dashboardTopAbsentEmpty");
        if (topAbsentList) {
            topAbsentList.innerHTML = "";
            const topAbsent = data.topAbsentStudents || [];
            if (topAbsentEmpty) topAbsentEmpty.hidden = topAbsent.length > 0;

            topAbsent.forEach((s, i) => {
                const item = document.createElement("div");
                item.className = "admin-dashboard-recent-item admin-dashboard-recent-item--absent";
                item.innerHTML = `
          <span class="admin-dashboard-absent-rank">${i + 1}</span>
          <span class="admin-dashboard-recent-name">${escapeHtmlForAdmin(s.studentUsername)}</span>
          <span class="admin-dashboard-recent-title">${escapeHtmlForAdmin(s.courseName)}</span>
          <span class="admin-dashboard-absent-count">결석 ${s.absentCount}회</span>
        `;
                topAbsentList.appendChild(item);
            });
        }

        const list = document.getElementById("dashboardRecentList");
        const emptyText = document.getElementById("dashboardRecentEmpty");
        if (!list) return;

        list.innerHTML = "";
        const recentPosts = data.recentPosts || [];
        if (emptyText) emptyText.hidden = recentPosts.length > 0;

        recentPosts.forEach((p) => {
            const item = document.createElement("div");
            item.className = "admin-dashboard-recent-item";
            item.innerHTML = `
        <span class="admin-dashboard-recent-topic">${escapeHtmlForAdmin(BOARD_TOPIC_LABEL_ADMIN[p.topic] || p.topic)}</span>
        <span class="admin-dashboard-recent-title">${escapeHtmlForAdmin(p.title)}</span>
        <span class="admin-dashboard-recent-name">${escapeHtmlForAdmin(p.nickname)}</span>
        <span class="admin-dashboard-recent-date">${p.createdAt}</span>
      `;
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }

    loadTodayTodos();
    loadRevenueReport();
}

async function loadBackups() {
    const listEl = document.getElementById("adminBackupList");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch("/api/admin/backups");
        if (!res.ok) {
            listEl.innerHTML = `<p class="admin-note-hint">불러오지 못했어요.</p>`;
            return;
        }
        const backups = await res.json();

        listEl.innerHTML = "";
        if (backups.length === 0) {
            listEl.innerHTML = `<p class="admin-note-hint">아직 백업된 파일이 없어요.</p>`;
            return;
        }

        backups.forEach((b) => {
            const row = document.createElement("div");
            row.className = "admin-lesson-note-row";
            row.innerHTML = `
        <div class="admin-lesson-note-row-head">
          <span class="admin-lesson-note-row-date">${b.createdAt}</span>
        </div>
        <p class="admin-lesson-note-row-content">${escapeHtmlForAdmin(b.fileName)} · ${b.sizeLabel}</p>
      `;
            listEl.appendChild(row);
        });
    } catch (err) {
        console.error(err);
        listEl.innerHTML = `<p class="admin-note-hint">서버에 연결할 수 없어요.</p>`;
    }
}

async function runBackupNow() {
    const btn = document.getElementById("adminBackupRunBtn");
    if (!btn) return;
    btn.disabled = true;
    btn.textContent = "백업하는 중...";

    try {
        const res = await fetch("/api/admin/backups/run-now", { method: "POST" });
        if (!res.ok) {
            alert((await res.text()) || "백업에 실패했어요.");
            return;
        }
        loadBackups();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    } finally {
        btn.disabled = false;
        btn.textContent = "지금 백업하기";
    }
}

async function loadRevenueReport() {
    const chartEl = document.getElementById("adminRevenueChart");
    if (!chartEl) return;

    try {
        const res = await fetch("/api/admin/dashboard/revenue");
        if (!res.ok) return;
        const months = await res.json();

        const maxRevenue = Math.max(...months.map((m) => m.totalRevenue), 1);

        chartEl.innerHTML = months.map((m) => {
            const [year, month] = m.month.split("-");
            const heightPct = Math.round((m.totalRevenue / maxRevenue) * 100);
            return `
        <div class="admin-revenue-bar-col">
          <span class="admin-revenue-bar-value">${m.totalRevenue.toLocaleString()}원</span>
          <div class="admin-revenue-bar-track">
            <div class="admin-revenue-bar-fill" style="height: ${Math.max(heightPct, 3)}%;"></div>
          </div>
          <span class="admin-revenue-bar-label">${Number(month)}월</span>
          <span class="admin-revenue-bar-count">${m.paymentCount}건</span>
        </div>
      `;
        }).join("");
    } catch (err) {
        console.error(err);
    }
}

const CLASSCHANGE_TODO_TYPE_LABEL = { CANCEL: "취소", RESCHEDULE: "변경" };

async function loadTodayTodos() {
    const classesList = document.getElementById("adminTodoClassesList");
    const paymentsList = document.getElementById("adminTodoPaymentsList");
    const changesList = document.getElementById("adminTodoClassChangesList");
    if (!classesList) return;

    try {
        const res = await fetch("/api/admin/dashboard/today-todos");
        if (!res.ok) return;
        const data = await res.json();

        // 오늘 수업
        const classes = data.todayClasses || [];
        classesList.innerHTML = classes.length === 0
            ? `<p class="admin-todo-empty">오늘 예정된 수업이 없어요.</p>`
            : classes.map((c) => `
        <div class="admin-todo-item">
          <span class="admin-todo-item-time">${c.classTime ? c.classTime.slice(0, 5) : "-"}</span>
          <div class="admin-todo-item-info">
            <p class="admin-todo-item-title">${escapeHtmlForAdmin(c.courseName)}</p>
            <p class="admin-todo-item-sub">${escapeHtmlForAdmin(c.studentNickname || "")}</p>
          </div>
          <span class="admin-todo-item-tag${c.status ? " is-done" : ""}">${c.status ? (ATTENDANCE_TODAY_STATUS_LABEL[c.status] || c.status) : "미체크"}</span>
        </div>
      `).join("");

        // 입금 확인 대기
        const payments = data.pendingPayments || [];
        paymentsList.innerHTML = payments.length === 0
            ? `<p class="admin-todo-empty">입금 확인 대기 중인 학생이 없어요.</p>`
            : payments.map((p) => `
        <div class="admin-todo-item">
          <div class="admin-todo-item-info">
            <p class="admin-todo-item-title">${escapeHtmlForAdmin(p.courseName)}</p>
            <p class="admin-todo-item-sub">${escapeHtmlForAdmin(p.username)}</p>
          </div>
          <button type="button" class="admin-todo-item-btn" data-dashboard-goto="students">확인하기</button>
        </div>
      `).join("");

        // 수업 변경 요청
        const changes = data.pendingClassChanges || [];
        changesList.innerHTML = changes.length === 0
            ? `<p class="admin-todo-empty">대기 중인 요청이 없어요.</p>`
            : changes.map((r) => `
        <div class="admin-todo-item">
          <div class="admin-todo-item-info">
            <p class="admin-todo-item-title">${escapeHtmlForAdmin(r.courseName)} · ${CLASSCHANGE_TODO_TYPE_LABEL[r.requestType] || r.requestType}</p>
            <p class="admin-todo-item-sub">${escapeHtmlForAdmin(r.studentUsername)} · ${r.classDate}${r.requestedDate ? ` → ${r.requestedDate}` : ""}</p>
          </div>
          <button type="button" class="admin-todo-item-btn" data-dashboard-goto="students">확인하기</button>
        </div>
      `).join("");
    } catch (err) {
        console.error(err);
    }
}

async function loadAdminPosts() {
    const list = document.getElementById("adminPostList");
    const emptyText = document.getElementById("adminPostsEmpty");
    if (!list) return;

    try {
        const res = await fetch("/api/admin/posts");
        if (!res.ok) return;
        const posts = await res.json();

        list.innerHTML = "";
        if (emptyText) emptyText.hidden = posts.length > 0;

        posts.forEach((p) => {
            const initial = (p.nickname || "?").charAt(0);
            const badgeHtml = p.category
                ? `<span class="admin-post-badge">${escapeHtmlForAdmin(BOARD_CATEGORY_LABEL_ADMIN[p.category] || p.category)}</span>`
                : "";

            const item = document.createElement("div");
            item.className = "admin-post-item";
            item.innerHTML = `
        <div class="admin-post-head">
          <span class="admin-post-avatar">${escapeHtmlForAdmin(initial)}</span>
          <span class="admin-post-name">${escapeHtmlForAdmin(p.nickname)}</span>
          <span class="admin-post-topic">${escapeHtmlForAdmin(BOARD_TOPIC_LABEL_ADMIN[p.topic] || p.topic)}</span>
          ${badgeHtml}
          <span class="admin-post-date">${p.createdAt}</span>
        </div>
        <p class="admin-post-title">${escapeHtmlForAdmin(p.title)}</p>
        <p class="admin-post-content">${escapeHtmlForAdmin(p.content)}</p>
        <div class="admin-post-meta">
          <span>${ICON_LIKE_ADMIN}${p.likeCount ?? 0}</span>
          <span>${ICON_DISLIKE_ADMIN}${p.dislikeCount ?? 0}</span>
          <button type="button" class="admin-post-comment-toggle">${ICON_COMMENT_ADMIN}<span data-comment-count>${p.commentCount ?? 0}</span></button>
        </div>
        <div class="admin-post-comments" hidden>
          <div class="admin-post-comments-list"></div>
          <div class="admin-post-comment-form">
            <input type="text" class="admin-post-comment-input" placeholder="관리자 댓글을 남겨보세요">
            <button type="button" class="admin-post-comment-submit">등록</button>
          </div>
        </div>
        <div class="admin-post-actions">
          <button type="button" class="admin-material-action-btn admin-material-action-btn--danger" data-delete-post-id="${p.id}">삭제</button>
        </div>
      `;

            const commentsPanel = item.querySelector(".admin-post-comments");
            item.querySelector(".admin-post-comment-toggle").addEventListener("click", () => {
                toggleAdminPostComments(p.id, commentsPanel);
            });

            const commentInput = item.querySelector(".admin-post-comment-input");
            const commentSubmitBtn = item.querySelector(".admin-post-comment-submit");
            const submitNewComment = () => submitAdminPostComment(p.id, item, commentInput, commentSubmitBtn);
            commentSubmitBtn.addEventListener("click", submitNewComment);
            commentInput.addEventListener("keydown", (e) => {
                if (e.key === "Enter") submitNewComment();
            });

            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

async function toggleAdminPostComments(postId, panel) {
    if (!panel) return;
    if (!panel.hidden) {
        panel.hidden = true;
        return;
    }
    panel.hidden = false;
    await loadAdminPostComments(postId, panel);
}

async function loadAdminPostComments(postId, panel) {
    const listEl = panel.querySelector(".admin-post-comments-list");
    if (!listEl) return;
    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/posts/${postId}/comments`);
        if (!res.ok) return;
        const comments = await res.json();

        listEl.innerHTML = "";
        if (comments.length === 0) {
            listEl.innerHTML = `<p class="admin-note-hint">아직 댓글이 없어요.</p>`;
            return;
        }

        const byParent = new Map();
        comments.forEach((c) => {
            const key = c.parentCommentId || "root";
            if (!byParent.has(key)) byParent.set(key, []);
            byParent.get(key).push(c);
        });

        (byParent.get("root") || []).forEach((c) => {
            listEl.appendChild(renderAdminCommentNode(c, byParent, postId, 0));
        });
    } catch (err) {
        console.error(err);
    }
}

function renderAdminCommentNode(comment, byParent, postId, depth) {
    const row = document.createElement("div");
    row.className = comment.isAdmin ? "admin-post-comment-item admin-post-comment-item--admin" : "admin-post-comment-item";
    if (depth > 0) row.classList.add("admin-post-comment-item--reply");

    const adminBadge = comment.isAdmin ? `<span class="admin-post-comment-badge">관리자</span>` : "";

    row.innerHTML = `
      <div class="admin-post-comment-row">
        <span class="admin-post-comment-name">${escapeHtmlForAdmin(comment.nickname)}</span>
        ${adminBadge}
        <span class="admin-post-comment-text">${escapeHtmlForAdmin(comment.content)}</span>
        <span class="admin-post-comment-date">${comment.createdAt}</span>
      </div>
      <div class="admin-post-comment-actions">
        <button type="button" class="admin-post-comment-reaction-btn ${comment.myReaction === "LIKE" ? "is-active" : ""}" data-reaction="LIKE">
          ${ICON_LIKE_ADMIN}<span>${comment.likeCount ?? 0}</span>
        </button>
        <button type="button" class="admin-post-comment-reaction-btn ${comment.myReaction === "DISLIKE" ? "is-active" : ""}" data-reaction="DISLIKE">
          ${ICON_DISLIKE_ADMIN}<span>${comment.dislikeCount ?? 0}</span>
        </button>
        <button type="button" class="admin-post-comment-reply-btn">답글</button>
      </div>
      <div class="admin-post-comment-reply-form" hidden>
        <input type="text" class="admin-post-comment-reply-input" placeholder="${escapeHtmlForAdmin(comment.nickname)}님에게 답글 남기기">
        <button type="button" class="admin-post-comment-reply-submit">등록</button>
      </div>
      <div class="admin-post-comment-replies"></div>
    `;

    const actionsEl = row.querySelector(".admin-post-comment-actions");
    const likeBtn = actionsEl.querySelector('[data-reaction="LIKE"]');
    const dislikeBtn = actionsEl.querySelector('[data-reaction="DISLIKE"]');
    likeBtn.addEventListener("click", () => submitAdminCommentReaction(comment.id, "LIKE", likeBtn, dislikeBtn));
    dislikeBtn.addEventListener("click", () => submitAdminCommentReaction(comment.id, "DISLIKE", likeBtn, dislikeBtn));

    const replyForm = row.querySelector(".admin-post-comment-reply-form");
    row.querySelector(".admin-post-comment-reply-btn").addEventListener("click", () => {
        replyForm.hidden = !replyForm.hidden;
        if (!replyForm.hidden) replyForm.querySelector("input").focus();
    });
    const replyInput = replyForm.querySelector(".admin-post-comment-reply-input");
    const replySubmitBtn = replyForm.querySelector(".admin-post-comment-reply-submit");
    const submitReply = () => {
        const itemEl = row.closest(".admin-post-item");
        submitAdminPostComment(postId, itemEl, replyInput, replySubmitBtn, comment.id);
        replyForm.hidden = true;
    };
    replySubmitBtn.addEventListener("click", submitReply);
    replyInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter") submitReply();
    });

    const repliesEl = row.querySelector(".admin-post-comment-replies");
    (byParent.get(comment.id) || []).forEach((child) => {
        repliesEl.appendChild(renderAdminCommentNode(child, byParent, postId, depth + 1));
    });

    return row;
}

async function submitAdminCommentReaction(commentId, type, likeBtn, dislikeBtn) {
    try {
        const res = await fetch(`/api/admin/comments/${commentId}/reaction`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ type }),
        });
        if (!res.ok) return;
        const updated = await res.json();

        if (likeBtn) {
            likeBtn.querySelector("span:last-child").textContent = updated.likeCount;
            likeBtn.classList.toggle("is-active", updated.myReaction === "LIKE");
        }
        if (dislikeBtn) {
            dislikeBtn.querySelector("span:last-child").textContent = updated.dislikeCount;
            dislikeBtn.classList.toggle("is-active", updated.myReaction === "DISLIKE");
        }
    } catch (err) {
        console.error(err);
    }
}

// parentCommentId가 있으면 그 댓글에 대한 답글로 등록돼요
async function submitAdminPostComment(postId, itemEl, inputEl, submitBtn, parentCommentId) {
    const content = inputEl.value.trim();
    if (!content) return;

    submitBtn.disabled = true;
    try {
        const res = await fetch(`/api/admin/posts/${postId}/comments`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ content, parentCommentId: parentCommentId || null }),
        });
        if (!res.ok) {
            alert((await res.text()) || "댓글 등록에 실패했어요.");
            return;
        }

        inputEl.value = "";
        const panel = itemEl.querySelector(".admin-post-comments");
        await loadAdminPostComments(postId, panel);

        const countEl = itemEl.querySelector("[data-comment-count]");
        if (countEl) countEl.textContent = String(Number(countEl.textContent || 0) + 1);
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    } finally {
        submitBtn.disabled = false;
    }
}

async function deleteAdminPost(id) {
    if (!confirm("이 글을 삭제할까요? 되돌릴 수 없어요.")) return;

    try {
        const res = await fetch(`/api/admin/posts/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        loadAdminPosts();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

document.addEventListener("fragments:loaded", () => {

    document.getElementById("adminBackBtn")?.addEventListener("click", logoutAdmin);

    document.getElementById("adminNotifBtn")?.addEventListener("click", (e) => {
        e.stopPropagation();
        toggleAdminNotifPanel();
    });
    document.getElementById("adminNotifReadAllBtn")?.addEventListener("click", (e) => {
        e.stopPropagation();
        markAllAdminNotifsRead();
    });
    document.getElementById("adminNotifPanel")?.addEventListener("click", (e) => e.stopPropagation());
    document.addEventListener("click", (e) => {
        const wrap = document.getElementById("adminNotifBtn")?.closest(".admin-notif-wrap");
        if (wrap && !wrap.contains(e.target)) closeAdminNotifPanel();
    });


    document.getElementById("toggleAdminLoginPwBtn")?.addEventListener("click", () => {
        const input = document.getElementById("adminLoginPassword");
        const btn = document.getElementById("toggleAdminLoginPwBtn");
        const isVisible = input.type === "text";
        input.type = isVisible ? "password" : "text";
        btn.classList.toggle("is-active", !isVisible);
    });

    let adminAuthMode = "login";

    document.getElementById("adminLoginToggleModeBtn")?.addEventListener("click", () => {
        adminAuthMode = adminAuthMode === "login" ? "signup" : "login";
        const title = document.querySelector(".admin-login-body h3");
        const desc = document.querySelector(".admin-login-body .auth-modal-desc");
        const submitBtn = document.getElementById("adminLoginSubmitBtn");
        const toggleBtn = document.getElementById("adminLoginToggleModeBtn");
        const errorEl = document.getElementById("adminLoginError");
        if (errorEl) errorEl.hidden = true;

        if (adminAuthMode === "signup") {
            if (title) title.textContent = "관리자 계정 만들기";
            if (desc) desc.innerHTML = "이메일, 전화번호, 아이디, 비밀번호(8자 이상)를 입력해주세요.<br>관리자 계정은 딱 하나만 만들 수 있어요.";
            if (submitBtn) submitBtn.textContent = "계정 만들기";
            if (toggleBtn) toggleBtn.innerHTML = `이미 계정이 있으신가요? <span>로그인하기</span>`;
        } else {
            if (title) title.textContent = "나만의 공부 화면";
            if (desc) desc.innerHTML = "관리자만 들어올 수 있어요.<br>이메일, 전화번호, 아이디, 비밀번호를 모두 입력해주세요.";
            if (submitBtn) submitBtn.textContent = "들어가기";
            if (toggleBtn) toggleBtn.innerHTML = `아직 계정이 없으신가요? <span>계정 만들기</span>`;
        }
    });

    document.getElementById("adminLoginSubmitBtn")?.addEventListener("click", async () => {
        const email = document.getElementById("adminLoginEmail").value.trim();
        const phone = document.getElementById("adminLoginPhone").value.trim();
        const username = document.getElementById("adminLoginUsername").value.trim();
        const password = document.getElementById("adminLoginPassword").value;
        const errorEl = document.getElementById("adminLoginError");
        const submitBtn = document.getElementById("adminLoginSubmitBtn");

        if (!email || !phone || !username || !password) {
            errorEl.textContent = "모든 항목을 입력해주세요.";
            errorEl.hidden = false;
            return;
        }
        errorEl.hidden = true;
        submitBtn.disabled = true;

        if (adminAuthMode === "signup") {
            submitBtn.textContent = "만드는 중...";
            try {
                const signupRes = await fetch("/api/admin/signup", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email, phone, username, password }),
                });
                if (!signupRes.ok) {
                    errorEl.textContent = (await signupRes.text()) || "계정 만들기에 실패했어요.";
                    errorEl.hidden = false;
                    return;
                }

                const loginRes = await fetch("/api/admin/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email, phone, username, password }),
                });
                if (!loginRes.ok) {
                    errorEl.textContent = "계정은 만들어졌어요. 다시 로그인해주세요.";
                    errorEl.hidden = false;
                    document.getElementById("adminLoginToggleModeBtn")?.click();
                    return;
                }

                document.getElementById("adminLoginEmail").value = "";
                document.getElementById("adminLoginPhone").value = "";
                document.getElementById("adminLoginUsername").value = "";
                document.getElementById("adminLoginPassword").value = "";
                showAdminScreen();
            } catch (err) {
                console.error(err);
                errorEl.textContent = "서버에 연결할 수 없어요.";
                errorEl.hidden = false;
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = "계정 만들기";
            }
            return;
        }

        submitBtn.textContent = "확인 중...";

        try {
            const res = await fetch("/api/admin/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, phone, username, password }),
            });

            if (!res.ok) {
                errorEl.textContent = (await res.text()) || "로그인에 실패했어요.";
                errorEl.hidden = false;
                return;
            }

            document.getElementById("adminLoginEmail").value = "";
            document.getElementById("adminLoginPhone").value = "";
            document.getElementById("adminLoginUsername").value = "";
            document.getElementById("adminLoginPassword").value = "";
            showAdminScreen();
        } catch (err) {
            console.error(err);
            errorEl.textContent = "서버에 연결할 수 없어요.";
            errorEl.hidden = false;
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = "들어가기";
        }
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeLightbox();
        }
        if (document.getElementById("adminLightbox")?.classList.contains("open")) {
            if (e.key === "ArrowLeft") showLightboxPrev();
            if (e.key === "ArrowRight") showLightboxNext();
        }
    });

    document.querySelectorAll(".admin-language-pills .admin-pill").forEach((pill) => {
        pill.addEventListener("click", () => {
            const scope = pill.dataset.scope;
            const language = pill.dataset.lang;
            const prefix = prefixForScope(scope);

            document.querySelectorAll(`#${prefix}LanguagePills .admin-pill`).forEach((p) => p.classList.remove("active"));
            pill.classList.add("active");

            if (scope === "VIDEO" || scope === "TRIAL") {
                loadMaterials(language, scope, scope);
                return;
            }

            renderCategoryPills(prefix, scope, language);

            if (language !== "other") {
                const catContainer = document.getElementById(`${prefix}CategoryPills`);
                const firstCatPill = catContainer?.querySelector(".admin-pill");
                firstCatPill?.click();
            }
        });
    });

    function activateMainTab(key) {
        document.querySelectorAll(".admin-maintab").forEach((t) => {
            t.classList.remove("active");
            t.setAttribute("aria-selected", "false");
        });
        document.querySelectorAll(".admin-main-panel").forEach((p) => (p.hidden = true));

        const tab = document.querySelector(`.admin-maintab[data-main-tab="${key}"]`);
        if (tab) {
            tab.classList.add("active");
            tab.setAttribute("aria-selected", "true");
        }

        const panel = document.querySelector(`.admin-main-panel[data-main-panel="${key}"]`);
        if (panel) panel.hidden = false;

        updateHeroContent(key);

        currentMaterialScope = key === "kwzm" ? "KWZM" : key === "video" ? "VIDEO" : key === "trial" ? "TRIAL" : "PERSONAL";

        if (key === "personal" || key === "kwzm" || key === "video" || key === "trial") {
            const prefix = prefixForScope(currentMaterialScope);
            const langContainer = document.getElementById(`${prefix}LanguagePills`);
            if (langContainer && !langContainer.querySelector(".admin-pill.active")) {
                autoSelectFirstMaterials(prefix, currentMaterialScope);
            }
        }

        if (key === "students") {
            loadStudentList();
            loadAdminReviews();
            loadAdminPosts();
            loadSurveyData();
            loadVoiceSubmissionsAdmin();
            loadAssignmentSubmissionsAdmin();
        }
        if (key === "my") loadAdminMe();
        if (key === "notices") loadAdminNotices();
        if (key === "dashboard") loadDashboard();
        if (key === "vocab" && !document.querySelector("#vocabAdminLanguagePills .admin-pill.active")) {
            document.querySelector("#vocabAdminLanguagePills .admin-pill")?.click();
        }
        if (key === "questions") loadAdminQuestions();
    }

    document.querySelectorAll(".admin-maintab[data-main-tab]").forEach((tab) => {
        tab.addEventListener("click", () => activateMainTab(tab.dataset.mainTab));
    });

    // 그룹 탭(나만의 공부화면 / KWZM Center 관리 / IMKhun 관리 / 학생 관리) — 1단계 큰 탭
    const GROUP_SUBTABS = { personal: "adminSubtabsPersonal", kwzm: "adminSubtabsKwzm", imkhun: "adminSubtabsImkhun", students: "adminSubtabsStudents" };
    const GROUP_DEFAULT_TAB = { personal: "dashboard", kwzm: "kwzm", imkhun: "notices", students: "students" };

    document.querySelectorAll(".admin-grouptab").forEach((groupTab) => {
        groupTab.addEventListener("click", () => {
            document.querySelectorAll(".admin-grouptab").forEach((g) => {
                g.classList.remove("active");
                g.setAttribute("aria-selected", "false");
            });
            groupTab.classList.add("active");
            groupTab.setAttribute("aria-selected", "true");

            const group = groupTab.dataset.groupTab;

            // 서브탭 줄은 그룹에 맞는 것만 보여주고 나머지는 숨김 (없는 그룹은 전부 숨김)
            Object.values(GROUP_SUBTABS).forEach((id) => {
                const el = document.getElementById(id);
                if (el) el.hidden = true;
            });
            const subtabId = GROUP_SUBTABS[group];
            let subtabEl = null;
            if (subtabId) {
                subtabEl = document.getElementById(subtabId);
                if (subtabEl) subtabEl.hidden = false;
            }

            const firstPill = subtabEl ? subtabEl.querySelector(".admin-maintab") : null;
            if (firstPill) {
                // 서브탭이 있는 그룹은 첫 번째 알약을 직접 눌러서, 화면 전환이랑 배경색 표시가 항상 같이 일어나게 함
                firstPill.click();
            } else {
                activateMainTab(GROUP_DEFAULT_TAB[group]);
            }
        });
    });

    // "IMKhun 관리" 서브탭 (공지 글 / 강의 시간표 / FAQ) — notices 패널 안의 세부 화면을 직접 전환해요
    document.querySelectorAll("[data-imkhun-tab]").forEach((btn) => {
        btn.addEventListener("click", () => {
            activateMainTab("notices");

            document.querySelectorAll("[data-imkhun-tab]").forEach((b) => {
                b.classList.remove("active");
                b.setAttribute("aria-selected", "false");
            });
            btn.classList.add("active");
            btn.setAttribute("aria-selected", "true");

            const key = btn.dataset.imkhunTab;
            document.querySelectorAll('.admin-inline-panel[data-notice-panel]').forEach((p) => (p.hidden = true));
            const panel = document.querySelector(`.admin-inline-panel[data-notice-panel="${key}"]`);
            if (panel) panel.hidden = false;

            if (key === "timetable") loadAdminTimetable();
            if (key === "faq") loadAdminFaqs();
            if (key === "events") loadAdminEvents();
            if (key === "companyinfo") {
                loadCompanyInfoAdmin();
                loadFuturePlanAdmin();
            }
        });
    });

    // "학생 관리" 서브탭 (강의 신청 내역 / 오늘 출석 체크 / 리뷰 / 학생 게시판)
    document.querySelectorAll("[data-students-subtab]").forEach((btn) => {
        btn.addEventListener("click", () => {
            activateMainTab("students");

            document.querySelectorAll("[data-students-subtab]").forEach((b) => {
                b.classList.remove("active");
                b.setAttribute("aria-selected", "false");
            });
            btn.classList.add("active");
            btn.setAttribute("aria-selected", "true");

            const key = btn.dataset.studentsSubtab;
            document.querySelectorAll('.admin-inline-panel[data-students-panel]').forEach((p) => (p.hidden = true));
            const panel = document.querySelector(`.admin-inline-panel[data-students-panel="${key}"]`);
            if (panel) panel.hidden = false;

            if (key === "attendance-history") loadAttendanceHistory();
        });
    });

    document.getElementById("adminPaymentSaveBtn")?.addEventListener("click", saveAdminPayment);
    document.getElementById("adminInfoSaveBtn")?.addEventListener("click", saveAdminInfo);
    document.getElementById("adminPasswordSaveBtn")?.addEventListener("click", changeAdminPassword);

    document.querySelectorAll(".admin-inline-tab").forEach((tab) => {
        tab.addEventListener("click", () => {
            // 탭 그룹(학생 관리 / 마이 / 공지사항 관리 등)마다 따로 동작하도록, 자기가 속한 nav 안에서만 active를 바꿔요
            const group = tab.closest(".admin-inline-tabs");
            const panelContainer = group?.parentElement;
            if (!group || !panelContainer) return;

            // data-xxx-tab 형태의 속성 이름을 그대로 찾아서, 그에 맞는 data-xxx-panel로 매칭해요
            const tabAttr = [...tab.attributes].map((a) => a.name).find((n) => n.startsWith("data-") && n.endsWith("-tab"));
            if (!tabAttr) return;
            const panelAttr = tabAttr.replace(/-tab$/, "-panel");
            const key = tab.getAttribute(tabAttr);

            group.querySelectorAll(".admin-inline-tab").forEach((t) => {
                t.classList.remove("active");
                t.setAttribute("aria-selected", "false");
            });
            panelContainer.querySelectorAll(".admin-inline-panel").forEach((p) => (p.hidden = true));

            tab.classList.add("active");
            tab.setAttribute("aria-selected", "true");

            const panel = panelContainer.querySelector(`.admin-inline-panel[${panelAttr}="${key}"]`);
            if (panel) panel.hidden = false;

            if (tabAttr === "data-notice-tab") {
                if (key === "timetable") loadAdminTimetable();
                if (key === "faq") loadAdminFaqs();
            }
            if (tabAttr === "data-students-tab" && key === "attendance-today") {
                loadAttendanceToday();
            }
            if (tabAttr === "data-my-tab" && key === "backup") {
                loadBackups();
            }
        });
    });

    document.getElementById("adminRegisterBtn")?.addEventListener("click", () => {
        closeRegisterModal();
        openRegisterModal();
        document.querySelectorAll('input[name="registerScope"]').forEach((r) => (r.disabled = false));
        const preselect = document.querySelector(`input[name="registerScope"][value="${currentMaterialScope}"]`);
        if (preselect) preselect.checked = true;
        updateCategoryFieldVisibility();
    });
    document.addEventListener("click", (e) => {
        if (e.target.closest("[data-register-close]")) closeRegisterModal();
    });
    document.getElementById("registerSubmitBtn")?.addEventListener("click", submitRegisterMaterial);

    document.getElementById("adminInviteBtn")?.addEventListener("click", () => {
        if (currentInviteLanguage) openInviteModal(currentInviteLanguage);
    });
    document.getElementById("videoInviteBtn")?.addEventListener("click", () => {
        if (currentInviteLanguage) openInviteModal(currentInviteLanguage);
    });
    document.addEventListener("click", (e) => {
        if (e.target.closest("[data-invite-close]")) closeInviteModal();
        if (e.target.closest("[data-course-change-close]")) closeCourseChangeModal();
        if (e.target.closest("[data-payment-info-close]")) closePaymentInfoModal();
        if (e.target.closest("[data-notice-modal-close]")) closeNoticeModal();
        if (e.target.closest("[data-event-modal-close]")) closeEventModal();
        if (e.target.closest("[data-futureplan-item-close]")) closeFuturePlanItemModal();

        const changeCourseBtn = e.target.closest("[data-change-course-id]");
        if (changeCourseBtn) openCourseChangeModal(changeCourseBtn.dataset.changeCourseId);

        const paymentInfoBtn = e.target.closest("[data-payment-info-id]");
        if (paymentInfoBtn) openPaymentInfoModal(paymentInfoBtn.dataset.paymentInfoId);

        const confirmPaymentBtn = e.target.closest("[data-confirm-payment-id]");
        if (confirmPaymentBtn) confirmPaymentReceived(confirmPaymentBtn.dataset.confirmPaymentId, confirmPaymentBtn);

        const viewReceiptBtn = e.target.closest("[data-view-receipt-id]");
        if (viewReceiptBtn) {
            const app = studentApplicationsCache.find((a) => String(a.id) === viewReceiptBtn.dataset.viewReceiptId);
            if (app && app.receiptImage) openLightbox([{ fileData: app.receiptImage }]);
        }

        const editNoticeBtn = e.target.closest("[data-edit-notice-id]");
        if (editNoticeBtn) openNoticeModal(editNoticeBtn.dataset.editNoticeId);

        const deleteNoticeBtn = e.target.closest("[data-delete-notice-id]");
        if (deleteNoticeBtn) deleteNotice(deleteNoticeBtn.dataset.deleteNoticeId);

        const deleteEventBtn = e.target.closest("[data-delete-event-id]");
        if (deleteEventBtn) deleteEvent(deleteEventBtn.dataset.deleteEventId);

        const editFuturePlanItemBtn = e.target.closest("[data-edit-futureplan-item]");
        if (editFuturePlanItemBtn) openFuturePlanItemModal(editFuturePlanItemBtn.dataset.editFutureplanItem);

        const deleteFuturePlanItemBtn = e.target.closest("[data-delete-futureplan-item]");
        if (deleteFuturePlanItemBtn) deleteFuturePlanItem(deleteFuturePlanItemBtn.dataset.deleteFutureplanItem);

        if (e.target.closest("[data-timetable-modal-close]")) closeTimetableModal();
        const editTimetableBtn = e.target.closest("[data-edit-timetable-id]");
        if (editTimetableBtn) openTimetableModal(editTimetableBtn.dataset.editTimetableId);
        const deleteTimetableBtn = e.target.closest("[data-delete-timetable-id]");
        if (deleteTimetableBtn) deleteTimetableEntry(deleteTimetableBtn.dataset.deleteTimetableId);

        if (e.target.closest("[data-attendance-modal-close]")) closeAttendanceModal();
        const attendanceBtn = e.target.closest("[data-attendance-id]");
        if (attendanceBtn) openAttendanceModal(attendanceBtn.dataset.attendanceId, attendanceBtn.dataset.attendanceCourse);
        const deleteAttendanceBtn = e.target.closest("[data-delete-attendance-id]");
        if (deleteAttendanceBtn) deleteAttendanceRecordAdmin(deleteAttendanceBtn.dataset.deleteAttendanceId);
        const deleteLessonNoteBtn = e.target.closest("[data-delete-lesson-note-id]");
        if (deleteLessonNoteBtn) deleteLessonNote(deleteLessonNoteBtn.dataset.deleteLessonNoteId);
        const deleteLevelRecordBtn = e.target.closest("[data-delete-level-record-id]");
        if (deleteLevelRecordBtn) deleteLevelRecord(deleteLevelRecordBtn.dataset.deleteLevelRecordId);
        const deleteAssignmentBtn = e.target.closest("[data-delete-assignment-id]");
        if (deleteAssignmentBtn) deleteAssignmentAdmin(deleteAssignmentBtn.dataset.deleteAssignmentId);
        const respondClasschangeBtn = e.target.closest("[data-respond-classchange-id]");
        if (respondClasschangeBtn) {
            respondToClassChangeRequest(respondClasschangeBtn.dataset.respondClasschangeId, respondClasschangeBtn.dataset.respondApproved === "true");
        }

        if (e.target.closest("[data-send-file-modal-close]")) closeSendFileModal();
        const sendFileBtn = e.target.closest("[data-send-file-id]");
        if (sendFileBtn) openSendFileModal(sendFileBtn.dataset.sendFileId, sendFileBtn.dataset.sendFileCourse);
        const deleteSentFileBtn = e.target.closest("[data-delete-sent-file-id]");
        if (deleteSentFileBtn) deleteSentFile(deleteSentFileBtn.dataset.deleteSentFileId);

        if (e.target.closest("[data-schedule-modal-close]")) closeScheduleModal();
        const scheduleBtn = e.target.closest("[data-schedule-id]");
        if (scheduleBtn) {
            openScheduleModal(scheduleBtn.dataset.scheduleId, scheduleBtn.dataset.scheduleCourse,
                scheduleBtn.dataset.scheduleDays, scheduleBtn.dataset.scheduleTime, scheduleBtn.dataset.scheduleEnrollmentEnd);
        }

        const markAttendanceBtn = e.target.closest("[data-mark-attendance]");
        if (markAttendanceBtn) markTodayAttendance(markAttendanceBtn.dataset.markAttendance, markAttendanceBtn.dataset.markStatus);

        if (e.target.closest("[data-faq-modal-close]")) closeFaqModal();
        const editFaqBtn = e.target.closest("[data-edit-faq-id]");
        if (editFaqBtn) openFaqModal(editFaqBtn.dataset.editFaqId);
        const deleteFaqBtn = e.target.closest("[data-delete-faq-id]");
        if (deleteFaqBtn) deleteFaq(deleteFaqBtn.dataset.deleteFaqId);

        const dashboardGotoBtn = e.target.closest("[data-dashboard-goto]");
        if (dashboardGotoBtn) {
            document.querySelector(`[data-main-tab="${dashboardGotoBtn.dataset.dashboardGoto}"]`)?.click();
        }
    });
    document.getElementById("courseChangeSaveBtn")?.addEventListener("click", submitCourseChange);
    document.getElementById("paymentInfoSaveBtn")?.addEventListener("click", submitPaymentInfo);
    document.getElementById("adminNoticeNewBtn")?.addEventListener("click", () => openNoticeModal());
    document.getElementById("noticeSaveBtn")?.addEventListener("click", submitNotice);
    document.getElementById("adminEventNewBtn")?.addEventListener("click", openEventModal);
    document.getElementById("eventSaveBtn")?.addEventListener("click", submitEvent);
    document.getElementById("companyInfoSaveBtn")?.addEventListener("click", () => submitCompanyInfo("COMPANY"));
    document.getElementById("businessInfoSaveBtn")?.addEventListener("click", () => submitCompanyInfo("BUSINESS"));
    document.getElementById("futureplanSectionSaveBtn")?.addEventListener("click", submitFuturePlanSection);
    document.getElementById("futureplanItemNewBtn")?.addEventListener("click", () => openFuturePlanItemModal(null));
    document.getElementById("futureplanItemSaveBtn")?.addEventListener("click", submitFuturePlanItem);
    document.getElementById("adminTimetableNewBtn")?.addEventListener("click", () => openTimetableModal());
    document.getElementById("timetableSaveBtn")?.addEventListener("click", submitTimetableEntry);
    document.getElementById("attendanceSaveBtn")?.addEventListener("click", submitAttendanceRecord);
    document.getElementById("lessonNoteSaveBtn")?.addEventListener("click", submitLessonNote);
    document.getElementById("adminBackupRunBtn")?.addEventListener("click", runBackupNow);
    document.getElementById("adminVoiceList")?.addEventListener("click", (e) => {
        const saveBtn = e.target.closest("[data-save-voice-comment]");
        if (saveBtn) saveVoiceComment(saveBtn.dataset.saveVoiceComment);
        const deleteBtn = e.target.closest("[data-delete-voice-id]");
        if (deleteBtn) deleteVoiceSubmission(deleteBtn.dataset.deleteVoiceId);
    });
    document.getElementById("adminAssignmentSubmissionsList")?.addEventListener("click", (e) => {
        const viewBtn = e.target.closest("[data-view-assignment-submission]");
        if (viewBtn) openAssignmentViewModal(viewBtn.dataset.viewAssignmentSubmission);
    });
    document.querySelectorAll("[data-assignment-view-close]").forEach((btn) => {
        btn.addEventListener("click", closeAssignmentViewModal);
    });
    document.getElementById("assignmentViewLightboxPrev")?.addEventListener("click", () => navigateAssignmentViewLightbox(-1));
    document.getElementById("assignmentViewLightboxNext")?.addEventListener("click", () => navigateAssignmentViewLightbox(1));
    document.getElementById("assignmentViewCommentSaveBtn")?.addEventListener("click", saveAssignmentViewComment);
    document.getElementById("assignmentSubmissionSearch")?.addEventListener("input", renderAssignmentSubmissions);
    document.querySelectorAll("[data-submission-filter]").forEach((tab) => {
        tab.addEventListener("click", () => {
            assignmentSubmissionFilter = tab.dataset.submissionFilter;
            document.querySelectorAll("[data-submission-filter]").forEach((t) => t.classList.toggle("active", t === tab));
            renderAssignmentSubmissions();
        });
    });
    document.getElementById("vocabAdminLanguagePills")?.addEventListener("click", (e) => {
        const pill = e.target.closest("[data-lang]");
        if (!pill) return;
        document.querySelectorAll("#vocabAdminLanguagePills .admin-pill").forEach((p) => p.classList.toggle("active", p === pill));
        loadVocabAdminSets(pill.dataset.lang);
    });
    document.getElementById("vocabAdminWordList")?.addEventListener("click", (e) => {
        const deleteBtn = e.target.closest("[data-delete-vocab-id]");
        if (deleteBtn) deleteVocabWord(deleteBtn.dataset.deleteVocabId);
    });
    document.getElementById("vocabSetNewBtn")?.addEventListener("click", () => openVocabRegisterModal("create"));
    document.getElementById("vocabWordAddMoreBtn")?.addEventListener("click", () => openVocabRegisterModal("add"));
    document.querySelectorAll("[data-vocab-register-close]").forEach((btn) => {
        btn.addEventListener("click", closeVocabRegisterModal);
    });
    document.getElementById("vocabRegisterAddRowBtn")?.addEventListener("click", addVocabRegisterRow);
    document.getElementById("vocabRegisterRows")?.addEventListener("click", (e) => {
        const removeBtn = e.target.closest("[data-remove-vocab-row]");
        if (removeBtn) removeBtn.closest(".admin-vocab-register-row")?.remove();
    });
    document.getElementById("vocabSetSaveBtn")?.addEventListener("click", submitVocabRegister);
    document.getElementById("vocabSetList")?.addEventListener("click", (e) => {
        const deleteBtn = e.target.closest("[data-delete-vocab-set]");
        if (deleteBtn) {
            deleteVocabSet(deleteBtn.dataset.deleteVocabSet);
            return;
        }
        const info = e.target.closest("[data-select-vocab-set]");
        if (info) selectVocabSet(Number(info.dataset.selectVocabSet));
    });
    document.getElementById("vocabQuizPreviewBtn")?.addEventListener("click", startVocabQuizPreview);
    document.getElementById("vocabPreviewQuizCloseBtn")?.addEventListener("click", closeVocabQuizPreview);
    document.getElementById("levelRecordSaveBtn")?.addEventListener("click", submitLevelRecord);
    document.getElementById("assignmentSaveBtn")?.addEventListener("click", submitAssignment);
    document.getElementById("sendFileSubmitBtn")?.addEventListener("click", submitSendFile);
    document.getElementById("scheduleSaveBtn")?.addEventListener("click", submitSchedule);
    document.getElementById("sendPaymentReminderBtn")?.addEventListener("click", sendPaymentRemindersNow);

    document.querySelectorAll("[data-timetable-view]").forEach((btn) => {
        btn.addEventListener("click", () => {
            document.querySelectorAll("[data-timetable-view]").forEach((b) => {
                b.classList.remove("active");
                b.setAttribute("aria-selected", "false");
            });
            btn.classList.add("active");
            btn.setAttribute("aria-selected", "true");

            const view = btn.dataset.timetableView;
            const listEl = document.getElementById("adminTimetableList");
            const calendarEl = document.getElementById("adminTimetableCalendar");
            if (listEl) listEl.hidden = view !== "list";
            if (calendarEl) calendarEl.hidden = view !== "calendar";
        });
    });
    document.getElementById("adminCheckinDockDateInput")?.addEventListener("change", () => {
        adminCalendarViewYear = null;
        adminCalendarViewMonth = null;
        loadAttendanceToday();
    });
    document.getElementById("adminCheckinCalendarPrevBtn")?.addEventListener("click", () => {
        adminCalendarViewMonth -= 1;
        if (adminCalendarViewMonth < 0) {
            adminCalendarViewMonth = 11;
            adminCalendarViewYear -= 1;
        }
        renderAdminCheckinCalendar();
    });
    document.getElementById("adminCheckinCalendarNextBtn")?.addEventListener("click", () => {
        adminCalendarViewMonth += 1;
        if (adminCalendarViewMonth > 11) {
            adminCalendarViewMonth = 0;
            adminCalendarViewYear += 1;
        }
        renderAdminCheckinCalendar();
    });
    document.getElementById("adminCheckinCalendarGrid")?.addEventListener("click", (e) => {
        const dayBtn = e.target.closest("[data-calendar-date]");
        if (!dayBtn) return;
        const dateInput = document.getElementById("adminCheckinDockDateInput");
        if (dateInput) dateInput.value = dayBtn.dataset.calendarDate;
        loadAttendanceToday();
    });
    document.getElementById("adminCheckinDockMarkRestBtn")?.addEventListener("click", markRestAbsentToday);
    document.getElementById("attendanceHistoryFilterTabs")?.addEventListener("click", (e) => {
        const tab = e.target.closest("[data-attendance-filter]");
        if (!tab) return;
        attendanceHistoryFilter = tab.dataset.attendanceFilter;
        document.querySelectorAll("[data-attendance-filter]").forEach((t) => t.classList.toggle("active", t === tab));
        renderAttendanceHistoryTable();
    });
    document.getElementById("attendanceHistoryTable")?.addEventListener("change", (e) => {
        const select = e.target.closest("[data-history-record-id]");
        if (select) changeAttendanceHistoryStatus(select);
    });

    document.getElementById("adminCheckinDockTab")?.addEventListener("click", (e) => {
        e.stopPropagation();
        const dock = document.getElementById("adminCheckinDock");
        const tab = document.getElementById("adminCheckinDockTab");
        if (!dock || !tab) return;
        const willOpen = !dock.classList.contains("is-open");
        dock.classList.toggle("is-open", willOpen);
        tab.setAttribute("aria-expanded", willOpen ? "true" : "false");
    });
    document.getElementById("adminCheckinDockPanel")?.addEventListener("click", (e) => e.stopPropagation());
    document.addEventListener("click", (e) => {
        const dock = document.getElementById("adminCheckinDock");
        if (dock && dock.classList.contains("is-open") && !dock.contains(e.target)) {
            dock.classList.remove("is-open");
            document.getElementById("adminCheckinDockTab")?.setAttribute("aria-expanded", "false");
        }
    });
    document.getElementById("adminFaqNewBtn")?.addEventListener("click", () => openFaqModal());
    document.getElementById("faqSaveBtn")?.addEventListener("click", submitFaq);
    document.getElementById("inviteAddBtn")?.addEventListener("click", inviteStudentToLanguage);
    document.getElementById("inviteStudentNumberInput")?.addEventListener("keydown", (e) => {
        if (e.key === "Enter") inviteStudentToLanguage();
    });
    document.getElementById("bulkInviteToggleBtn")?.addEventListener("click", toggleBulkInviteField);
    document.getElementById("bulkInviteSubmitBtn")?.addEventListener("click", submitBulkInvite);
    document.addEventListener("click", (e) => {
        const removeBtn = e.target.closest("[data-remove-student]");
        if (!removeBtn) return;
        removeInvitedStudent(removeBtn.dataset.removeStudent);
    });
    document.getElementById("registerLanguageSelect")?.addEventListener("change", updateCategoryFieldVisibility);
    document.querySelectorAll('input[name="registerScope"]').forEach((radio) => {
        radio.addEventListener("change", updateCategoryFieldVisibility);
    });
    document.getElementById("registerFileInput")?.addEventListener("change", (e) => {
        const fileNameEl = document.getElementById("registerFileName");
        const files = Array.from(e.target.files || []);
        if (!fileNameEl) return;

        if (files.length === 0) {
            fileNameEl.textContent = "";
        } else if (files.length === 1) {
            fileNameEl.textContent = `선택된 파일: ${files[0].name}`;
        } else {
            fileNameEl.textContent = `선택된 파일 ${files.length}개: ${files.map((f) => f.name).join(", ")}`;
        }
    });

    document.addEventListener("click", (e) => {
        if (e.target.closest("[data-lightbox-close]")) closeLightbox();
    });

    document.getElementById("adminLightboxPrev")?.addEventListener("click", showLightboxPrev);
    document.getElementById("adminLightboxNext")?.addEventListener("click", showLightboxNext);

    document.addEventListener("click", (e) => {
        const card = e.target.closest(".admin-material-item");
        if (!card || !card._materialData) return;
        const material = card._materialData;

        if (e.target.closest("[data-view-btn]")) {
            handleViewMaterial(material);
            return;
        }
        if (e.target.closest("[data-edit-btn]")) {
            openEditModal(material);
            return;
        }
        if (e.target.closest("[data-delete-btn]")) {
            deleteMaterial(material.id, material.language, material.category, material.scope);
            return;
        }
    });

    document.addEventListener("click", async (e) => {
        const approveBtn = e.target.closest("[data-approve-id]");
        if (!approveBtn) return;

        const id = approveBtn.dataset.approveId;
        approveBtn.disabled = true;

        try {
            const res = await fetch(`/api/admin/applications/${id}/status`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ status: "APPROVED" }),
            });

            if (!res.ok) {
                alert((await res.text()) || "승인에 실패했어요.");
                approveBtn.disabled = false;
                return;
            }

            loadStudentList();
        } catch (err) {
            console.error(err);
            alert("서버에 연결할 수 없어요.");
            approveBtn.disabled = false;
        }
    });

    document.addEventListener("click", async (e) => {
        const btn = e.target.closest("[data-set-status-id]");
        if (!btn) return;

        const id = btn.dataset.setStatusId;
        const status = btn.dataset.setStatusValue;
        const confirmText = status === "WITHDRAWN" ? "이 학생을 퇴원 처리할까요? KWZM 로그인이 막혀요."
            : status === "SUSPENDED" ? "이 학생을 휴면 처리할까요? KWZM 로그인이 막혀요."
                : "이 학생을 다시 승인 상태로 복구할까요?";
        if (!confirm(confirmText)) return;

        btn.disabled = true;
        try {
            const res = await fetch(`/api/admin/applications/${id}/status`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ status }),
            });

            if (!res.ok) {
                alert((await res.text()) || "처리에 실패했어요.");
                btn.disabled = false;
                return;
            }

            loadStudentList();
        } catch (err) {
            console.error(err);
            alert("서버에 연결할 수 없어요.");
            btn.disabled = false;
        }
    });

    document.addEventListener("click", (e) => {
        const deleteBtn = e.target.closest("[data-delete-post-id]");
        if (!deleteBtn) return;
        deleteAdminPost(deleteBtn.dataset.deletePostId);
    });

    document.addEventListener("click", async (e) => {
        const copyBtn = e.target.closest(".admin-copy-btn");
        if (!copyBtn) return;
        e.stopPropagation();

        const value = copyBtn.dataset.copyValue;
        if (!value) return;

        try {
            await navigator.clipboard.writeText(value);
            const original = copyBtn.innerHTML;
            copyBtn.innerHTML = ICON_CHECK;
            copyBtn.classList.add("is-copied");
            setTimeout(() => {
                copyBtn.innerHTML = original;
                copyBtn.classList.remove("is-copied");
            }, 1200);
        } catch (err) {
            console.error(err);
        }
    });

    document.addEventListener("click", async (e) => {
        const submitBtn = e.target.closest("[data-reply-submit]");
        if (!submitBtn) return;

        const reviewId = submitBtn.dataset.replySubmit;
        const item = submitBtn.closest(".admin-review-item");
        const textarea = item?.querySelector("[data-reply-input]");
        const reply = textarea ? textarea.value.trim() : "";

        if (!reply) {
            alert("답글 내용을 입력해주세요.");
            return;
        }

        submitBtn.disabled = true;

        try {
            const res = await fetch(`/api/admin/reviews/${reviewId}/reply`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ reply }),
            });

            if (!res.ok) {
                alert((await res.text()) || "답글 등록에 실패했어요.");
                submitBtn.disabled = false;
                return;
            }

            loadAdminReviews();
        } catch (err) {
            console.error(err);
            alert("서버에 연결할 수 없어요.");
            submitBtn.disabled = false;
        }
    });

    document.addEventListener("click", async (e) => {
        const answerBtn = e.target.closest("[data-answer-submit]");
        if (!answerBtn) return;

        const questionId = answerBtn.dataset.answerSubmit;
        const item = answerBtn.closest(".admin-review-item");
        const textarea = item?.querySelector("[data-answer-input]");
        const answerText = textarea ? textarea.value.trim() : "";

        if (!answerText) {
            alert("답변 내용을 입력해주세요.");
            return;
        }

        answerBtn.disabled = true;

        try {
            const res = await fetch(`/api/admin/questions/${questionId}/answer`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ answerText }),
            });

            if (!res.ok) {
                alert((await res.text()) || "답변 등록에 실패했어요.");
                answerBtn.disabled = false;
                return;
            }

            loadAdminQuestions();
        } catch (err) {
            console.error(err);
            alert("서버에 연결할 수 없어요.");
            answerBtn.disabled = false;
        }
    });

    initHeroSlideshow();
    checkAdminSessionOnLoad();
});