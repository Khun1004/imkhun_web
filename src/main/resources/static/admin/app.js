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

function openFileInNewTab(dataUri) {
    try {
        const [header, base64] = dataUri.split(",");
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
            item.innerHTML = `
        <div class="admin-notice-item-head">
          <p class="admin-notice-item-title">${escapeHtmlForAdmin(n.title)}</p>
          <span class="admin-notice-item-date">${n.createdAt}</span>
        </div>
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
            body: JSON.stringify({ title, content }),
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
    } catch (err) {
        console.error(err);
    }
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

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    loadAttendanceForModal(applicationId);
}

function closeAttendanceModal() {
    const modal = document.getElementById("attendanceModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
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
        const fileData = await new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = reject;
            reader.readAsDataURL(file);
        });

        const res = await fetch(`/api/admin/applications/${applicationId}/files`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ category, fileName: file.name, fileData }),
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
    const input = document.getElementById("attendanceTodayDateInput");
    if (!input) return new Date().toISOString().slice(0, 10);
    if (!input.value) input.value = new Date().toISOString().slice(0, 10);
    return input.value;
}

async function loadAttendanceToday() {
    const list = document.getElementById("attendanceTodayList");
    const emptyEl = document.getElementById("attendanceTodayEmpty");
    if (!list) return;
    const date = getAttendanceTodayDate();
    list.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/attendance/today?date=${date}`);
        if (!res.ok) return;
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
        <span class="admin-attendance-today-student">${escapeHtmlForAdmin(entry.studentNickname || "")}</span>
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



function openScheduleModal(applicationId, courseName, classDays, classTime) {
    const modal = document.getElementById("scheduleModal");
    if (!modal) return;

    document.getElementById("scheduleApplicationId").value = applicationId;
    document.getElementById("scheduleModalTitle").textContent = `수업 시간 설정 · ${courseName}`;
    document.querySelectorAll('input[name="scheduleDay"]').forEach((cb) => {
        cb.checked = classDays ? classDays.split(",").includes(cb.value) : false;
    });
    document.getElementById("scheduleTimeInput").value = classTime || "";
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
    if (selectedFiles.some((f) => f.size > 4 * 1024 * 1024)) {
        errorEl.textContent = "파일 하나당 용량은 4MB 이하로 올려주세요.";
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

    const readFileAsDataUrl = (file) =>
        new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () =>
                resolve({ fileName: file.name, fileType: file.type, fileData: reader.result, linkUrl: null, textContent: null });
            reader.onerror = reject;
            reader.readAsDataURL(file);
        });

    try {
        const newFiles = await Promise.all(selectedFiles.map(readFileAsDataUrl));
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
        errorEl.textContent = "서버에 연결할 수 없어요.";
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
        return `<span class="admin-payment-status admin-payment-status--done">${ICON_CHECK} 결제 완료</span>`;
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
    const statusLabel = { PENDING: "승인대기", APPROVED: "승인완료" };
    const statusClass = { PENDING: "mypage-badge--pending", APPROVED: "mypage-badge--approved" };

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
          ${app.status === "APPROVED" ? `<button type="button" class="admin-material-action-btn" data-schedule-id="${app.id}" data-schedule-course="${escapeHtmlForAdmin(app.courseName)}" data-schedule-days="${app.classDays || ""}" data-schedule-time="${app.classTime || ""}">${app.classDays ? "수업 시간 확인/수정" : "수업 시간 설정"}</button>` : ""}
          ${app.status === "APPROVED" ? `<button type="button" class="admin-material-action-btn" data-attendance-id="${app.id}" data-attendance-course="${escapeHtmlForAdmin(app.courseName)}">출석부</button>` : ""}
          ${app.status === "APPROVED" ? `<button type="button" class="admin-material-action-btn" data-send-file-id="${app.id}" data-send-file-course="${escapeHtmlForAdmin(app.courseName)}">파일 보내기</button>` : ""}
          ${paymentStatusHtml(app)}
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

async function loadDashboard() {
    try {
        const res = await fetch("/api/admin/dashboard");
        if (!res.ok) return;
        const data = await res.json();

        const pendingEl = document.getElementById("dashboardPendingCount");
        const paymentEl = document.getElementById("dashboardPaymentPendingCount");
        const newThisMonthEl = document.getElementById("dashboardNewThisMonthCount");
        const unreadEl = document.getElementById("dashboardUnreadNotifCount");
        if (pendingEl) pendingEl.textContent = data.pendingApplicationsCount;
        if (paymentEl) paymentEl.textContent = data.paymentPendingConfirmCount;
        if (newThisMonthEl) newThisMonthEl.textContent = data.newApplicationsThisMonth;
        if (unreadEl) unreadEl.textContent = data.unreadNotificationsCount;

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
        }
        if (key === "my") loadAdminMe();
        if (key === "notices") loadAdminNotices();
        if (key === "dashboard") loadDashboard();
    }

    document.querySelectorAll(".admin-maintab").forEach((tab) => {
        tab.addEventListener("click", () => activateMainTab(tab.dataset.mainTab));
    });

    // 그룹 탭(나만의 공부화면 / KWZM Center 관리 / IMKhun 관리 / 학생 관리) — 1단계 큰 탭
    const GROUP_SUBTABS = { personal: "adminSubtabsPersonal", kwzm: "adminSubtabsKwzm" };
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
            if (subtabId) {
                const subtabEl = document.getElementById(subtabId);
                if (subtabEl) subtabEl.hidden = false;
            }

            activateMainTab(GROUP_DEFAULT_TAB[group]);
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

        if (e.target.closest("[data-send-file-modal-close]")) closeSendFileModal();
        const sendFileBtn = e.target.closest("[data-send-file-id]");
        if (sendFileBtn) openSendFileModal(sendFileBtn.dataset.sendFileId, sendFileBtn.dataset.sendFileCourse);
        const deleteSentFileBtn = e.target.closest("[data-delete-sent-file-id]");
        if (deleteSentFileBtn) deleteSentFile(deleteSentFileBtn.dataset.deleteSentFileId);

        if (e.target.closest("[data-schedule-modal-close]")) closeScheduleModal();
        const scheduleBtn = e.target.closest("[data-schedule-id]");
        if (scheduleBtn) {
            openScheduleModal(scheduleBtn.dataset.scheduleId, scheduleBtn.dataset.scheduleCourse,
                scheduleBtn.dataset.scheduleDays, scheduleBtn.dataset.scheduleTime);
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
    document.getElementById("adminTimetableNewBtn")?.addEventListener("click", () => openTimetableModal());
    document.getElementById("timetableSaveBtn")?.addEventListener("click", submitTimetableEntry);
    document.getElementById("attendanceSaveBtn")?.addEventListener("click", submitAttendanceRecord);
    document.getElementById("sendFileSubmitBtn")?.addEventListener("click", submitSendFile);
    document.getElementById("scheduleSaveBtn")?.addEventListener("click", submitSchedule);
    document.getElementById("attendanceTodayDateInput")?.addEventListener("change", loadAttendanceToday);
    document.getElementById("attendanceMarkRestAbsentBtn")?.addEventListener("click", markRestAbsentToday);
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

    initHeroSlideshow();
    checkAdminSessionOnLoad();
});