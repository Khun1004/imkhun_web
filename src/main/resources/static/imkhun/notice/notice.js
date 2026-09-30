// '공지사항' 화면 전용 스크립트 — 실제 등록된 공지 목록을 불러와서 보여줌
// 주의: notice.html은 fragment로 나중에 fetch되므로, fragments:loaded 이후에
// 요소를 찾아야 함

document.addEventListener("fragments:loaded", () => {
    loadNotices();
    loadTimetable();
    loadFaqs();
});

const ONLINE_COURSE_COLOR_LABEL = { korean: "한국어", computer: "컴퓨터", other: "기타" };

// 사용자가 입력한 텍스트에 혹시 있을 수 있는 HTML 태그를 무력화 (안전하게 표시)
function escapeHtmlForNotice(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
}

async function loadNotices() {
    const list = document.getElementById("noticeList");
    const emptyText = document.getElementById("noticeListEmpty");
    if (!list) return;

    try {
        const res = await fetch("/api/notices");
        if (!res.ok) return;
        const notices = await res.json();

        list.innerHTML = "";
        if (emptyText) emptyText.hidden = notices.length > 0;

        notices.forEach((n) => {
            const li = document.createElement("li");
            li.className = "notice-item";
            li.innerHTML = `
        <span class="notice-date">${n.createdAt}</span>
        <div class="notice-item-body">
          <h3>${escapeHtmlForNotice(n.title)}</h3>
          <p>${escapeHtmlForNotice(n.content)}</p>
        </div>
      `;
            list.appendChild(li);
        });
    } catch (err) {
        console.error(err);
    }
}

const TIMETABLE_DAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];

async function loadTimetable() {
    const tbody = document.getElementById("timetableBody");
    const emptyText = document.getElementById("timetableEmpty");
    const wrap = document.querySelector(".timetable-wrap");
    if (!tbody) return;

    try {
        const res = await fetch("/api/timetable");
        if (!res.ok) return;
        const allEntries = await res.json();

        // 온라인 강좌(VIDEO)는 요일·시간이 없어서 이 표에는 안 넣고, 아래 "강의 시청" 목록에 따로 보여줌
        const entries = allEntries.filter((e) => (e.studyType || "TOGETHER") === "TOGETHER");

        if (wrap) wrap.hidden = entries.length === 0;
        if (emptyText) emptyText.hidden = entries.length > 0;
        tbody.innerHTML = "";

        if (entries.length > 0) {
            // 같은 시작 시간끼리 한 행으로 묶어요 (시간 하나에 요일별로 여러 수업이 있을 수 있음)
            const rowsByTime = new Map();
            entries.forEach((e) => {
                const key = `${e.startTime}~${e.endTime}`;
                if (!rowsByTime.has(key)) rowsByTime.set(key, { startTime: e.startTime, endTime: e.endTime, byDay: {} });
                rowsByTime.get(key).byDay[e.day] = e;
            });

            [...rowsByTime.values()]
                .sort((a, b) => a.startTime.localeCompare(b.startTime))
                .forEach((row) => {
                    const tr = document.createElement("tr");
                    let cellsHtml = `<th scope="row">${escapeHtmlForNotice(row.startTime)}<br>${escapeHtmlForNotice(row.endTime)}</th>`;
                    TIMETABLE_DAYS.forEach((day) => {
                        const entry = row.byDay[day];
                        cellsHtml += entry
                            ? `<td class="tt-${entry.colorType}">${escapeHtmlForNotice(entry.courseName)}</td>`
                            : `<td></td>`;
                    });
                    tr.innerHTML = cellsHtml;
                    tbody.appendChild(tr);
                });
        }

        renderOnlineCourses(allEntries.filter((e) => e.studyType === "VIDEO"));
    } catch (err) {
        console.error(err);
    }
}

function renderOnlineCourses(entries) {
    const list = document.getElementById("onlineCoursesList");
    const emptyText = document.getElementById("onlineCoursesEmpty");
    if (!list) return;

    list.innerHTML = "";
    if (emptyText) emptyText.hidden = entries.length > 0;

    entries
        .sort((a, b) => (a.startTime || "").localeCompare(b.startTime || ""))
        .forEach((e) => {
            const item = document.createElement("div");
            item.className = "online-course-item";
            item.innerHTML = `
        <span class="tt-dot tt-dot--${e.colorType}" aria-hidden="true"></span>
        <span class="online-course-name">${escapeHtmlForNotice(e.courseName)}</span>
        <span class="online-course-period">${escapeHtmlForNotice(e.startTime)} ~ ${escapeHtmlForNotice(e.endTime)}</span>
      `;
            list.appendChild(item);
        });
}

async function loadFaqs() {
    const list = document.getElementById("faqList");
    const emptyText = document.getElementById("faqEmpty");
    if (!list) return;

    try {
        const res = await fetch("/api/faqs");
        if (!res.ok) return;
        const faqs = await res.json();

        list.innerHTML = "";
        if (emptyText) emptyText.hidden = faqs.length > 0;

        faqs.forEach((faq) => {
            const details = document.createElement("details");
            details.className = "faq-item";
            details.innerHTML = `
        <summary class="faq-question">${escapeHtmlForNotice(faq.question)}</summary>
        <p class="faq-answer">${escapeHtmlForNotice(faq.answer)}</p>
      `;
            list.appendChild(details);
        });
    } catch (err) {
        console.error(err);
    }
}