// '강의 신청' 화면 전용 스크립트
// apply.html은 fragment로 나중에 로드되므로, document 이벤트 위임으로 처리해서
// 로드 타이밍에 상관없이 항상 동작하게 함

document.addEventListener("submit", async (e) => {
    const form = e.target.closest("#applyForm");
    if (!form) return;
    e.preventDefault();

    const studyTypeInput = form.querySelector('input[name="studyType"]:checked');
    const courseSelect = document.getElementById("applyCourseSelect");
    const contactInput = document.getElementById("applyContactInput");
    const memoInput = document.getElementById("applyMemoInput");
    const errorEl = document.getElementById("applyFormError");
    const submitBtn = form.querySelector(".apply-submit-btn");

    const showError = (msg) => {
        errorEl.textContent = msg;
        errorEl.hidden = false;
    };

    if (!studyTypeInput) return showError("학습 방식을 선택해주세요.");
    if (!courseSelect.value) return showError("과목을 선택해주세요.");
    if (!contactInput.value.trim()) return showError("연락처를 입력해주세요.");
    errorEl.hidden = true;

    // 로그인 여부 먼저 확인
    try {
        const meRes = await fetch("/api/auth/me");
        if (!meRes.ok) {
            if (typeof openAuthModal === "function") {
                openAuthModal("login");
            } else {
                alert("강의 신청을 하려면 먼저 로그인해주세요.");
            }
            return;
        }
    } catch (err) {
        return showError("서버에 연결할 수 없어요.");
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "신청 중...";

    try {
        const res = await fetch("/api/applications", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                studyType: studyTypeInput.value,
                courseName: courseSelect.value,
                contact: contactInput.value.trim(),
                memo: memoInput.value.trim(),
            }),
        });

        if (!res.ok) {
            const message = await res.text();
            showError(message || "신청에 실패했어요. 다시 시도해주세요.");
            return;
        }

        form.hidden = true;
        const success = document.getElementById("applySuccess");
        if (success) success.hidden = false;
    } catch (err) {
        console.error(err);
        showError("서버에 연결할 수 없어요.");
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = "신청하기";
    }
});

document.addEventListener("click", (e) => {
    if (!e.target.closest("#applyAgainBtn")) return;

    const form = document.getElementById("applyForm");
    const success = document.getElementById("applySuccess");
    const errorEl = document.getElementById("applyFormError");
    if (!form) return;

    form.reset();
    form.hidden = false;
    if (success) success.hidden = true;
    if (errorEl) errorEl.hidden = true;
});