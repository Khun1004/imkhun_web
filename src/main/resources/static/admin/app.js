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
    updateHeroContent("personal");
}

// 상단 배너에 탭마다 다른 제목/설명을 보여줘요.
const HERO_CONTENT = {
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
    if (key !== "personal" && key !== "kwzm") return;

    const prefix = key === "kwzm" ? "kwzm" : "personal";
    const scope = key === "kwzm" ? "KWZM" : "PERSONAL";
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

const CATEGORY_LABEL = { GRAMMAR: "문법", READING: "읽기", WRITING: "쓰기", SPEAKING: "말하기", OTHER: "기타" };
const LANGUAGE_LABEL = { korean: "한국어", japanese: "일본어", thai: "태국어", english: "영어", other: "기타" };

function escapeHtmlForAdminMaterial(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
}

let currentMaterialScope = "PERSONAL";
let editingMaterialScope = "PERSONAL";

function materialsApiBase(scope) {
    return scope === "KWZM" ? "/api/admin/kwzm-materials" : "/api/admin/materials";
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

    Object.entries(CATEGORY_LABEL).forEach(([key, label]) => {
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

async function loadMaterials(language, category, scope) {
    currentMaterialScope = scope || "PERSONAL";
    const prefix = currentMaterialScope === "KWZM" ? "kwzm" : "personal";
    const emptyText = document.getElementById(`${prefix}MaterialsEmpty`);
    const list = document.getElementById(`${prefix}MaterialsList`);
    const heading = document.getElementById(`${prefix}MaterialsHeading`);
    const view = document.getElementById(`${prefix}MaterialsView`);
    const inviteBtn = document.getElementById("adminInviteBtn");
    if (!list) return;

    if (view) view.hidden = false;
    heading.textContent = `${LANGUAGE_LABEL[language] || language} · ${CATEGORY_LABEL[category] || category}`;

    if (inviteBtn) {
        if (currentMaterialScope === "KWZM") {
            inviteBtn.hidden = false;
            currentInviteLanguage = language;
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

function openInviteModal(language) {
    currentInviteLanguage = language;
    const modal = document.getElementById("adminInviteModal");
    if (!modal) return;
    document.getElementById("inviteModalTitle").textContent = `학생 초대 · ${LANGUAGE_LABEL[language] || language}`;
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
}

async function loadInvitedStudents() {
    const listEl = document.getElementById("adminInvitedList");
    const countEl = document.getElementById("inviteCount");
    if (!listEl || !currentInviteLanguage) return;

    listEl.innerHTML = `<p class="admin-note-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/admin/kwzm-invites?language=${currentInviteLanguage}`);
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
        const res = await fetch(`/api/admin/kwzm-invites?language=${currentInviteLanguage}`, {
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

async function removeInvitedStudent(studentNumber) {
    if (!confirm("이 학생을 초대 목록에서 제거할까요?")) return;
    try {
        const res = await fetch(`/api/admin/kwzm-invites?language=${currentInviteLanguage}&studentNumber=${encodeURIComponent(studentNumber)}`, {
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
    const badge = document.getElementById("adminInviteCountBadge");
    if (!badge) return;
    if (knownCount !== undefined) {
        badge.textContent = knownCount;
        return;
    }
    try {
        const res = await fetch(`/api/admin/kwzm-invites?language=${currentInviteLanguage}`);
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
    document.getElementById("registerModalTitle").textContent = `자료 수정 (${editingMaterialScope === "KWZM" ? "KWZM 학생용" : "개인용"})`;
    document.getElementById("registerSubmitBtn").textContent = "수정하기";
    document.getElementById("registerEditingId").value = material.id;
    document.getElementById("registerLanguageSelect").value = material.language;
    document.getElementById("registerTitleInput").value = material.title;
    document.getElementById("registerDescriptionInput").value = material.description || "";
    document.querySelector(`input[name="registerCategory"][value="${material.category}"]`).checked = true;
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
    const categoryField = document.getElementById("registerCategoryField");
    if (categoryField) categoryField.hidden = language === "other";
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

    document.getElementById("registerModalTitle").textContent = `자료 등록 (${currentMaterialScope === "KWZM" ? "KWZM 학생용" : "개인용"})`;
    document.getElementById("registerSubmitBtn").textContent = "등록하기";
    document.getElementById("registerEditingId").value = "";
}

async function submitRegisterMaterial() {
    const editingId = document.getElementById("registerEditingId").value;
    const selectedScope = document.querySelector('input[name="registerScope"]:checked')?.value || "PERSONAL";
    const language = document.getElementById("registerLanguageSelect").value;
    const category = language === "other"
        ? "OTHER"
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

        const prefix = submitScope === "KWZM" ? "kwzm" : "personal";
        const activeLangPill = document.querySelector(`#${prefix}LanguagePills .admin-pill.active`);
        const activeCatPill = document.querySelector(`#${prefix}CategoryPills .admin-pill.active`);
        const activeCategory = language === "other" ? "OTHER" : activeCatPill?.textContent && Object.entries(CATEGORY_LABEL).find(([, v]) => v === activeCatPill.textContent)?.[0];
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

async function loadAdminMe() {
    try {
        const res = await fetch("/api/admin/me");
        if (!res.ok) return;
        const data = await res.json();

        document.getElementById("adminMyUsername").textContent = data.username;
        document.getElementById("adminMyEmail").textContent = data.email;
        document.getElementById("adminMyPhone").textContent = data.phone;

        const paymentTextarea = document.getElementById("adminPaymentTextarea");
        if (paymentTextarea) paymentTextarea.value = data.paymentInfo || "";
    } catch (err) {
        console.error(err);
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

        list.innerHTML = "";
        emptyText.hidden = applications.length > 0;

        applications.forEach((app) => {
            const item = document.createElement("div");
            item.className = "admin-student-item";
            item.innerHTML = `
        <div class="admin-student-main">
          <p class="admin-student-name">${escapeHtmlForAdmin(app.nickname)} 님</p>
          ${app.memo ? `<p class="admin-student-memo">${escapeHtmlForAdmin(app.memo)}</p>` : ""}
        </div>
        <p class="admin-student-course">${escapeHtmlForAdmin(app.courseName)}<span>${escapeHtmlForAdmin(studyTypeLabel[app.studyType] || app.studyType)}</span></p>
        <p class="admin-student-contact">${escapeHtmlForAdmin(app.contact)}</p>
        ${app.studentNumber ? `<span class="admin-student-number">${escapeHtmlForAdmin(app.studentNumber)}</span>` : `<span></span>`}
        <p class="admin-student-date">${app.createdAt}</p>
        <div class="admin-student-status">
          <span class="mypage-badge ${statusClass[app.status] || ""}">${statusLabel[app.status] || app.status}</span>
          ${app.status === "PENDING" ? `<button type="button" class="admin-approve-btn" data-approve-id="${app.id}">승인하기</button>` : ""}
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

document.addEventListener("fragments:loaded", () => {

    document.getElementById("adminBackBtn")?.addEventListener("click", logoutAdmin);

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
            const prefix = scope === "KWZM" ? "kwzm" : "personal";

            document.querySelectorAll(`#${prefix}LanguagePills .admin-pill`).forEach((p) => p.classList.remove("active"));
            pill.classList.add("active");

            renderCategoryPills(prefix, scope, language);

            if (language !== "other") {
                const catContainer = document.getElementById(`${prefix}CategoryPills`);
                const firstCatPill = catContainer?.querySelector(".admin-pill");
                firstCatPill?.click();
            }
        });
    });

    document.querySelectorAll(".admin-maintab").forEach((tab) => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".admin-maintab").forEach((t) => {
                t.classList.remove("active");
                t.setAttribute("aria-selected", "false");
            });
            document.querySelectorAll(".admin-main-panel").forEach((p) => (p.hidden = true));

            tab.classList.add("active");
            tab.setAttribute("aria-selected", "true");

            const key = tab.dataset.mainTab;
            const panel = document.querySelector(`.admin-main-panel[data-main-panel="${key}"]`);
            if (panel) panel.hidden = false;

            updateHeroContent(key);

            currentMaterialScope = key === "kwzm" ? "KWZM" : "PERSONAL";

            if (key === "personal" || key === "kwzm") {
                const prefix = key === "kwzm" ? "kwzm" : "personal";
                const langContainer = document.getElementById(`${prefix}LanguagePills`);
                if (langContainer && !langContainer.querySelector(".admin-pill.active")) {
                    autoSelectFirstMaterials(prefix, currentMaterialScope);
                }
            }

            if (key === "students") {
                loadStudentList();
                loadAdminReviews();
            }
            if (key === "my") loadAdminMe();
        });
    });

    document.getElementById("adminPaymentSaveBtn")?.addEventListener("click", saveAdminPayment);

    document.querySelectorAll(".admin-inline-tab").forEach((tab) => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".admin-inline-tab").forEach((t) => {
                t.classList.remove("active");
                t.setAttribute("aria-selected", "false");
            });
            document.querySelectorAll(".admin-inline-panel").forEach((p) => (p.hidden = true));

            tab.classList.add("active");
            tab.setAttribute("aria-selected", "true");

            const panel = document.querySelector(`.admin-inline-panel[data-students-panel="${tab.dataset.studentsTab}"]`);
            if (panel) panel.hidden = false;
        });
    });

    document.getElementById("adminRegisterBtn")?.addEventListener("click", () => {
        closeRegisterModal();
        openRegisterModal();
        document.querySelectorAll('input[name="registerScope"]').forEach((r) => (r.disabled = false));
        const preselect = document.querySelector(`input[name="registerScope"][value="${currentMaterialScope}"]`);
        if (preselect) preselect.checked = true;
    });
    document.addEventListener("click", (e) => {
        if (e.target.closest("[data-register-close]")) closeRegisterModal();
    });
    document.getElementById("registerSubmitBtn")?.addEventListener("click", submitRegisterMaterial);

    document.getElementById("adminInviteBtn")?.addEventListener("click", () => {
        if (currentInviteLanguage) openInviteModal(currentInviteLanguage);
    });
    document.addEventListener("click", (e) => {
        if (e.target.closest("[data-invite-close]")) closeInviteModal();
    });
    document.getElementById("inviteAddBtn")?.addEventListener("click", inviteStudentToLanguage);
    document.getElementById("inviteStudentNumberInput")?.addEventListener("keydown", (e) => {
        if (e.key === "Enter") inviteStudentToLanguage();
    });
    document.addEventListener("click", (e) => {
        const removeBtn = e.target.closest("[data-remove-student]");
        if (!removeBtn) return;
        removeInvitedStudent(removeBtn.dataset.removeStudent);
    });
    document.getElementById("registerLanguageSelect")?.addEventListener("change", updateCategoryFieldVisibility);
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