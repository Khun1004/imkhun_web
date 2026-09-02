// imkhun 메인 사이트에서 "나만의 공부 화면" 탭을 누르면
// 팝업이 아니라 완전히 새 창(새 탭)으로 관리자 페이지를 엶
document.querySelector('.tab-study')?.addEventListener("click", (e) => {
    e.preventDefault();
    window.open("admin/index.html", "_blank");
});