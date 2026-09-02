// '강의 자료' 화면 전용 스크립트 — 카드를 누르면 PDF 뷰어 모달을 엶
// (fragment가 나중에 로드되므로 document에 이벤트 위임)

function openMaterialModal(pdfId, title) {
    const modal = document.getElementById("materialModal");
    const frame = document.getElementById("materialFrame");
    const titleEl = document.getElementById("materialModalTitle");
    if (!modal || !frame) return;

    // Google Drive 미리보기 링크 형식. REPLACE_WITH_FILE_ID_... 부분을
    // 실제 구글 드라이브 파일 ID로 바꿔주세요.
    frame.src = `https://drive.google.com/file/d/${pdfId}/preview`;
    if (titleEl) titleEl.textContent = title || "강의 자료";

    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
    document.body.classList.add("modal-open");
}

function closeMaterialModal() {
    const modal = document.getElementById("materialModal");
    const frame = document.getElementById("materialFrame");
    if (!modal) return;

    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
    document.body.style.overflow = "";
    if (frame) frame.src = ""; // 재생/로드 중단
}

document.addEventListener("click", (e) => {
    const card = e.target.closest(".material-card");
    if (card) {
        e.preventDefault();
        openMaterialModal(card.dataset.pdf, card.dataset.title);
        return;
    }

    if (e.target.closest("[data-modal-close]")) {
        closeMaterialModal();
        return;
    }

    const arrow = e.target.closest(".carousel-arrow");
    if (arrow) {
        const track = arrow.closest(".material-carousel")?.querySelector(".material-grid--scroll");
        if (track) {
            const amount = track.clientWidth * 0.8;
            const isPrev = arrow.classList.contains("carousel-arrow--prev");
            track.scrollBy({ left: isPrev ? -amount : amount, behavior: "smooth" });
        }
    }
});

document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeMaterialModal();
});

// 우클릭(다운로드/이미지로 저장 메뉴) 방지 — 보조 수단일 뿐, 완벽한 차단은 아님
document.addEventListener("contextmenu", (e) => {
    if (e.target.closest(".material-modal-body")) {
        e.preventDefault();
    }
});