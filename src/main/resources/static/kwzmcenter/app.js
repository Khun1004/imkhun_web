// KWZM Computer Training & Language Center — 독립 페이지 전용 로직
// 이 페이지는 imkhun 메인 사이트와 별개의 새 창(새 탭)으로 열림

// ---- 조각(fragment) 불러오기: 메인 사이트의 script.js와 같은 방식, 이 페이지 안에서 자체적으로 처리 ----
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
                el.innerHTML = '<p class="eyebrow">오류</p><h2>내용을 불러오지 못했어요</h2>';
                console.error(err);
            }
        })
    );

    await loadFragments();
}

loadFragments().then(() => {
    document.dispatchEvent(new Event("fragments:loaded"));
});

// ---- 로그인 페이지 ↔ 포털 화면 전환 (팝업 아님, 이 페이지 안에서 그냥 보이기/숨기기) ----

function showStudentLoginPage() {
    const loginPage = document.getElementById("studentLoginPage");
    const screen = document.getElementById("studentScreen");
    if (loginPage) loginPage.hidden = false;
    if (screen) screen.hidden = true;
}

function showStudentScreen() {
    const loginPage = document.getElementById("studentLoginPage");
    const screen = document.getElementById("studentScreen");
    if (loginPage) loginPage.hidden = true;
    if (screen) screen.hidden = false;
    window.scrollTo({ top: 0, behavior: "instant" });
    // 화면이 숨겨진 동안엔 높이가 0으로 잡히므로, 실제로 보이게 된 다음에 다시 잼
    setTimeout(syncKwzmFixedOffsets, 0);
}

// 페이지를 열었을 때 이미 로그인이 살아있으면 바로 포털로, 아니면 로그인 화면
async function checkStudentSessionOnLoad() {
    try {
        const res = await fetch("/api/student/check");
        const data = await res.json();
        if (data.isStudent) {
            showStudentScreen();
            loadStudentPortalData();
        } else {
            showStudentLoginPage();
        }
    } catch (err) {
        console.error(err);
        showStudentLoginPage();
    }
}

async function logoutStudent() {
    try {
        await fetch("/api/student/logout", { method: "POST" });
    } catch (err) {
        console.error(err);
    } finally {
        showStudentLoginPage();
    }
}

const STUDENT_CATEGORY_LABEL = { GRAMMAR: "문법", READING: "읽기", WRITING: "쓰기", SPEAKING: "말하기", OTHER: "기타" };
const STUDENT_LANGUAGE_LABEL = { korean: "한국어", japanese: "일본어", thai: "태국어", english: "영어", other: "기타" };

function escapeHtmlForStudent(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
}

let studentCourses = [];

// 메인 탭(홈/언어/영상/Post/커뮤니티) 전환
function switchStudentMainTab(key) {
    document.querySelectorAll(".student-subnav-link").forEach((t) => {
        t.classList.toggle("active", t.dataset.studentMainTab === key);
        t.setAttribute("aria-selected", t.dataset.studentMainTab === key ? "true" : "false");
    });
    document.querySelectorAll(".student-main-panel").forEach((p) => {
        p.hidden = p.dataset.studentPanel !== key;
    });
}

// 로그인한 학생의 정보(닉네임 + 승인받은 강의)를 불러옴
async function loadStudentPortalData() {
    try {
        const res = await fetch("/api/student/me");
        if (!res.ok) return;
        const data = await res.json();

        const nameEl = document.getElementById("studentTopheaderName");
        if (nameEl) nameEl.textContent = `${data.nickname} 님`;

        studentCourses = data.courses || [];
        renderStudentHome(data.nickname);
        renderStudentLanguagePills();
        renderTodaysTip();
        loadRecentMaterials();
    } catch (err) {
        console.error(err);
    }
}

function renderStudentHome(nickname) {
    const greetingEl = document.getElementById("studentHomeGreeting");
    const coursesEl = document.getElementById("studentHomeCourses");
    const mypageGreetingEl = document.getElementById("mypageGreeting");
    const mypageCoursesEl = document.getElementById("mypageCourses");

    if (greetingEl) greetingEl.textContent = `안녕하세요, ${nickname}님!`;
    if (mypageGreetingEl) mypageGreetingEl.textContent = `안녕하세요, ${nickname}님!`;

    const renderCourseCards = (el) => {
        if (!el) return;
        el.innerHTML = "";
        if (studentCourses.length === 0) {
            el.innerHTML = `<p class="admin-empty-text">아직 승인된 강의가 없어요.</p>`;
            return;
        }
        studentCourses.forEach((c) => {
            const card = document.createElement("div");
            card.className = "student-home-course-card";
            card.innerHTML = `
        <p class="student-home-course-name">${escapeHtmlForStudent(c.courseName)}</p>
        <p class="student-home-course-number">${escapeHtmlForStudent(c.studentNumber)}</p>
      `;
            el.appendChild(card);
        });
    };
    renderCourseCards(coursesEl);
    renderCourseCards(mypageCoursesEl);

    // 홈 화면(공지사항 옆) "내 수강 정보" 카드 리스트
    const listEl = document.getElementById("studentHomeCourseList");
    if (!listEl) return;
    listEl.innerHTML = "";
    if (studentCourses.length === 0) {
        listEl.innerHTML = `<li class="admin-empty-text">아직 승인된 강의가 없어요.</li>`;
        return;
    }
    studentCourses.forEach((c) => {
        const li = document.createElement("li");
        li.innerHTML = `<span class="student-info-panel-date">${escapeHtmlForStudent(STUDENT_LANGUAGE_LABEL[c.language] || c.language)}</span><span>${escapeHtmlForStudent(c.courseName)} · ${escapeHtmlForStudent(c.studentNumber)}</span>`;
        listEl.appendChild(li);
    });
}

const STUDENT_LANGUAGE_ICON = { korean: "가", japanese: "あ", thai: "ก", english: "A", other: "＋" };

// 오늘의 표현 — 날짜 기준으로 하나씩 순서대로 보여줌 (매일 다른 문장)
const STUDENT_TODAYS_TIPS = [
    { text: "천 리 길도 한 걸음부터", sub: "A journey of a thousand miles begins with a single step" },
    { text: "고생 끝에 낙이 온다", sub: "After hardship comes happiness" },
    { text: "안녕하세요, 오늘도 화이팅!", sub: "Hello, let's do our best today!" },
    { text: "실수해도 괜찮아요, 그게 배우는 과정이에요", sub: "It's okay to make mistakes — that's how we learn" },
    { text: "티끌 모아 태산", sub: "Little strokes fell great oaks" },
    { text: "오늘 배운 것 하나가 내일의 나를 만들어요", sub: "One thing learned today shapes tomorrow's you" },
    { text: "시작이 반이다", sub: "Well begun is half done" },
];

function renderTodaysTip() {
    const textEl = document.getElementById("studentTipText");
    const subEl = document.getElementById("studentTipSub");
    if (!textEl) return;

    const dayIndex = Math.floor(Date.now() / (1000 * 60 * 60 * 24)) % STUDENT_TODAYS_TIPS.length;
    const tip = STUDENT_TODAYS_TIPS[dayIndex];
    textEl.textContent = tip.text;
    if (subEl) subEl.textContent = tip.sub;
}

// 홈 화면 "최근 등록된 자료"
async function loadRecentMaterials() {
    const listEl = document.getElementById("studentRecentMaterials");
    const emptyEl = document.getElementById("studentRecentEmpty");
    if (!listEl) return;

    try {
        const res = await fetch("/api/student/materials/recent");
        if (!res.ok) return;
        const materials = await res.json();

        listEl.innerHTML = "";
        if (emptyEl) emptyEl.hidden = materials.length > 0;

        materials.forEach((m) => {
            const files = m.files || [];
            const first = files[0];
            const firstIsImage = first && first.fileType && first.fileType.startsWith("image/");

            const item = document.createElement("div");
            item.className = "student-recent-item";
            item.innerHTML = `
        <div class="student-recent-thumb">${firstIsImage ? `<img src="${first.fileData}" alt="">` : `<span>${escapeHtmlForStudent(STUDENT_LANGUAGE_LABEL[m.language] || m.language)}</span>`}</div>
        <div class="student-recent-main">
          <p class="student-recent-title">${escapeHtmlForStudent(m.title)}</p>
          <p class="student-recent-meta">${escapeHtmlForStudent(STUDENT_LANGUAGE_LABEL[m.language] || m.language)} · ${m.createdAt}</p>
        </div>
      `;
            item.addEventListener("click", () => {
                switchStudentMainTab("language");
                handleStudentViewMaterial(m);
            });
            listEl.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

function renderStudentLanguagePills() {
    const pillsEl = document.getElementById("studentLanguagePills");
    if (!pillsEl) return;

    const languages = [...new Set(studentCourses.map((c) => c.language))];
    pillsEl.innerHTML = "";
    languages.forEach((lang) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "student-pill";
        btn.dataset.studentLang = lang;
        btn.textContent = STUDENT_LANGUAGE_LABEL[lang] || lang;
        btn.addEventListener("click", () => {
            pillsEl.querySelectorAll(".student-pill").forEach((p) => p.classList.remove("active"));
            btn.classList.add("active");
            renderStudentCategoryPills(lang);
        });
        pillsEl.appendChild(btn);
    });
}

function renderStudentCategoryPills(language) {
    const catEl = document.getElementById("studentCategoryPills");
    const view = document.getElementById("studentMaterialsView");
    if (!catEl) return;

    catEl.hidden = false;
    if (view) view.hidden = true;
    catEl.innerHTML = "";

    Object.entries(STUDENT_CATEGORY_LABEL).forEach(([key, label]) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "student-pill";
        btn.textContent = label;
        btn.addEventListener("click", () => {
            catEl.querySelectorAll(".student-pill").forEach((p) => p.classList.remove("active"));
            btn.classList.add("active");
            loadStudentMaterials(language, key);
        });
        catEl.appendChild(btn);
    });
}

async function loadStudentMaterials(language, category) {
    const emptyText = document.getElementById("studentMaterialsEmpty");
    const list = document.getElementById("studentMaterialsList");
    const heading = document.getElementById("studentMaterialsHeading");
    const view = document.getElementById("studentMaterialsView");
    if (!list) return;

    if (view) view.hidden = false;
    heading.textContent = `${STUDENT_LANGUAGE_LABEL[language] || language} · ${STUDENT_CATEGORY_LABEL[category] || category}`;

    try {
        const res = await fetch(`/api/student/materials?language=${language}&category=${category}`);
        if (!res.ok) return;
        const materials = await res.json();

        list.innerHTML = "";
        if (emptyText) emptyText.hidden = materials.length > 0;

        const ICON_LINK = `<svg viewBox="0 0 24 24" fill="none"><path d="M9.5 14.5l5-5M8 10l-1.5 1.5a3.5 3.5 0 0 0 5 5L13 15M16 14l1.5-1.5a3.5 3.5 0 0 0-5-5L11 9" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
        const ICON_TEXT = `<svg viewBox="0 0 24 24" fill="none"><path d="M6 4h9l4 4v12H6V4Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M9 12h6M9 16h6M9 8h3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`;
        const ICON_FILE = `<svg viewBox="0 0 24 24" fill="none"><path d="M6 4h9l4 4v12H6V4Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M15 4v4h4" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`;
        const ICON_NONE = `<svg viewBox="0 0 24 24" fill="none"><rect x="4" y="5" width="16" height="14" rx="2" stroke="currentColor" stroke-width="1.6"/></svg>`;

        materials.forEach((m) => {
            const item = document.createElement("div");
            item.className = "student-material-item";

            const files = m.files || [];
            const first = files[0];
            const firstIsImage = first && first.fileType && first.fileType.startsWith("image/");
            const firstIsLink = first && first.linkUrl;
            const firstIsText = first && first.textContent && !first.linkUrl && !first.fileData;
            const fileExt = (first?.fileName || "").split(".").pop()?.toUpperCase().slice(0, 4) || "FILE";

            const thumbHtml = firstIsImage
                ? `<img src="${first.fileData}" alt="">`
                : firstIsLink
                    ? `<div class="student-material-thumb-icon">${ICON_LINK}<span>링크</span></div>`
                    : firstIsText
                        ? `<div class="student-material-thumb-icon">${ICON_TEXT}<span>글</span></div>`
                        : first
                            ? `<div class="student-material-thumb-icon">${ICON_FILE}<span>${escapeHtmlForStudent(fileExt)}</span></div>`
                            : `<div class="student-material-thumb-icon">${ICON_NONE}<span>없음</span></div>`;

            const countBadgeHtml = files.length > 1
                ? `<span class="student-material-count-badge">+${files.length - 1}</span>`
                : "";

            const metaText = files.length === 0
                ? "첨부 없음"
                : files.length === 1
                    ? (firstIsLink ? "링크" : firstIsText ? "글" : first.fileName || "파일")
                    : `자료 ${files.length}개`;

            item.innerHTML = `
        <div class="student-material-thumb">${thumbHtml}${countBadgeHtml}</div>
        <div class="student-material-main">
          <p class="student-material-title">${escapeHtmlForStudent(m.title)}</p>
          ${m.description ? `<p class="student-material-desc">${escapeHtmlForStudent(m.description)}</p>` : ""}
          <p class="student-material-meta">${escapeHtmlForStudent(metaText)} · ${m.createdAt}</p>
        </div>
      `;
            item.addEventListener("click", () => handleStudentViewMaterial(m));
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

function openStudentFileInNewTab(dataUri) {
    try {
        const [header, base64] = dataUri.split(",");
        const mimeMatch = header.match(/data:(.*?);base64/);
        const mimeType = mimeMatch ? mimeMatch[1] : "application/octet-stream";
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        const blob = new Blob([bytes], { type: mimeType });
        window.open(URL.createObjectURL(blob), "_blank");
    } catch (err) {
        console.error(err);
        alert("파일을 여는 데 실패했어요.");
    }
}

function openStudentTextInNewTab(text, title) {
    const escapeForHtml = (s) => (s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    const html = `<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><title>${escapeForHtml(title || "글")}</title>
<style>body{font-family:"Noto Sans KR",sans-serif;max-width:720px;margin:60px auto;padding:0 24px 60px;line-height:1.9;color:#222;white-space:pre-wrap;}h1{font-size:20px;margin-bottom:24px;}</style>
</head><body><h1>${escapeForHtml(title || "")}</h1>${escapeForHtml(text)}</body></html>`;
    const blob = new Blob([html], { type: "text/html" });
    window.open(URL.createObjectURL(blob), "_blank");
}

function handleStudentViewMaterial(material) {
    const files = material.files || [];
    if (files.length === 0) return;

    const allImages = files.every((f) => f.fileType && f.fileType.startsWith("image/"));
    if (allImages) {
        openStudentLightbox(files);
        return;
    }

    const first = files[0];
    if (first.linkUrl) {
        window.open(first.linkUrl, "_blank");
    } else if (first.textContent && !first.fileData) {
        openStudentTextInNewTab(first.textContent, material.title);
    } else if (first.fileData) {
        openStudentFileInNewTab(first.fileData);
    }
}

let studentLightboxFiles = [];
let studentLightboxIndex = 0;

function openStudentLightbox(files) {
    studentLightboxFiles = files || [];
    studentLightboxIndex = 0;
    if (studentLightboxFiles.length === 0) return;

    const lightbox = document.getElementById("studentLightbox");
    if (!lightbox) return;
    lightbox.classList.add("open");
    lightbox.setAttribute("aria-hidden", "false");
    renderStudentLightboxSlide();
}

function renderStudentLightboxSlide() {
    const file = studentLightboxFiles[studentLightboxIndex];
    const img = document.getElementById("studentLightboxImg");
    const countEl = document.getElementById("studentLightboxCount");
    const prevBtn = document.getElementById("studentLightboxPrev");
    const nextBtn = document.getElementById("studentLightboxNext");
    if (!file || !img || !countEl || !prevBtn || !nextBtn) return;

    img.src = file.fileData;
    countEl.textContent = studentLightboxFiles.length > 1 ? `${studentLightboxIndex + 1} / ${studentLightboxFiles.length}` : "";
    prevBtn.hidden = studentLightboxFiles.length <= 1;
    nextBtn.hidden = studentLightboxFiles.length <= 1;
}

function showStudentLightboxPrev() {
    if (studentLightboxFiles.length <= 1) return;
    studentLightboxIndex = (studentLightboxIndex - 1 + studentLightboxFiles.length) % studentLightboxFiles.length;
    renderStudentLightboxSlide();
}

function showStudentLightboxNext() {
    if (studentLightboxFiles.length <= 1) return;
    studentLightboxIndex = (studentLightboxIndex + 1) % studentLightboxFiles.length;
    renderStudentLightboxSlide();
}

function closeStudentLightbox() {
    const lightbox = document.getElementById("studentLightbox");
    if (!lightbox) return;
    lightbox.classList.remove("open");
    lightbox.setAttribute("aria-hidden", "true");
}

// 홈 화면 상단 사진 히어로 캐러셀 (자동 넘김 + 화살표/점 클릭)
let heroSlideIndex = 0;
let heroAutoTimer = null;

function goToHeroSlide(index) {
    const slides = document.querySelectorAll(".student-hero-slide");
    const dots = document.querySelectorAll(".student-hero-dot");
    if (slides.length === 0) return;

    heroSlideIndex = (index + slides.length) % slides.length;
    slides.forEach((s, i) => s.classList.toggle("active", i === heroSlideIndex));
    dots.forEach((d, i) => d.classList.toggle("active", i === heroSlideIndex));
}

function startHeroAutoplay() {
    stopHeroAutoplay();
    heroAutoTimer = setInterval(() => goToHeroSlide(heroSlideIndex + 1), 5000);
}

function stopHeroAutoplay() {
    if (heroAutoTimer) clearInterval(heroAutoTimer);
}

// 고정된 헤더(마스트헤드+서브내비)/푸터의 실제 높이를 재서 본문 여백에 반영
function syncKwzmFixedOffsets() {
    const masthead = document.querySelector(".student-masthead");
    const subnav = document.querySelector(".student-subnav");
    const footer = document.querySelector(".student-footer");
    const root = document.documentElement;

    if (masthead) root.style.setProperty("--kwzm-masthead-h", `${masthead.offsetHeight}px`);
    if (subnav) root.style.setProperty("--kwzm-subnav-h", `${subnav.offsetHeight}px`);
    if (footer) root.style.setProperty("--kwzm-footer-h", `${footer.offsetHeight}px`);
}

function setupHeroCarousel() {
    const carousel = document.getElementById("studentHeroCarousel");
    if (!carousel) return;

    document.getElementById("studentHeroPrev")?.addEventListener("click", () => {
        goToHeroSlide(heroSlideIndex - 1);
        startHeroAutoplay();
    });
    document.getElementById("studentHeroNext")?.addEventListener("click", () => {
        goToHeroSlide(heroSlideIndex + 1);
        startHeroAutoplay();
    });
    document.querySelectorAll(".student-hero-dot").forEach((dot, i) => {
        dot.addEventListener("click", () => {
            goToHeroSlide(i);
            startHeroAutoplay();
        });
    });

    goToHeroSlide(0);
    startHeroAutoplay();
}

document.addEventListener("fragments:loaded", () => {

    setupHeroCarousel();
    syncKwzmFixedOffsets();
    window.addEventListener("resize", syncKwzmFixedOffsets);

    // "로그아웃" 버튼
    document.getElementById("studentBackBtn")?.addEventListener("click", logoutStudent);

    // "OO 님" 클릭 → 마이페이지 화면으로 바로 이동
    document.getElementById("studentTopheaderName")?.addEventListener("click", () => {
        switchStudentMainTab("mypage");
    });

    document.getElementById("toggleStudentLoginPwBtn")?.addEventListener("click", () => {
        const input = document.getElementById("studentLoginPassword");
        const btn = document.getElementById("toggleStudentLoginPwBtn");
        const isVisible = input.type === "text";
        input.type = isVisible ? "password" : "text";
        btn.classList.toggle("is-active", !isVisible);
    });

    document.getElementById("studentLoginSubmitBtn")?.addEventListener("click", async () => {
        const studentNumber = document.getElementById("studentLoginNumber").value.trim();
        const username = document.getElementById("studentLoginUsername").value.trim();
        const password = document.getElementById("studentLoginPassword").value;
        const email = document.getElementById("studentLoginEmail").value.trim();
        const errorEl = document.getElementById("studentLoginError");
        const submitBtn = document.getElementById("studentLoginSubmitBtn");

        if (!studentNumber || !username || !password || !email) {
            errorEl.textContent = "모든 항목을 입력해주세요.";
            errorEl.hidden = false;
            return;
        }
        errorEl.hidden = true;
        submitBtn.disabled = true;
        submitBtn.textContent = "확인 중...";

        try {
            const res = await fetch("/api/student/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password, email, studentNumber }),
            });

            if (!res.ok) {
                errorEl.textContent = (await res.text()) || "로그인에 실패했어요.";
                errorEl.hidden = false;
                return;
            }

            document.getElementById("studentLoginNumber").value = "";
            document.getElementById("studentLoginUsername").value = "";
            document.getElementById("studentLoginPassword").value = "";
            document.getElementById("studentLoginEmail").value = "";
            showStudentScreen();
            loadStudentPortalData();
        } catch (err) {
            console.error(err);
            errorEl.textContent = "서버에 연결할 수 없어요.";
            errorEl.hidden = false;
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = "들어가기";
        }
    });

    // 홈/언어/영상/Post/커뮤니티 탭 전환
    document.querySelectorAll(".student-subnav-link").forEach((tab) => {
        tab.addEventListener("click", () => switchStudentMainTab(tab.dataset.studentMainTab));
    });

    // 홈/마이페이지 화면의 바로가기 버튼
    document.addEventListener("click", (e) => {
        const shortcut = e.target.closest("[data-home-shortcut]");
        if (!shortcut) return;
        switchStudentMainTab(shortcut.dataset.homeShortcut);
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeStudentLightbox();
        }
        if (document.getElementById("studentLightbox")?.classList.contains("open")) {
            if (e.key === "ArrowLeft") showStudentLightboxPrev();
            if (e.key === "ArrowRight") showStudentLightboxNext();
        }
    });

    document.addEventListener("click", (e) => {
        if (e.target.closest("[data-student-lightbox-close]")) closeStudentLightbox();
    });

    document.getElementById("studentLightboxPrev")?.addEventListener("click", showStudentLightboxPrev);
    document.getElementById("studentLightboxNext")?.addEventListener("click", showStudentLightboxNext);

    checkStudentSessionOnLoad();
});