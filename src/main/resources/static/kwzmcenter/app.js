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
const COMPUTER_PROGRAM_LABEL = { BASIC: "Basic", WORD: "Word", EXCEL: "Excel", POWERPOINT: "PowerPoint", PAGEMAKER: "PageMaker", PHOTOSHOP: "Photoshop" };

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

    const enrolledLanguages = new Set(studentCourses.map((c) => c.language));
    pillsEl.innerHTML = "";

    Object.keys(STUDENT_LANGUAGE_LABEL).forEach((lang) => {
        const isEnrolled = enrolledLanguages.has(lang);
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "student-lang-sidebar-item";
        btn.classList.toggle("is-locked", !isEnrolled);
        btn.dataset.studentLang = lang;
        btn.textContent = STUDENT_LANGUAGE_LABEL[lang] || lang;
        btn.addEventListener("click", () => {
            pillsEl.querySelectorAll(".student-lang-sidebar-item").forEach((p) => p.classList.remove("active"));
            btn.classList.add("active");

            const catEl = document.getElementById("studentCategoryPills");
            const view = document.getElementById("studentMaterialsView");
            const notEnrolledEl = document.getElementById("studentLangNotEnrolled");

            if (isEnrolled) {
                if (notEnrolledEl) notEnrolledEl.hidden = true;
                renderStudentCategoryPills(lang);
            } else {
                if (catEl) { catEl.hidden = true; catEl.innerHTML = ""; }
                if (view) view.hidden = true;
                if (notEnrolledEl) notEnrolledEl.hidden = false;
            }
        });
        pillsEl.appendChild(btn);
    });

    // 신청한 언어가 있으면 그걸 먼저 보여주고, 없으면 그냥 첫 번째 언어를 보여줘요.
    const firstEnrolledPill = pillsEl.querySelector(".student-lang-sidebar-item:not(.is-locked)");
    const firstPill = firstEnrolledPill || pillsEl.querySelector(".student-lang-sidebar-item");
    if (firstPill) firstPill.click();
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

    const firstCatPill = catEl.querySelector(".student-pill");
    if (firstCatPill) firstCatPill.click();
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

            const typeLabels = [];
            if (files.some((f) => f.fileType && f.fileType.startsWith("image/"))) typeLabels.push("이미지");
            if (files.some((f) => f.fileData && !(f.fileType && f.fileType.startsWith("image/")))) typeLabels.push("파일");
            if (files.some((f) => f.linkUrl)) typeLabels.push("링크");
            if (files.some((f) => f.textContent && !f.linkUrl && !f.fileData)) typeLabels.push("글");
            const typeBadgeHtml = typeLabels.length
                ? typeLabels.map((t) => `<span class="student-material-type-badge" data-type="${t}">${t}</span>`).join("")
                : `<span class="student-material-type-badge" data-type="없음">없음</span>`;

            const descHtml = m.description
                ? escapeHtmlForStudent(m.description)
                : `<span class="student-material-desc-empty">설명 없음</span>`;

            item.innerHTML = `
        <div class="student-material-thumb">${thumbHtml}${countBadgeHtml}</div>
        <div class="student-material-main">
          <p class="student-material-title">${escapeHtmlForStudent(m.title)}</p>
          <p class="student-material-desc">${descHtml}</p>
        </div>
        <div class="student-material-types">${typeBadgeHtml}</div>
        <p class="student-material-date">${m.createdAt}</p>
      `;
            item.addEventListener("click", () => handleStudentViewMaterial(m));
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

const VIDEO_TOPIC_LABEL = { korean: "한국어", japanese: "일본어", thai: "태국어", english: "영어", computer: "컴퓨터" };

async function loadVideoMaterials(topic) {
    const emptyText = document.getElementById("videoMaterialsEmpty");
    const lockedText = document.getElementById("videoMaterialsLocked");
    const list = document.getElementById("videoMaterialsList");
    const heading = document.getElementById("videoMaterialsHeading");
    if (!list) return;

    if (lockedText) lockedText.hidden = true;
    if (emptyText) emptyText.hidden = true;
    if (heading) heading.textContent = VIDEO_TOPIC_LABEL[topic] || topic;
    list.innerHTML = "";

    try {
        const res = await fetch(`/api/student/videos?topic=${topic}`);
        if (!res.ok) {
            if (res.status === 403 && lockedText) lockedText.hidden = false;
            return;
        }
        const materials = await res.json();

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

            const typeLabels = [];
            if (files.some((f) => f.fileType && f.fileType.startsWith("image/"))) typeLabels.push("이미지");
            if (files.some((f) => f.fileData && !(f.fileType && f.fileType.startsWith("image/")))) typeLabels.push("파일");
            if (files.some((f) => f.linkUrl)) typeLabels.push("링크");
            if (files.some((f) => f.textContent && !f.linkUrl && !f.fileData)) typeLabels.push("글");
            const typeBadgeHtml = typeLabels.length
                ? typeLabels.map((t) => `<span class="student-material-type-badge" data-type="${t}">${t}</span>`).join("")
                : `<span class="student-material-type-badge" data-type="없음">없음</span>`;

            const descHtml = m.description
                ? escapeHtmlForStudent(m.description)
                : `<span class="student-material-desc-empty">설명 없음</span>`;

            item.innerHTML = `
        <div class="student-material-thumb">${thumbHtml}${countBadgeHtml}</div>
        <div class="student-material-main">
          <p class="student-material-title">${escapeHtmlForStudent(m.title)}</p>
          <p class="student-material-desc">${descHtml}</p>
        </div>
        <div class="student-material-types">${typeBadgeHtml}</div>
        <p class="student-material-date">${m.createdAt}</p>
      `;
            item.addEventListener("click", () => handleStudentViewMaterial(m));
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

const TRIAL_TOPIC_LABEL = { korean: "한국어", japanese: "일본어", thai: "태국어", english: "영어", computer: "컴퓨터", video: "영상" };

async function loadTrialMaterials(topic) {
    const emptyText = document.getElementById("trialMaterialsEmpty");
    const list = document.getElementById("trialMaterialsList");
    const heading = document.getElementById("trialMaterialsHeading");
    if (!list) return;

    if (emptyText) emptyText.hidden = true;
    if (heading) heading.textContent = TRIAL_TOPIC_LABEL[topic] || topic;
    list.innerHTML = "";

    try {
        const res = await fetch(`/api/student/trial?topic=${topic}`);
        if (!res.ok) return;
        const materials = await res.json();

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

            const typeLabels = [];
            if (files.some((f) => f.fileType && f.fileType.startsWith("image/"))) typeLabels.push("이미지");
            if (files.some((f) => f.fileData && !(f.fileType && f.fileType.startsWith("image/")))) typeLabels.push("파일");
            if (files.some((f) => f.linkUrl)) typeLabels.push("링크");
            if (files.some((f) => f.textContent && !f.linkUrl && !f.fileData)) typeLabels.push("글");
            const typeBadgeHtml = typeLabels.length
                ? typeLabels.map((t) => `<span class="student-material-type-badge" data-type="${t}">${t}</span>`).join("")
                : `<span class="student-material-type-badge" data-type="없음">없음</span>`;

            const descHtml = m.description
                ? escapeHtmlForStudent(m.description)
                : `<span class="student-material-desc-empty">설명 없음</span>`;

            item.innerHTML = `
        <div class="student-material-thumb">${thumbHtml}${countBadgeHtml}</div>
        <div class="student-material-main">
          <p class="student-material-title">${escapeHtmlForStudent(m.title)}</p>
          <p class="student-material-desc">${descHtml}</p>
        </div>
        <div class="student-material-types">${typeBadgeHtml}</div>
        <p class="student-material-date">${m.createdAt}</p>
      `;
            item.addEventListener("click", () => handleStudentViewMaterial(m));
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

async function loadComputerMaterials(category) {
    const emptyText = document.getElementById("computerMaterialsEmpty");
    const lockedText = document.getElementById("computerMaterialsLocked");
    const list = document.getElementById("computerMaterialsList");
    const heading = document.getElementById("computerMaterialsHeading");
    if (!list) return;

    if (lockedText) lockedText.hidden = true;
    if (emptyText) emptyText.hidden = true;
    if (heading) heading.textContent = COMPUTER_PROGRAM_LABEL[category] || category;
    list.innerHTML = "";

    try {
        const res = await fetch(`/api/student/materials?language=computer&category=${category}`);
        if (!res.ok) {
            if (res.status === 403 && lockedText) lockedText.hidden = false;
            return;
        }
        const materials = await res.json();

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

            const typeLabels = [];
            if (files.some((f) => f.fileType && f.fileType.startsWith("image/"))) typeLabels.push("이미지");
            if (files.some((f) => f.fileData && !(f.fileType && f.fileType.startsWith("image/")))) typeLabels.push("파일");
            if (files.some((f) => f.linkUrl)) typeLabels.push("링크");
            if (files.some((f) => f.textContent && !f.linkUrl && !f.fileData)) typeLabels.push("글");
            const typeBadgeHtml = typeLabels.length
                ? typeLabels.map((t) => `<span class="student-material-type-badge" data-type="${t}">${t}</span>`).join("")
                : `<span class="student-material-type-badge" data-type="없음">없음</span>`;

            const descHtml = m.description
                ? escapeHtmlForStudent(m.description)
                : `<span class="student-material-desc-empty">설명 없음</span>`;

            item.innerHTML = `
        <div class="student-material-thumb">${thumbHtml}${countBadgeHtml}</div>
        <div class="student-material-main">
          <p class="student-material-title">${escapeHtmlForStudent(m.title)}</p>
          <p class="student-material-desc">${descHtml}</p>
        </div>
        <div class="student-material-types">${typeBadgeHtml}</div>
        <p class="student-material-date">${m.createdAt}</p>
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

// ---- 게시판: 주제/항목별로 학생들이 글을 올리고 볼 수 있는 공간 ----

const BOARD_TOPIC_LABEL = { korean: "한국어", japanese: "일본어", thai: "태국어", english: "영어", computer: "컴퓨터" };
const BOARD_CATEGORY_LABEL = { VOCAB: "어휘", GRAMMAR: "문법", WRITING: "쓰기", OTHER: "기타" };

const ICON_LIKE = `<svg class="board-reaction-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M7 11v9H4a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1h3Zm0 0 4.5-8a2 2 0 0 1 2.7 2.7L13 9h5.2a2 2 0 0 1 1.98 2.28l-1 7A2 2 0 0 1 17.2 20H10a3 3 0 0 1-3-3v-6Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`;
const ICON_DISLIKE = `<svg class="board-reaction-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M17 13V4h3a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1h-3Zm0 0-4.5 8a2 2 0 0 1-2.7-2.7L11 15H5.8a2 2 0 0 1-1.98-2.28l1-7A2 2 0 0 1 6.8 4H14a3 3 0 0 1 3 3v6Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`;
const ICON_COMMENT = `<svg class="board-reaction-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 5h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H9l-4.5 3.5a.5.5 0 0 1-.8-.4V17H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`;

let currentBoardTopic = "korean";
let currentBoardCategory = "VOCAB";
let editingPostId = null;

function renderBoardTopics() {
    const el = document.getElementById("boardTopics");
    if (!el) return;

    el.innerHTML = "";
    Object.keys(BOARD_TOPIC_LABEL).forEach((topic) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "board-sidebar-item";
        btn.dataset.boardTopic = topic;
        btn.textContent = BOARD_TOPIC_LABEL[topic];
        btn.addEventListener("click", () => {
            el.querySelectorAll(".board-sidebar-item").forEach((p) => p.classList.remove("active"));
            btn.classList.add("active");
            currentBoardTopic = topic;
            renderBoardCategoryPills(topic);
        });
        el.appendChild(btn);
    });

    const firstTopic = el.querySelector(".board-sidebar-item");
    if (firstTopic) firstTopic.click();
}

function renderBoardCategoryPills(topic) {
    const el = document.getElementById("boardCategoryPills");
    if (!el) return;

    if (topic === "computer") {
        el.hidden = true;
        el.innerHTML = "";
        currentBoardCategory = "ALL";
        loadBoardPosts(topic, "ALL");
        return;
    }

    el.hidden = false;
    el.innerHTML = "";
    Object.entries(BOARD_CATEGORY_LABEL).forEach(([key, label]) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "board-pill";
        btn.textContent = label;
        btn.addEventListener("click", () => {
            el.querySelectorAll(".board-pill").forEach((p) => p.classList.remove("active"));
            btn.classList.add("active");
            currentBoardCategory = key;
            loadBoardPosts(topic, key);
        });
        el.appendChild(btn);
    });

    const firstCat = el.querySelector(".board-pill");
    if (firstCat) firstCat.click();
}

async function loadBoardPosts(topic, category) {
    const list = document.getElementById("boardList");
    const emptyText = document.getElementById("boardEmpty");
    const heading = document.getElementById("boardHeading");
    if (!list) return;

    if (heading) {
        heading.textContent = category === "ALL"
            ? `${BOARD_TOPIC_LABEL[topic] || topic}`
            : `${BOARD_TOPIC_LABEL[topic] || topic} · ${BOARD_CATEGORY_LABEL[category] || category}`;
    }

    try {
        const query = category === "ALL"
            ? `topic=${topic}`
            : `topic=${topic}&category=${category}`;
        const res = await fetch(`/api/student/posts?${query}`);
        if (!res.ok) return;
        const posts = await res.json();

        list.innerHTML = "";
        if (emptyText) emptyText.hidden = posts.length > 0;

        posts.forEach((p) => {
            const item = document.createElement("div");
            item.className = "board-item";
            const initial = (p.nickname || "?").charAt(0);
            const badgeHtml = p.category
                ? `<span class="board-item-badge">${escapeHtmlForStudent(BOARD_CATEGORY_LABEL[p.category] || p.category)}</span>`
                : "";
            item.innerHTML = `
        <div class="board-item-header">
          <span class="board-item-avatar">${escapeHtmlForStudent(initial)}</span>
          <span class="board-item-name">${escapeHtmlForStudent(p.nickname)}</span>
          ${badgeHtml}
          <span class="board-item-date">${p.createdAt}</span>
        </div>
        <p class="board-item-title">${escapeHtmlForStudent(p.title)}</p>
        <p class="board-item-preview">${escapeHtmlForStudent(p.content)}</p>
        <p class="board-item-content" hidden>${escapeHtmlForStudent(p.content)}</p>
        <div class="board-item-footer">
          <button type="button" class="board-reaction-btn ${p.myReaction === "LIKE" ? "is-active" : ""}" data-reaction="LIKE">
            ${ICON_LIKE}<span>${p.likeCount ?? 0}</span>
          </button>
          <button type="button" class="board-reaction-btn ${p.myReaction === "DISLIKE" ? "is-active" : ""}" data-reaction="DISLIKE">
            ${ICON_DISLIKE}<span>${p.dislikeCount ?? 0}</span>
          </button>
          <button type="button" class="board-comment-toggle-btn">
            ${ICON_COMMENT}<span>${p.commentCount ?? 0}</span>
          </button>
        </div>
        <div class="board-comments" hidden>
          <div class="board-comments-list"></div>
          <div class="board-comment-form">
            <input type="text" class="board-comment-input" placeholder="댓글을 남겨보세요">
            <button type="button" class="board-comment-submit">등록</button>
          </div>
        </div>
      `;
            item.addEventListener("click", () => {
                const preview = item.querySelector(".board-item-preview");
                const full = item.querySelector(".board-item-content");
                if (!preview || !full) return;
                const isOpen = !full.hidden;
                full.hidden = isOpen;
                preview.hidden = !isOpen;
            });

            const footer = item.querySelector(".board-item-footer");
            footer.addEventListener("click", (e) => e.stopPropagation());

            item.querySelector('.board-reaction-btn[data-reaction="LIKE"]').addEventListener("click", () => {
                submitBoardReaction(p.id, "LIKE", item);
            });
            item.querySelector('.board-reaction-btn[data-reaction="DISLIKE"]').addEventListener("click", () => {
                submitBoardReaction(p.id, "DISLIKE", item);
            });

            const commentsPanel = item.querySelector(".board-comments");
            commentsPanel.addEventListener("click", (e) => e.stopPropagation());

            item.querySelector(".board-comment-toggle-btn").addEventListener("click", () => {
                toggleBoardComments(p.id, commentsPanel);
            });

            const commentInput = item.querySelector(".board-comment-input");
            const commentSubmitBtn = item.querySelector(".board-comment-submit");
            const submitNewComment = () => submitBoardComment(p.id, item, commentInput, commentSubmitBtn);
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

// 좋아요/싫어요 — 같은 걸 다시 누르면 취소, 다른 걸 누르면 전환 (서버가 최신 상태를 돌려줌)
async function submitBoardReaction(postId, type, itemEl) {
    try {
        const res = await fetch(`/api/student/posts/${postId}/reaction`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ type }),
        });
        if (!res.ok) return;
        const updated = await res.json();

        const likeBtn = itemEl.querySelector('.board-reaction-btn[data-reaction="LIKE"]');
        const dislikeBtn = itemEl.querySelector('.board-reaction-btn[data-reaction="DISLIKE"]');
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

async function toggleBoardComments(postId, panel) {
    if (!panel) return;
    if (!panel.hidden) {
        panel.hidden = true;
        return;
    }
    panel.hidden = false;
    await loadBoardComments(postId, panel);
}

async function loadBoardComments(postId, panel) {
    const listEl = panel.querySelector(".board-comments-list");
    if (!listEl) return;
    listEl.innerHTML = `<p class="board-comments-hint">불러오는 중...</p>`;

    try {
        const res = await fetch(`/api/student/posts/${postId}/comments`);
        if (!res.ok) return;
        const comments = await res.json();

        listEl.innerHTML = "";
        if (comments.length === 0) {
            listEl.innerHTML = `<p class="board-comments-hint">아직 댓글이 없어요. 첫 댓글을 남겨보세요!</p>`;
            return;
        }
        comments.forEach((c) => {
            const row = document.createElement("div");
            row.className = c.isAdmin ? "board-comment-item board-comment-item--admin" : "board-comment-item";
            const adminBadgeHtml = c.isAdmin ? `<span class="board-comment-admin-badge">관리자 댓글</span>` : "";
            row.innerHTML = `
        <span class="board-comment-name">${escapeHtmlForStudent(c.nickname)}</span>
        ${adminBadgeHtml}
        <span class="board-comment-text">${escapeHtmlForStudent(c.content)}</span>
        <span class="board-comment-date">${c.createdAt}</span>
      `;
            listEl.appendChild(row);
        });
    } catch (err) {
        console.error(err);
    }
}

async function submitBoardComment(postId, itemEl, inputEl, submitBtn) {
    const content = inputEl.value.trim();
    if (!content) return;

    submitBtn.disabled = true;
    try {
        const res = await fetch(`/api/student/posts/${postId}/comments`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ content }),
        });
        if (!res.ok) return;

        inputEl.value = "";
        const panel = itemEl.querySelector(".board-comments");
        await loadBoardComments(postId, panel);

        const commentCountEl = itemEl.querySelector(".board-comment-toggle-btn span:last-child");
        if (commentCountEl) commentCountEl.textContent = String(Number(commentCountEl.textContent || 0) + 1);
    } catch (err) {
        console.error(err);
    } finally {
        submitBtn.disabled = false;
    }
}

function updateBoardCategoryFieldVisibility() {
    const topic = document.getElementById("boardFormTopic").value;
    const field = document.getElementById("boardFormCategoryField");
    if (field) field.hidden = topic === "computer";
}

// post가 주어지면 "수정" 모드로, 없으면 "새 글쓰기" 모드로 열려요.
function openBoardWriteModal(post) {
    const modal = document.getElementById("boardWriteModal");
    if (!modal) return;

    editingPostId = post ? post.id : null;

    const titleEl = document.getElementById("boardModalTitle");
    const topicEl = document.getElementById("boardFormTopic");
    const submitBtn = document.getElementById("boardFormSubmitBtn");

    topicEl.value = post ? post.topic : currentBoardTopic;
    topicEl.disabled = !!post; // 수정할 때는 주제를 바꿀 수 없어요
    document.getElementById("boardFormCategory").value = post
        ? (BOARD_CATEGORY_LABEL[post.category] ? post.category : "VOCAB")
        : (BOARD_CATEGORY_LABEL[currentBoardCategory] ? currentBoardCategory : "VOCAB");
    document.getElementById("boardFormTitle").value = post ? post.title : "";
    document.getElementById("boardFormContent").value = post ? post.content : "";
    document.getElementById("boardFormError").hidden = true;
    if (titleEl) titleEl.textContent = post ? "글 수정" : "글쓰기";
    if (submitBtn) submitBtn.textContent = post ? "수정하기" : "올리기";

    updateBoardCategoryFieldVisibility();
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

function closeBoardWriteModal() {
    const modal = document.getElementById("boardWriteModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    editingPostId = null;
    const topicEl = document.getElementById("boardFormTopic");
    if (topicEl) topicEl.disabled = false;
}

async function submitBoardPost() {
    const topic = document.getElementById("boardFormTopic").value;
    const category = topic === "computer" ? null : document.getElementById("boardFormCategory").value;
    const title = document.getElementById("boardFormTitle").value.trim();
    const content = document.getElementById("boardFormContent").value.trim();
    const errorEl = document.getElementById("boardFormError");
    const submitBtn = document.getElementById("boardFormSubmitBtn");
    const isEditing = !!editingPostId;

    if (!title || !content) {
        errorEl.textContent = "제목과 내용을 모두 입력해주세요.";
        errorEl.hidden = false;
        return;
    }
    errorEl.hidden = true;
    submitBtn.disabled = true;
    submitBtn.textContent = isEditing ? "수정하는 중..." : "올리는 중...";

    try {
        const url = isEditing ? `/api/student/posts/${editingPostId}` : "/api/student/posts";
        const method = isEditing ? "PUT" : "POST";
        const body = isEditing ? { category, title, content } : { topic, category, title, content };

        const res = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });

        if (!res.ok) {
            errorEl.textContent = (await res.text()) || (isEditing ? "수정에 실패했어요." : "글 등록에 실패했어요.");
            errorEl.hidden = false;
            return;
        }

        closeBoardWriteModal();

        if (isEditing) {
            renderMyPosts();
        } else {
            const refreshCategory = topic === "computer" ? "ALL" : category;
            if (topic === currentBoardTopic && refreshCategory === currentBoardCategory) {
                loadBoardPosts(topic, refreshCategory);
            }
        }
    } catch (err) {
        console.error(err);
        errorEl.textContent = "서버에 연결할 수 없어요.";
        errorEl.hidden = false;
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = isEditing ? "수정하기" : "올리기";
    }
}

// 마이페이지 "내가 쓴 글"
async function renderMyPosts() {
    const listEl = document.getElementById("mypagePostsList");
    const emptyEl = document.getElementById("mypagePostsEmpty");
    if (!listEl) return;

    try {
        const res = await fetch("/api/student/posts/mine");
        if (!res.ok) return;
        const posts = await res.json();

        listEl.innerHTML = "";
        if (emptyEl) emptyEl.hidden = posts.length > 0;

        posts.forEach((p) => {
            const item = document.createElement("div");
            item.className = "mypost-item";
            item.innerHTML = `
        <div class="mypost-item-head">
          <span class="mypost-topic">${escapeHtmlForStudent(BOARD_TOPIC_LABEL[p.topic] || p.topic)}${p.category ? " · " + escapeHtmlForStudent(BOARD_CATEGORY_LABEL[p.category] || p.category) : ""}</span>
          <span class="mypost-date">${p.createdAt}</span>
        </div>
        <p class="mypost-title">${escapeHtmlForStudent(p.title)}</p>
        <p class="mypost-preview">${escapeHtmlForStudent(p.content)}</p>
        <div class="mypost-actions">
          <button type="button" class="mypost-action-btn" data-mypost-edit>수정</button>
          <button type="button" class="mypost-action-btn mypost-action-btn--danger" data-mypost-delete>삭제</button>
        </div>
      `;
            item.querySelector("[data-mypost-edit]").addEventListener("click", () => openBoardWriteModal(p));
            item.querySelector("[data-mypost-delete]").addEventListener("click", () => deleteMyPost(p.id));
            listEl.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

async function deleteMyPost(id) {
    if (!confirm("이 글을 삭제할까요? 되돌릴 수 없어요.")) return;

    try {
        const res = await fetch(`/api/student/posts/${id}`, { method: "DELETE" });
        if (!res.ok) {
            alert((await res.text()) || "삭제에 실패했어요.");
            return;
        }
        renderMyPosts();
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    }
}

// 사진 배너 상단 슬라이드쇼 (언어/영상 등 여러 화면에서 재사용, 화면마다 각자 독립적으로 넘어감)
function setupPhotoBanners() {
    document.querySelectorAll(".student-photo-banner").forEach((banner) => {
        const slides = banner.querySelectorAll(".student-photo-banner-slide");
        const dots = banner.querySelectorAll(".student-photo-banner-dot");
        if (slides.length <= 1) return;

        let index = 0;
        let timer = null;

        const goTo = (i) => {
            index = (i + slides.length) % slides.length;
            slides.forEach((s, idx) => s.classList.toggle("is-active", idx === index));
            dots.forEach((d, idx) => d.classList.toggle("is-active", idx === index));
        };

        const restart = () => {
            if (timer) clearInterval(timer);
            timer = setInterval(() => goTo(index + 1), 5000);
        };

        dots.forEach((dot) => {
            dot.addEventListener("click", () => {
                goTo(Number(dot.dataset.slideIndex));
                restart();
            });
        });

        const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        if (!prefersReducedMotion) restart();
    });
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
    setupPhotoBanners();
    renderBoardTopics();
    if (document.getElementById("computerMaterialsList")) loadComputerMaterials("BASIC");
    if (document.getElementById("videoMaterialsList")) loadVideoMaterials("korean");
    if (document.getElementById("trialMaterialsList")) loadTrialMaterials("korean");
    syncKwzmFixedOffsets();
    window.addEventListener("resize", syncKwzmFixedOffsets);

    document.getElementById("boardWriteBtn")?.addEventListener("click", () => openBoardWriteModal());
    document.addEventListener("click", (e) => {
        if (e.target.closest("[data-board-modal-close]")) closeBoardWriteModal();
    });
    document.getElementById("boardFormSubmitBtn")?.addEventListener("click", submitBoardPost);
    document.getElementById("boardFormTopic")?.addEventListener("change", updateBoardCategoryFieldVisibility);

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

    // "온라인 영상" 화면의 언어/컴퓨터 탭 전환
    document.addEventListener("click", (e) => {
        const tab = e.target.closest("[data-video-tab]");
        if (!tab) return;
        const key = tab.dataset.videoTab;

        document.querySelectorAll("[data-video-tab]").forEach((t) => {
            t.classList.toggle("active", t === tab);
            t.setAttribute("aria-selected", t === tab ? "true" : "false");
        });
        loadVideoMaterials(key);
    });

    // "무료체험" 화면의 언어/컴퓨터/영상 탭 전환
    document.addEventListener("click", (e) => {
        const tab = e.target.closest("[data-trial-tab]");
        if (!tab) return;
        const key = tab.dataset.trialTab;

        document.querySelectorAll("[data-trial-tab]").forEach((t) => {
            t.classList.toggle("active", t === tab);
            t.setAttribute("aria-selected", t === tab ? "true" : "false");
        });
        loadTrialMaterials(key);
    });

    // "컴퓨터 자료" 화면의 프로그램별 탭 전환
    document.addEventListener("click", (e) => {
        const tab = e.target.closest("[data-computer-tab]");
        if (!tab) return;
        const key = tab.dataset.computerTab;

        document.querySelectorAll("[data-computer-tab]").forEach((t) => {
            t.classList.toggle("active", t === tab);
            t.setAttribute("aria-selected", t === tab ? "true" : "false");
        });
        loadComputerMaterials(key);
    });

    // "마이페이지"의 내 수강 정보/바로가기 탭 전환
    document.addEventListener("click", (e) => {
        const tab = e.target.closest("[data-mypage-tab]");
        if (!tab) return;
        const key = tab.dataset.mypageTab;

        document.querySelectorAll("[data-mypage-tab]").forEach((t) => {
            t.classList.toggle("active", t === tab);
            t.setAttribute("aria-selected", t === tab ? "true" : "false");
        });
        document.querySelectorAll("[data-mypage-panel]").forEach((p) => {
            p.hidden = p.dataset.mypagePanel !== key;
            p.classList.toggle("active", p.dataset.mypagePanel === key);
        });

        if (key === "myposts") renderMyPosts();
    });

    // "합격증 다운로드" / "시험 보러가기" — 관리자가 나중에 보내주는 기능이라 지금은 안내만
    document.addEventListener("click", (e) => {
        const btn = e.target.closest("[data-mypage-action]");
        if (!btn) return;
        const action = btn.dataset.mypageAction;
        if (action === "certificate") {
            alert("합격증은 관리자가 발급하면 여기에서 다운로드할 수 있어요.");
        } else if (action === "exam") {
            alert("시험은 관리자가 열어주면 여기에서 볼 수 있어요.");
        }
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