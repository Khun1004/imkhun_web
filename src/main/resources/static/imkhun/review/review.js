// '리뷰' 화면 전용 스크립트 — 리뷰 작성 모달 (로그인 필요) + 실제 목록 불러오기/작성
// 주의: review.html은 fragment로 나중에 fetch되므로, fragments:loaded 이후에
// 요소를 찾아야 하는 부분(버튼 연결, 목록 불러오기)은 이벤트 리스너 안에서 처리함

let selectedRating = 0;

document.addEventListener("fragments:loaded", () => {
    // "리뷰 작성" 버튼 누르면: 로그인 여부부터 확인
    document.getElementById("reviewWriteBtn")?.addEventListener("click", async () => {
        try {
            const res = await fetch("/api/auth/me");
            if (!res.ok) {
                // 로그인 안 되어 있으면 → 로그인 모달을 대신 열어줌 (auth.js의 함수 재사용)
                if (typeof openAuthModal === "function") {
                    openAuthModal("login");
                } else {
                    alert("리뷰를 작성하려면 먼저 로그인해주세요.");
                }
                return;
            }

            const me = await res.json();
            document.getElementById("reviewWriterName").textContent = `${me.nickname} 님으로 작성돼요`;
            openReviewModal();
        } catch (err) {
            console.error(err);
            alert("서버에 연결할 수 없어요.");
        }
    });

    loadRealReviews();
});

function openReviewModal() {
    const modal = document.getElementById("reviewModal");
    if (!modal) return;
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
    document.body.classList.add("modal-open");
}

function closeReviewModal() {
    const modal = document.getElementById("reviewModal");
    const form = document.getElementById("reviewForm");
    const success = document.getElementById("reviewFormSuccess");
    const error = document.getElementById("reviewFormError");
    if (!modal) return;

    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
    document.body.classList.remove("modal-open");

    if (form) {
        form.reset();
        form.hidden = false;
    }
    if (success) success.hidden = true;
    if (error) error.hidden = true;
    setRating(0);
}

function setRating(value) {
    selectedRating = value;
    document.querySelectorAll(".review-star-btn").forEach((btn) => {
        const starValue = Number(btn.dataset.star);
        btn.classList.toggle("is-active", starValue <= value);
    });
}

document.addEventListener("click", (e) => {
    if (e.target.closest("[data-review-close]")) {
        closeReviewModal();
        return;
    }

    const starBtn = e.target.closest(".review-star-btn");
    if (starBtn) {
        setRating(Number(starBtn.dataset.star));
    }
});

document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeReviewModal();
});

document.addEventListener("submit", async (e) => {
    const form = e.target.closest("#reviewForm");
    if (!form) return;
    e.preventDefault();

    const course = document.getElementById("reviewCourseInput")?.value.trim();
    const text = document.getElementById("reviewTextInput")?.value.trim();
    const errorEl = document.getElementById("reviewFormError");

    if (!selectedRating || !course || !text) {
        if (errorEl) {
            errorEl.textContent = "필수 항목을 모두 입력해주세요.";
            errorEl.hidden = false;
        }
        return;
    }
    if (errorEl) errorEl.hidden = true;

    try {
        const res = await fetch("/api/reviews", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ courseName: course, rating: selectedRating, content: text }),
        });

        if (!res.ok) {
            const message = await res.text();
            if (errorEl) {
                errorEl.textContent = message || "제출에 실패했어요.";
                errorEl.hidden = false;
            }
            return;
        }

        form.hidden = true;
        const success = document.getElementById("reviewFormSuccess");
        if (success) success.hidden = false;

        loadRealReviews(); // 새로 쓴 리뷰가 목록에 바로 보이게 새로고침
    } catch (err) {
        console.error(err);
        if (errorEl) {
            errorEl.textContent = "서버에 연결할 수 없어요.";
            errorEl.hidden = false;
        }
    }
});

// ---------- 실제로 작성된 리뷰들을 목록 맨 위에 추가 ----------

// 사용자가 입력한 텍스트에 혹시 있을 수 있는 HTML 태그를 무력화 (안전하게 표시)
function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
}

async function loadRealReviews() {
    const grid = document.querySelector(".review-grid");
    if (!grid) return;

    try {
        const res = await fetch("/api/reviews");
        if (!res.ok) return;
        const reviews = await res.json();

        // 이전에 불러온 실제 리뷰 카드들 제거 후 다시 그림 (중복 방지)
        grid.querySelectorAll(".review-card[data-real='true']").forEach((el) => el.remove());

        reviews.forEach((review) => {
            const card = document.createElement("div");
            card.className = "review-card";
            card.dataset.real = "true";

            const stars = "★".repeat(review.rating) + "☆".repeat(5 - review.rating);
            const nickname = escapeHtml(review.nickname);
            const courseName = escapeHtml(review.courseName);
            const content = escapeHtml(review.content);
            const initial = (review.nickname || "?").charAt(0);
            const avatarHtml = review.profileImage
                ? `<img src="${review.profileImage}" alt="">`
                : escapeHtml(initial);

            const replyHtml = review.adminReply
                ? `<div class="review-reply-box">
             <p class="review-reply-label">쿤 선생님의 답글</p>
             <p class="review-reply-text">${escapeHtml(review.adminReply)}</p>
           </div>`
                : "";

            card.innerHTML = `
        <div class="review-card-head">
          <span class="review-avatar">${avatarHtml}</span>
          <div>
            <p class="review-name">${nickname} 님</p>
            <div class="review-stars review-stars--sm" aria-hidden="true">${stars}</div>
          </div>
          <span class="review-tag review-tag--korean">${courseName}</span>
        </div>
        <p class="review-text">${content}</p>
        <p class="review-date">${review.createdAt}</p>
        ${replyHtml}
      `;
            grid.prepend(card);
        });
    } catch (err) {
        console.error(err);
    }
}