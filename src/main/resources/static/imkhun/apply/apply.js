// '강의 신청' 화면 전용 스크립트
// apply.html은 fragment로 나중에 로드되므로, document 이벤트 위임으로 처리해서
// 로드 타이밍에 상관없이 항상 동작하게 함

let timetableByCourse = {}; // { "한국어 1급": [{days:["MON","FRI"], startTime:"14:00", endTime:"15:30"}, ...], ... }
let selectedScheduleKey = null;

document.addEventListener("fragments:loaded", () => {
    loadTimetableForApply();
});

function escapeHtmlForApply(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
}

async function loadTimetableForApply() {
    const courseSelect = document.getElementById("applyCourseSelect");
    if (!courseSelect) return;

    try {
        const res = await fetch("/api/timetable");
        if (!res.ok) return;
        const entries = await res.json();

        // 과목별 → 같은 시간(시작~종료)끼리 묶고, 그 시간에 해당하는 요일들을 모음
        timetableByCourse = {};
        entries.forEach((e) => {
            if (!timetableByCourse[e.courseName]) timetableByCourse[e.courseName] = {};
            const timeKey = `${e.startTime}~${e.endTime}`;
            if (!timetableByCourse[e.courseName][timeKey]) {
                timetableByCourse[e.courseName][timeKey] = { startTime: e.startTime, endTime: e.endTime, days: [] };
            }
            timetableByCourse[e.courseName][timeKey].days.push(e.day);
        });

        // 시간표에 없는 과목은 신청 화면에서 고를 수 없게 숨김
        courseSelect.querySelectorAll("option[value]").forEach((opt) => {
            if (!opt.value) return;
            const hasSchedule = !!timetableByCourse[opt.value];
            opt.hidden = !hasSchedule;
            opt.disabled = !hasSchedule;
        });
        // optgroup 안 옵션이 전부 숨겨졌으면 그룹 제목도 숨김
        courseSelect.querySelectorAll("optgroup").forEach((group) => {
            const visibleOptions = [...group.querySelectorAll("option")].some((opt) => !opt.hidden);
            group.hidden = !visibleOptions;
        });
    } catch (err) {
        console.error(err);
    }
}

const SCHEDULE_DAY_LABEL_APPLY = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금", SAT: "토", SUN: "일" };

function renderApplyScheduleSlots(courseName) {
    const slotsEl = document.getElementById("applyScheduleSlots");
    const hintEl = document.getElementById("applyScheduleHint");
    const daysValue = document.getElementById("applyScheduleDaysValue");
    const timeValue = document.getElementById("applyScheduleTimeValue");
    if (!slotsEl || !hintEl) return;

    selectedScheduleKey = null;
    if (daysValue) daysValue.value = "";
    if (timeValue) timeValue.value = "";
    slotsEl.innerHTML = "";

    if (!courseName) {
        hintEl.textContent = "먼저 과목을 선택해주세요.";
        return;
    }

    const slots = timetableByCourse[courseName];
    if (!slots || Object.keys(slots).length === 0) {
        hintEl.textContent = "이 과목은 아직 시간표가 등록되지 않았어요. 선생님께 문의해주세요.";
        return;
    }

    hintEl.textContent = "원하시는 시간을 골라주세요. (선택 안 하셔도 신청은 가능해요)";

    Object.entries(slots).forEach(([timeKey, slot]) => {
        const daysLabel = slot.days.map((d) => SCHEDULE_DAY_LABEL_APPLY[d] || d).join(", ");
        const chip = document.createElement("button");
        chip.type = "button";
        chip.className = "apply-slot-chip";
        chip.dataset.days = slot.days.join(",");
        chip.dataset.time = slot.startTime;
        chip.innerHTML = `<strong>${escapeHtmlForApply(daysLabel)}</strong><span>${escapeHtmlForApply(slot.startTime)} - ${escapeHtmlForApply(slot.endTime)}</span>`;
        chip.addEventListener("click", () => {
            const willSelect = selectedScheduleKey !== timeKey;
            slotsEl.querySelectorAll(".apply-slot-chip").forEach((c) => c.classList.remove("active"));
            if (willSelect) {
                chip.classList.add("active");
                selectedScheduleKey = timeKey;
                if (daysValue) daysValue.value = slot.days.join(",");
                if (timeValue) timeValue.value = slot.startTime;
            } else {
                selectedScheduleKey = null;
                if (daysValue) daysValue.value = "";
                if (timeValue) timeValue.value = "";
            }
        });
        slotsEl.appendChild(chip);
    });
}

document.addEventListener("change", (e) => {
    if (e.target.id === "applyCourseSelect") {
        renderApplyScheduleSlots(e.target.value);
    }
});

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
        const scheduleDays = document.getElementById("applyScheduleDaysValue")?.value || "";
        const scheduleTime = document.getElementById("applyScheduleTimeValue")?.value || "";

        const res = await fetch("/api/applications", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                studyType: studyTypeInput.value,
                courseName: courseSelect.value,
                contact: contactInput.value.trim(),
                memo: memoInput.value.trim(),
                classDays: scheduleDays || null,
                classTime: scheduleTime || null,
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
    renderApplyScheduleSlots("");
});