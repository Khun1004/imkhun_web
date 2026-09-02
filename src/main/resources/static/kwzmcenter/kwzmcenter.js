// imkhun 메인 사이트에서 "KWZM Center" 탭을 누르면
// 팝업이 아니라 완전히 새 창(새 탭)으로 KWZM Center 페이지를 엶
document.querySelector('.tab-teach')?.addEventListener("click", (e) => {
    e.preventDefault();
    window.open("kwzmcenter/index.html", "_blank");
});