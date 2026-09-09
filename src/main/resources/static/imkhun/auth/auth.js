// 회원가입 / 로그인 / 계정 찾기 / 마이페이지
//
// 주의: 이 화면들의 HTML은 auth/auth.html 파일에서 fetch로 나중에 불러와 붙여요.
// 그래서 헤더에 있는 버튼(회원가입/로그인/프로필)처럼 처음부터 페이지에 있는 요소가 아니라면,
// 반드시 fragments:loaded 이벤트가 뜬 뒤에(= auth.html이 다 붙은 뒤에) 찾아야 해요.
// 그 전에 document.getElementById로 찾으면 아직 없어서 null이 나와요.

// ---------- 로그인 상태 확인 (헤더는 항상 있으니 바로 실행) ----------

let isSiteUserLoggedIn = false;

// 회원가입/로그인/비밀번호 변경 등 방금 한 행동에 대한 짧은 알림(토스트) — 알림 목록에는 안 쌓이고 화면에 잠깐 떴다가 사라짐
function showSiteToast(message, type = "success") {
    const container = document.getElementById("siteToastContainer");
    if (!container) return;

    const toast = document.createElement("div");
    toast.className = `site-toast site-toast--${type}`;
    const iconHtml = type === "success"
        ? `<svg viewBox="0 0 24 24" fill="none"><path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`
        : `<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6"/><path d="M12 8v5M12 16h.01" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`;
    toast.innerHTML = `<span class="site-toast-icon">${iconHtml}</span><span>${message}</span>`;
    container.appendChild(toast);

    requestAnimationFrame(() => toast.classList.add("is-visible"));
    setTimeout(() => {
        toast.classList.remove("is-visible");
        setTimeout(() => toast.remove(), 250);
    }, 3200);
}

async function checkAuthState() {
    const guest = document.getElementById("authGuest");
    const user = document.getElementById("authUser");
    const avatar = document.getElementById("authProfileAvatar");
    const nameEl = document.getElementById("authProfileName");
    if (!guest || !user) return;

    try {
        const res = await fetch("/api/auth/me");
        if (!res.ok) throw new Error("not logged in");

        const data = await res.json();
        if (data.profileImage) {
            avatar.innerHTML = `<img src="${data.profileImage}" alt="">`;
        } else {
            avatar.textContent = (data.nickname || "?").charAt(0);
        }
        nameEl.textContent = data.nickname;

        guest.hidden = true;
        user.hidden = false;
        isSiteUserLoggedIn = true;
        loadSiteNotifUnreadCount();
        if (!siteNotifPollTimer) {
            siteNotifPollTimer = setInterval(loadSiteNotifUnreadCount, 30000);
        }
    } catch {
        guest.hidden = false;
        user.hidden = true;
        isSiteUserLoggedIn = false;
        clearInterval(siteNotifPollTimer);
        siteNotifPollTimer = null;
        const badge = document.getElementById("siteNotifBadge");
        if (badge) badge.hidden = true;
    }
}

let siteNotifPollTimer = null;

const SITE_NOTIF_TYPE_ICON = {
    COMMENT_REPLY: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 5h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H9l-4.5 3.5a.5.5 0 0 1-.8-.4V17H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`,
    POST_COMMENT: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 5h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H9l-4.5 3.5a.5.5 0 0 1-.8-.4V17H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`,
    ADMIN_COMMENT: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 3 2 8l10 5 10-5-10-5Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M6 10.5V16c0 1.5 2.7 3 6 3s6-1.5 6-3v-5.5" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`,
    APPLICATION_APPROVED: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
    COURSE_CHANGED: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M6 4h9l4 4v12H6V4Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M9 12h6M9 16h6M9 8h3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`,
    PAYMENT_INFO_REGISTERED: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="3" y="6" width="18" height="13" rx="2" stroke="currentColor" stroke-width="1.6"/><path d="M3 10h18M7 15h4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`,
    PAYMENT_CONFIRMED_BY_ADMIN: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
    ATTENDANCE_ABSENT: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6"/><path d="M15 9l-6 6M9 9l6 6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`,
    REVIEW_REPLY: `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m12 3 2.6 5.9 6.4.6-4.8 4.3 1.4 6.3L12 17l-5.6 3.1 1.4-6.3-4.8-4.3 6.4-.6L12 3Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>`,
};
const SITE_NOTIF_ICON_DEFAULT = `<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6"/><path d="M12 8v5M12 16h.01" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`;

async function loadSiteNotifUnreadCount() {
    const badge = document.getElementById("siteNotifBadge");
    if (!badge) return;
    try {
        const res = await fetch("/api/mypage/notifications/unread-count");
        if (!res.ok) return;
        const count = await res.json();
        badge.textContent = count > 99 ? "99+" : String(count);
        badge.hidden = count === 0;
    } catch (err) {
        console.error(err);
    }
}

async function toggleSiteNotifPanel() {
    const panel = document.getElementById("siteNotifPanel");
    const btn = document.getElementById("siteNotifBtn");
    if (!panel || !btn) return;

    const willOpen = panel.hidden;
    if (willOpen) {
        const rect = btn.getBoundingClientRect();
        panel.style.top = `${rect.bottom + 10}px`;
        panel.style.right = `${window.innerWidth - rect.right}px`;
    }
    panel.hidden = !willOpen;
    btn.setAttribute("aria-expanded", willOpen ? "true" : "false");
    if (!willOpen) return;

    const readAllBtn = document.getElementById("siteNotifReadAllBtn");
    if (!isSiteUserLoggedIn) {
        if (readAllBtn) readAllBtn.hidden = true;
        const listEl = document.getElementById("siteNotifList");
        if (listEl) listEl.innerHTML = `<p class="site-notif-hint">로그인하면 알림을 확인할 수 있어요.</p>`;
        return;
    }
    if (readAllBtn) readAllBtn.hidden = false;
    await loadSiteNotifList();
}

function closeSiteNotifPanel() {
    const panel = document.getElementById("siteNotifPanel");
    const btn = document.getElementById("siteNotifBtn");
    if (panel) panel.hidden = true;
    if (btn) btn.setAttribute("aria-expanded", "false");
}

async function loadSiteNotifList() {
    const listEl = document.getElementById("siteNotifList");
    if (!listEl) return;
    listEl.innerHTML = `<p class="site-notif-hint">불러오는 중...</p>`;

    try {
        const res = await fetch("/api/mypage/notifications");
        if (!res.ok) return;
        const notifications = await res.json();

        listEl.innerHTML = "";
        if (notifications.length === 0) {
            listEl.innerHTML = `<p class="site-notif-hint">아직 알림이 없어요.</p>`;
            return;
        }

        notifications.forEach((n) => {
            const item = document.createElement("button");
            item.type = "button";
            item.className = n.isRead ? "site-notif-item" : "site-notif-item is-unread";
            item.innerHTML = `
        <span class="site-notif-icon">${SITE_NOTIF_TYPE_ICON[n.type] || SITE_NOTIF_ICON_DEFAULT}</span>
        <span class="site-notif-body">
          <span class="site-notif-message">${escapeHtmlForMypage(n.message)}</span>
          <span class="site-notif-date">${n.createdAt}</span>
        </span>
      `;
            item.addEventListener("click", () => handleSiteNotifClick(n));
            listEl.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

async function handleSiteNotifClick(notification) {
    if (!notification.isRead) {
        try {
            await fetch(`/api/mypage/notifications/${notification.id}/read`, { method: "POST" });
        } catch (err) {
            console.error(err);
        }
    }
    closeSiteNotifPanel();
    loadSiteNotifUnreadCount();
}

async function markAllSiteNotifsRead() {
    try {
        await fetch("/api/mypage/notifications/read-all", { method: "POST" });
        await loadSiteNotifList();
        loadSiteNotifUnreadCount();
    } catch (err) {
        console.error(err);
    }
}

checkAuthState();

document.getElementById("siteNotifBtn")?.addEventListener("click", (e) => {
    e.stopPropagation();
    toggleSiteNotifPanel();
});
document.getElementById("siteNotifReadAllBtn")?.addEventListener("click", (e) => {
    e.stopPropagation();
    markAllSiteNotifsRead();
});
document.getElementById("siteNotifPanel")?.addEventListener("click", (e) => e.stopPropagation());
document.addEventListener("click", (e) => {
    const wrap = document.getElementById("siteNotifBtn")?.closest(".site-notif-wrap");
    if (wrap && !wrap.contains(e.target)) closeSiteNotifPanel();
});

// ---------- 비밀번호 눈 아이콘 공용 함수 ----------

function togglePasswordVisibility(inputId, btnId) {
    const input = document.getElementById(inputId);
    const btn = document.getElementById(btnId);
    if (!input || !btn) return;

    const isVisible = input.type === "text";
    input.type = isVisible ? "password" : "text";
    btn.classList.toggle("is-active", !isVisible);
}

// ---------- 회원가입 / 로그인 모달 ----------

function showAuthStep(id) {
    ["authStepGoogle", "authStepDetails", "authStepLogin", "authStepRecover", "authStepRecoverResult"]
        .forEach((stepId) => {
            const el = document.getElementById(stepId);
            if (el) el.hidden = stepId !== id;
        });
}

function openAuthModal(type) {
    const modal = document.getElementById("authModal");
    if (!modal) return;

    if (type === "login") {
        const intro = document.getElementById("loginIntro");
        if (intro) intro.textContent = "아이디와 비밀번호를 입력해주세요.";
        showAuthStep("authStepLogin");
    } else {
        showAuthStep("authStepGoogle");
    }

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
    document.body.classList.add("modal-open");
}

function closeAuthModal() {
    const modal = document.getElementById("authModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
    document.body.classList.remove("modal-open");
}

// 헤더의 회원가입/로그인 버튼은 처음부터 페이지에 있으므로 바로 연결
document.getElementById("signupOpenBtn")?.addEventListener("click", () => openAuthModal("signup"));
document.getElementById("loginOpenBtn")?.addEventListener("click", () => openAuthModal("login"));

// ---------- 마이페이지 ----------

function openMypageModal() {
    const modal = document.getElementById("mypageModal");
    if (!modal) return;
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
    document.body.classList.add("modal-open");
    loadMypageInfo();
    loadMyReviews();
    loadMyApplications();
}

function closeMypageModal() {
    const modal = document.getElementById("mypageModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
    document.body.classList.remove("modal-open");
}

document.getElementById("authProfileBtn")?.addEventListener("click", openMypageModal);

async function loadMypageInfo() {
    try {
        const res = await fetch("/api/mypage/me");
        if (!res.ok) return;
        const data = await res.json();

        const avatar = document.getElementById("mypageAvatar");
        if (data.profileImage) {
            avatar.innerHTML = `<img src="${data.profileImage}" alt="">`;
        } else {
            avatar.textContent = (data.nickname || "?").charAt(0);
        }

        document.getElementById("mypageNicknameDisplay").textContent = data.nickname;
        document.getElementById("mypageEmailDisplay").textContent = data.email;
        document.getElementById("mypageUsername").textContent = data.username;
        document.getElementById("mypageJoinDate").textContent = data.createdAt;
        document.getElementById("mypageNicknameInput").value = data.nickname;
        document.getElementById("mypagePhoneInput").value = data.phone || "";
    } catch (err) {
        console.error(err);
    }
}

// 사용자가 입력한 텍스트에 혹시 있을 수 있는 HTML 태그를 무력화 (안전하게 표시)
function escapeHtmlForMypage(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
}

async function loadMyReviews() {
    const list = document.getElementById("mypageReviewList");
    const emptyText = document.getElementById("mypageReviewsEmpty");
    if (!list) return;

    try {
        const res = await fetch("/api/reviews/mine");
        if (!res.ok) return;
        const reviews = await res.json();

        list.innerHTML = "";
        emptyText.hidden = reviews.length > 0;

        reviews.forEach((review) => {
            const stars = "★".repeat(review.rating) + "☆".repeat(5 - review.rating);
            const replyHtml = review.adminReply
                ? `<div class="mypage-history-reply-box">
             <p class="mypage-history-reply-label">쿤 선생님의 답글</p>
             <p class="mypage-history-reply-text">${escapeHtmlForMypage(review.adminReply)}</p>
           </div>`
                : "";

            const item = document.createElement("div");
            item.className = "mypage-history-item mypage-history-item--review";
            item.innerHTML = `
        <div class="mypage-history-main">
          <span class="mypage-history-title">${escapeHtmlForMypage(review.courseName)}</span>
          <div class="mypage-history-stars" aria-hidden="true">${stars}</div>
          <p class="mypage-history-text">${escapeHtmlForMypage(review.content)}</p>
          ${replyHtml}
        </div>
        <span class="mypage-history-date">${review.createdAt}</span>
      `;
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
    }
}

async function loadMyApplications() {
    const list = document.getElementById("mypageApplicationList");
    const emptyText = document.getElementById("mypageApplicationsEmpty");
    if (!list) return;

    const studyTypeLabel = { TOGETHER: "실시간으로 함께 배우기", VIDEO: "언제든 영상으로 배우기" };
    const statusLabel = { PENDING: "승인대기", APPROVED: "승인완료" };
    const statusClass = { PENDING: "mypage-badge--pending", APPROVED: "mypage-badge--approved" };

    try {
        const res = await fetch("/api/applications/mine");
        if (!res.ok) {
            emptyText.hidden = false;
            emptyText.textContent = res.status === 401
                ? "로그인 정보가 없어요. 다시 로그인해주세요."
                : `불러오지 못했어요. (오류 코드 ${res.status})`;
            return;
        }
        const applications = await res.json();
        myApplicationsCache = applications;

        list.innerHTML = "";
        emptyText.hidden = applications.length > 0;

        applications.forEach((app) => {
            const item = document.createElement("div");
            item.className = "mypage-history-item";
            item.innerHTML = `
        <div class="mypage-history-main">
          <span class="mypage-history-title">${escapeHtmlForMypage(app.courseName)} · ${studyTypeLabel[app.studyType] || app.studyType}</span>
          <span class="mypage-history-date">신청일 ${app.createdAt}</span>
          ${app.studentNumber ? `
            <span class="mypage-student-number-row">
              <span class="mypage-student-number">학생번호: ${escapeHtmlForMypage(app.studentNumber)} · KWZM Center 입장에 필요해요</span>
              <button type="button" class="mypage-copy-btn" data-copy-text="${escapeHtmlForMypage(app.studentNumber)}" aria-label="학생번호 복사">
                <svg viewBox="0 0 24 24" fill="none"><rect x="9" y="9" width="11" height="11" rx="2" stroke="currentColor" stroke-width="1.6"/><path d="M5 15V6a2 2 0 0 1 2-2h9" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
                복사
              </button>
            </span>
          ` : ""}
          ${app.hasPaymentInfo || app.status === "APPROVED" ? `
            <div class="mypage-history-actions">
              ${app.hasPaymentInfo ? `<button type="button" class="mypage-payment-confirm-btn" data-payment-confirm-id="${app.id}">결제 확인</button>` : ""}
              ${app.status === "APPROVED" ? `<button type="button" class="mypage-attendance-btn" data-attendance-id="${app.id}" data-attendance-course="${escapeHtmlForMypage(app.courseName)}">출석 현황</button>` : ""}
            </div>
          ` : ""}
        </div>
        <span class="mypage-badge ${statusClass[app.status] || ""}">${statusLabel[app.status] || app.status}</span>
      `;
            list.appendChild(item);
        });
    } catch (err) {
        console.error(err);
        emptyText.hidden = false;
        emptyText.textContent = "서버에 연결할 수 없어요.";
    }
}

let myApplicationsCache = [];

function openPaymentConfirmModal(applicationId) {
    const app = myApplicationsCache.find((a) => String(a.id) === String(applicationId));
    if (!app) return;

    const modal = document.getElementById("paymentConfirmModal");
    if (!modal) return;

    document.getElementById("paymentConfirmCourse").textContent = app.courseName;
    document.getElementById("paymentConfirmMethod").textContent = app.paymentMethod || "-";
    document.getElementById("paymentConfirmAmount").textContent = app.amount || "-";
    document.getElementById("paymentConfirmReason").textContent = app.amountReason || "-";
    document.getElementById("paymentConfirmMaterial").textContent = app.materialGuide || "-";
    document.getElementById("paymentConfirmClass").textContent = app.classGuide || "-";
    document.getElementById("paymentConfirmApplicationId").value = app.id;

    // 영수증 첨부 UI 초기화
    selectedReceiptDataUri = null;
    const receiptInput = document.getElementById("paymentReceiptInput");
    if (receiptInput) receiptInput.value = "";
    const receiptPreview = document.getElementById("paymentReceiptPreview");
    if (receiptPreview) receiptPreview.hidden = true;
    const receiptUploadLabel = document.getElementById("paymentReceiptUpload");
    if (receiptUploadLabel) receiptUploadLabel.querySelector(".payment-receipt-upload-label").hidden = false;

    updatePaymentConfirmFooter(app.paymentConfirmedByStudent, app.paymentConfirmedByAdmin);

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
}

let selectedReceiptDataUri = null;

function handleReceiptFileSelected(file) {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
        selectedReceiptDataUri = reader.result;
        const previewImg = document.getElementById("paymentReceiptPreviewImg");
        const previewBox = document.getElementById("paymentReceiptPreview");
        const uploadLabel = document.querySelector("#paymentReceiptUpload .payment-receipt-upload-label");
        if (previewImg) previewImg.src = selectedReceiptDataUri;
        if (previewBox) previewBox.hidden = false;
        if (uploadLabel) uploadLabel.hidden = true;
    };
    reader.readAsDataURL(file);
}

function removeReceiptPreview() {
    selectedReceiptDataUri = null;
    const receiptInput = document.getElementById("paymentReceiptInput");
    if (receiptInput) receiptInput.value = "";
    const previewBox = document.getElementById("paymentReceiptPreview");
    if (previewBox) previewBox.hidden = true;
    const uploadLabel = document.querySelector("#paymentReceiptUpload .payment-receipt-upload-label");
    if (uploadLabel) uploadLabel.hidden = false;
}

function updatePaymentConfirmFooter(confirmedByStudent, confirmedByAdmin) {
    const btn = document.getElementById("paymentConfirmBtn");
    const pendingStatus = document.getElementById("paymentConfirmPendingStatus");
    const doneStatus = document.getElementById("paymentConfirmDoneStatus");
    const uploadEl = document.getElementById("paymentReceiptUpload");
    if (!btn || !pendingStatus || !doneStatus) return;

    if (confirmedByAdmin) {
        btn.hidden = true;
        pendingStatus.hidden = true;
        doneStatus.hidden = false;
        if (uploadEl) uploadEl.hidden = true;
    } else if (confirmedByStudent) {
        btn.hidden = true;
        pendingStatus.hidden = false;
        doneStatus.hidden = true;
        if (uploadEl) uploadEl.hidden = true;
    } else {
        btn.hidden = false;
        pendingStatus.hidden = true;
        doneStatus.hidden = true;
        if (uploadEl) uploadEl.hidden = false;
    }
}

async function submitPaymentConfirm() {
    const applicationId = document.getElementById("paymentConfirmApplicationId").value;
    const btn = document.getElementById("paymentConfirmBtn");
    if (!applicationId || !btn) return;

    btn.disabled = true;
    try {
        const res = await fetch(`/api/applications/${applicationId}/confirm-payment`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ receiptImage: selectedReceiptDataUri }),
        });
        if (!res.ok) {
            alert((await res.text()) || "확인 처리에 실패했어요.");
            return;
        }

        const cached = myApplicationsCache.find((a) => String(a.id) === String(applicationId));
        if (cached) cached.paymentConfirmedByStudent = true;
        updatePaymentConfirmFooter(true, cached ? cached.paymentConfirmedByAdmin : false);

        const uploadEl = document.getElementById("paymentReceiptUpload");
        if (uploadEl) uploadEl.hidden = true;
    } catch (err) {
        console.error(err);
        alert("서버에 연결할 수 없어요.");
    } finally {
        btn.disabled = false;
    }
}

function closePaymentConfirmModal() {
    const modal = document.getElementById("paymentConfirmModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

const ATTENDANCE_STATUS_LABEL_STUDENT = { PRESENT: "출석", ABSENT: "결석", MAKEUP: "보강" };

async function openAttendanceModal(applicationId, courseName) {
    const modal = document.getElementById("attendanceModal");
    if (!modal) return;

    document.getElementById("attendanceCourseTitle").textContent = courseName || "";
    const summaryEl = document.getElementById("attendanceSummaryGrid");
    const listEl = document.getElementById("attendanceRecordList");
    const emptyEl = document.getElementById("attendanceEmpty");
    if (summaryEl) summaryEl.innerHTML = "";
    if (listEl) listEl.innerHTML = `<p class="course-materials-hint">불러오는 중...</p>`;
    if (emptyEl) emptyEl.hidden = true;

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");

    try {
        const res = await fetch(`/api/applications/${applicationId}/attendance`);
        if (!res.ok) {
            if (listEl) listEl.innerHTML = `<p class="course-materials-hint">불러오지 못했어요.</p>`;
            return;
        }
        const data = await res.json();

        const total = data.presentCount + data.absentCount + data.makeupCount;
        const rate = total > 0 ? Math.round((data.presentCount / total) * 100) : 0;
        if (summaryEl) {
            summaryEl.innerHTML = `
        <div class="attendance-stat"><span class="attendance-stat-value">${data.presentCount}</span><span>출석</span></div>
        <div class="attendance-stat"><span class="attendance-stat-value">${data.absentCount}</span><span>결석</span></div>
        <div class="attendance-stat"><span class="attendance-stat-value">${data.makeupCount}</span><span>보강</span></div>
        <div class="attendance-stat attendance-stat--rate"><span class="attendance-stat-value">${rate}%</span><span>출석률</span></div>
      `;
        }

        if (listEl) {
            listEl.innerHTML = "";
            if (data.records.length === 0) {
                if (emptyEl) emptyEl.hidden = false;
                return;
            }
            data.records.forEach((r) => {
                const row = document.createElement("div");
                row.className = "attendance-record-row";
                row.innerHTML = `
          <span class="attendance-record-date">${r.classDate}</span>
          <span class="attendance-record-status attendance-record-status--${r.status.toLowerCase()}">${ATTENDANCE_STATUS_LABEL_STUDENT[r.status] || r.status}</span>
          <span class="attendance-record-note">${escapeHtmlForMypage(r.note || "")}</span>
        `;
                listEl.appendChild(row);
            });
        }
    } catch (err) {
        console.error(err);
        if (listEl) listEl.innerHTML = `<p class="course-materials-hint">서버에 연결할 수 없어요.</p>`;
    }
}

function closeAttendanceModal() {
    const modal = document.getElementById("attendanceModal");
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
}

// ---------- fragment가 실제로 로드된 뒤에만 연결해야 하는 것들 ----------

document.addEventListener("fragments:loaded", () => {

    // 모달들 닫기 (배경 클릭 / X 버튼)
    document.addEventListener("click", (e) => {
        if (e.target.closest("[data-auth-close]")) closeAuthModal();
        if (e.target.closest("[data-mypage-close]")) closeMypageModal();
        if (e.target.closest("[data-payment-confirm-close]")) closePaymentConfirmModal();
        if (e.target.closest("[data-attendance-close]")) closeAttendanceModal();
    });

    // "결제 확인" / "출석 현황" 버튼 (동적으로 생기므로 위임 처리)
    document.addEventListener("click", (e) => {
        const paymentBtn = e.target.closest("[data-payment-confirm-id]");
        if (paymentBtn) openPaymentConfirmModal(paymentBtn.dataset.paymentConfirmId);

        const attendanceBtn = e.target.closest("[data-attendance-id]");
        if (attendanceBtn) openAttendanceModal(attendanceBtn.dataset.attendanceId, attendanceBtn.dataset.attendanceCourse);
    });

    document.getElementById("paymentConfirmBtn")?.addEventListener("click", submitPaymentConfirm);
    document.getElementById("paymentReceiptInput")?.addEventListener("change", (e) => {
        handleReceiptFileSelected(e.target.files[0]);
    });
    document.getElementById("paymentReceiptRemoveBtn")?.addEventListener("click", (e) => {
        e.preventDefault();
        removeReceiptPreview();
    });

    // 학생번호 복사 버튼 (동적으로 생기므로 위임 처리)
    document.addEventListener("click", async (e) => {
        const btn = e.target.closest(".mypage-copy-btn");
        if (!btn) return;

        const text = btn.dataset.copyText;
        try {
            await navigator.clipboard.writeText(text);
        } catch (err) {
            // 클립보드 API를 못 쓰는 환경(구형 브라우저 등)을 위한 대체 방법
            const temp = document.createElement("textarea");
            temp.value = text;
            temp.style.position = "fixed";
            temp.style.opacity = "0";
            document.body.appendChild(temp);
            temp.select();
            document.execCommand("copy");
            document.body.removeChild(temp);
        }

        const originalHtml = btn.innerHTML;
        btn.classList.add("is-copied");
        btn.innerHTML = `
      <svg viewBox="0 0 24 24" fill="none"><path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
      복사됨
    `;
        setTimeout(() => {
            btn.classList.remove("is-copied");
            btn.innerHTML = originalHtml;
        }, 1500);
    });

    document.addEventListener("keydown", (e) => {
        if (e.key !== "Escape") return;
        closeAuthModal();
        closeMypageModal();
    });

    // 비밀번호 눈 아이콘들
    document.getElementById("toggleSignupPasswordBtn")?.addEventListener("click", () => {
        togglePasswordVisibility("signupPassword", "toggleSignupPasswordBtn");
    });
    document.getElementById("toggleLoginPasswordBtn")?.addEventListener("click", () => {
        togglePasswordVisibility("loginPassword", "toggleLoginPasswordBtn");
    });
    document.getElementById("toggleRecoverPasswordBtn")?.addEventListener("click", () => {
        togglePasswordVisibility("recoverNewPassword", "toggleRecoverPasswordBtn");
    });
    document.getElementById("toggleMypageCurrentPwBtn")?.addEventListener("click", () => {
        togglePasswordVisibility("mypageCurrentPassword", "toggleMypageCurrentPwBtn");
    });
    document.getElementById("toggleMypageNewPwBtn")?.addEventListener("click", () => {
        togglePasswordVisibility("mypageNewPassword", "toggleMypageNewPwBtn");
    });

    // ---- 아이디 중복확인 ----
    document.getElementById("checkUsernameBtn")?.addEventListener("click", async () => {
        const usernameInput = document.getElementById("signupUsername");
        const hint = document.getElementById("usernameHint");
        const username = usernameInput.value.trim();

        hint.className = "auth-field-hint";
        if (username.length < 4) {
            hint.textContent = "아이디는 4자 이상으로 입력해주세요.";
            return;
        }

        try {
            const res = await fetch(`/api/auth/check-username?username=${encodeURIComponent(username)}`);
            const data = await res.json();

            if (data.available) {
                hint.textContent = "사용할 수 있는 아이디예요.";
                hint.classList.add("is-ok");
                usernameInput.dataset.checked = username;
            } else {
                hint.textContent = "이미 사용 중인 아이디예요.";
                hint.classList.add("is-taken");
                usernameInput.dataset.checked = "";
            }
        } catch (err) {
            hint.textContent = "확인 중 오류가 발생했어요.";
            console.error(err);
        }
    });

    document.getElementById("signupUsername")?.addEventListener("input", (e) => {
        e.target.dataset.checked = "";
        const hint = document.getElementById("usernameHint");
        hint.textContent = "";
        hint.className = "auth-field-hint";
    });

    // ---- STEP 2: 회원가입 완료 ----
    document.getElementById("completeSignupBtn")?.addEventListener("click", async () => {
        const usernameInput = document.getElementById("signupUsername");
        const passwordInput = document.getElementById("signupPassword");
        const errorEl = document.getElementById("signupDetailsError");
        const username = usernameInput.value.trim();
        const password = passwordInput.value;

        const showError = (msg) => {
            errorEl.textContent = msg;
            errorEl.hidden = false;
        };

        if (username.length < 4) return showError("아이디는 4자 이상으로 입력해주세요.");
        if (usernameInput.dataset.checked !== username) return showError("아이디 중복확인을 먼저 해주세요.");
        if (password.length < 8) return showError("비밀번호는 8자 이상이어야 해요.");
        errorEl.hidden = true;

        try {
            const res = await fetch("/api/auth/complete-signup", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
            });

            if (!res.ok) {
                showError((await res.text()) || "가입에 실패했어요. 다시 시도해주세요.");
                return;
            }

            document.getElementById("loginUsername").value = username;
            document.getElementById("loginIntro").textContent = "가입이 완료됐어요! 방금 만든 아이디로 로그인해주세요.";
            showAuthStep("authStepLogin");
            showSiteToast("회원가입이 완료됐어요!");
        } catch (err) {
            showError("서버에 연결할 수 없어요.");
            console.error(err);
        }
    });

    // ---- STEP 3: 로그인 ----
    document.getElementById("loginSubmitBtn")?.addEventListener("click", async () => {
        const username = document.getElementById("loginUsername").value.trim();
        const password = document.getElementById("loginPassword").value;
        const errorEl = document.getElementById("loginError");

        if (!username || !password) {
            errorEl.textContent = "아이디와 비밀번호를 모두 입력해주세요.";
            errorEl.hidden = false;
            return;
        }
        errorEl.hidden = true;

        try {
            const res = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
            });

            if (!res.ok) {
                errorEl.textContent = (await res.text()) || "아이디 또는 비밀번호가 올바르지 않아요.";
                errorEl.hidden = false;
                return;
            }

            closeAuthModal();
            checkAuthState();
            showSiteToast("로그인됐어요. 환영해요!");
        } catch (err) {
            errorEl.textContent = "서버에 연결할 수 없어요.";
            errorEl.hidden = false;
            console.error(err);
        }
    });

    // ---- STEP 4: 아이디/비밀번호 찾기 진입 (목적을 구분해서 기억해둠) ----
    document.getElementById("showFindIdBtn")?.addEventListener("click", () => {
        sessionStorage.setItem("recoveryType", "id");
        document.getElementById("recoverStepTitle").textContent = "아이디 찾기";
        document.getElementById("recoverStepDesc").textContent = "가입할 때 쓰신 구글 계정으로 본인 확인을 해주세요.";
        showAuthStep("authStepRecover");
    });

    document.getElementById("showFindPasswordBtn")?.addEventListener("click", () => {
        sessionStorage.setItem("recoveryType", "password");
        document.getElementById("recoverStepTitle").textContent = "비밀번호 찾기";
        document.getElementById("recoverStepDesc").textContent = "가입할 때 쓰신 구글 계정으로 본인 확인을 해주세요.";
        showAuthStep("authStepRecover");
    });

    document.getElementById("goToSignupBtn")?.addEventListener("click", () => {
        showAuthStep("authStepGoogle");
    });

    document.getElementById("goToLoginFromIdBtn")?.addEventListener("click", () => {
        showAuthStep("authStepLogin");
    });

    // ---- STEP 5: 찾기 결과 처리는 구글 재인증 후(?googleRecovery=true) 아래에서 별도 처리 ----
    document.getElementById("recoverResetBtn")?.addEventListener("click", async () => {
        const newPassword = document.getElementById("recoverNewPassword").value;
        const errorEl = document.getElementById("recoverError");

        if (newPassword.length < 8) {
            errorEl.textContent = "비밀번호는 8자 이상이어야 해요.";
            errorEl.hidden = false;
            return;
        }
        errorEl.hidden = true;

        try {
            const res = await fetch("/api/auth/reset-password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ newPassword }),
            });

            if (!res.ok) {
                errorEl.textContent = (await res.text()) || "재설정에 실패했어요.";
                errorEl.hidden = false;
                return;
            }

            document.getElementById("loginIntro").textContent = "비밀번호가 재설정됐어요. 새 비밀번호로 로그인해주세요.";
            showAuthStep("authStepLogin");
        } catch (err) {
            errorEl.textContent = "서버에 연결할 수 없어요.";
            errorEl.hidden = false;
            console.error(err);
        }
    });

    // ---- 마이페이지: 닉네임 변경 ----
    document.getElementById("mypageSaveNicknameBtn")?.addEventListener("click", async () => {
        const input = document.getElementById("mypageNicknameInput");
        const hint = document.getElementById("mypageNicknameHint");
        const nickname = input.value.trim();

        hint.className = "auth-field-hint";
        if (!nickname) {
            hint.textContent = "닉네임을 입력해주세요.";
            return;
        }

        try {
            const res = await fetch("/api/mypage/nickname", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ nickname }),
            });

            if (!res.ok) {
                hint.textContent = (await res.text()) || "변경에 실패했어요.";
                return;
            }

            hint.textContent = "닉네임이 변경됐어요.";
            hint.classList.add("is-ok");
            document.getElementById("mypageNicknameDisplay").textContent = nickname;
            checkAuthState();
        } catch (err) {
            hint.textContent = "서버에 연결할 수 없어요.";
            console.error(err);
        }
    });

    // ---- 마이페이지: 전화번호 저장 ----
    document.getElementById("mypageSavePhoneBtn")?.addEventListener("click", async () => {
        const input = document.getElementById("mypagePhoneInput");
        const hint = document.getElementById("mypagePhoneHint");
        const phone = input.value.trim();

        hint.className = "auth-field-hint";

        try {
            const res = await fetch("/api/mypage/phone", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ phone }),
            });

            if (!res.ok) {
                hint.textContent = (await res.text()) || "변경에 실패했어요.";
                return;
            }

            hint.textContent = "전화번호가 저장됐어요.";
            hint.classList.add("is-ok");
        } catch (err) {
            hint.textContent = "서버에 연결할 수 없어요.";
            console.error(err);
        }
    });

    // ---- 마이페이지: 비밀번호 변경 ----
    document.getElementById("mypageChangePasswordBtn")?.addEventListener("click", async () => {
        const current = document.getElementById("mypageCurrentPassword").value;
        const next = document.getElementById("mypageNewPassword").value;
        const errorEl = document.getElementById("mypagePasswordError");
        const successEl = document.getElementById("mypagePasswordSuccess");

        successEl.hidden = true;
        if (!current || next.length < 8) {
            errorEl.textContent = "현재 비밀번호를 입력하고, 새 비밀번호는 8자 이상으로 해주세요.";
            errorEl.hidden = false;
            return;
        }
        errorEl.hidden = true;

        try {
            const res = await fetch("/api/mypage/password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ currentPassword: current, newPassword: next }),
            });

            if (!res.ok) {
                errorEl.textContent = (await res.text()) || "변경에 실패했어요.";
                errorEl.hidden = false;
                return;
            }

            document.getElementById("mypageCurrentPassword").value = "";
            document.getElementById("mypageNewPassword").value = "";
            successEl.hidden = false;
            showSiteToast("비밀번호가 변경됐어요.");
        } catch (err) {
            errorEl.textContent = "서버에 연결할 수 없어요.";
            errorEl.hidden = false;
            console.error(err);
        }
    });

    // ---- 마이페이지: 프로필 사진 업로드 ----
    document.getElementById("mypageAvatarInput")?.addEventListener("change", async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        if (file.size > 2 * 1024 * 1024) {
            alert("이미지 용량은 2MB 이하로 올려주세요.");
            return;
        }

        const reader = new FileReader();
        reader.onload = async () => {
            const imageBase64 = reader.result;
            try {
                const res = await fetch("/api/mypage/photo", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ imageBase64 }),
                });
                if (!res.ok) {
                    alert("사진 업로드에 실패했어요.");
                    return;
                }
                document.getElementById("mypageAvatar").innerHTML = `<img src="${imageBase64}" alt="">`;
                checkAuthState();
            } catch (err) {
                console.error(err);
                alert("서버에 연결할 수 없어요.");
            }
        };
        reader.readAsDataURL(file);
    });

    // ---- 마이페이지: 로그아웃 ----
    document.getElementById("mypageLogoutBtn")?.addEventListener("click", () => {
        window.location.href = "/logout";
    });

    // ---- 마이페이지: 탭 전환 (개인정보 / 강의 신청 내역 / 리뷰 내역) ----
    document.querySelectorAll(".mypage-tab").forEach((tab) => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".mypage-tab").forEach((t) => {
                t.classList.remove("active");
                t.setAttribute("aria-selected", "false");
            });
            document.querySelectorAll(".mypage-tab-panel").forEach((p) => {
                p.hidden = true;
            });

            tab.classList.add("active");
            tab.setAttribute("aria-selected", "true");

            const targetId = "mypageTab" + tab.dataset.mypageTab.charAt(0).toUpperCase() + tab.dataset.mypageTab.slice(1);
            const targetPanel = document.getElementById(targetId);
            if (targetPanel) targetPanel.hidden = false;
        });
    });

    // ---- 구글 재인증(회원가입 2단계 / 계정찾기)에서 돌아왔을 때 처리 ----
    handleGoogleRedirect();
});

// 비밀번호 찾기 결과에서 아이디 일부만 보여줄 때 씀 (예: khun1000 -> kh******)
function maskUsername(username) {
    if (!username || username.length <= 2) return username;
    return username.slice(0, 2) + "*".repeat(username.length - 2);
}

async function handleGoogleRedirect() {
    const params = new URLSearchParams(window.location.search);
    const isSignupVerified = params.get("googleVerified") === "true";
    const isRecovery = params.get("googleRecovery") === "true";
    if (!isSignupVerified && !isRecovery) return;

    // 주소창의 쿼리스트링은 지워서, 새로고침해도 다시 이 로직이 안 돌게 함
    window.history.replaceState({}, "", window.location.pathname);

    try {
        const profileRes = await fetch("/api/auth/google-profile");
        if (!profileRes.ok) return;
        const profile = await profileRes.json();

        if (isSignupVerified) {
            document.getElementById("authGoogleNickname").textContent = profile.nickname;
            document.getElementById("authGoogleEmail").textContent = profile.email;
            openAuthModal("signup");
            showAuthStep("authStepDetails");
            return;
        }

        if (isRecovery) {
            const infoRes = await fetch("/api/auth/recovery-info");
            const info = await infoRes.json();
            const recoveryType = sessionStorage.getItem("recoveryType") || "id";
            sessionStorage.removeItem("recoveryType");

            openAuthModal("login");
            showAuthStep("authStepRecoverResult");

            const foundIdEl = document.getElementById("recoverFoundId");
            const foundPasswordEl = document.getElementById("recoverFoundPassword");
            const notFoundEl = document.getElementById("recoverNotFound");

            foundIdEl.hidden = true;
            foundPasswordEl.hidden = true;
            notFoundEl.hidden = true;

            if (!info.found) {
                notFoundEl.hidden = false;
            } else if (recoveryType === "password") {
                foundPasswordEl.hidden = false;
                document.getElementById("recoverUsername").textContent = maskUsername(info.username);
            } else {
                foundIdEl.hidden = false;
                document.getElementById("recoverUsernameOnly").textContent = info.username;
            }
        }
    } catch (err) {
        console.error(err);
    }
}