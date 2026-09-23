// KWZM Center를 "홈 화면에 추가(설치)"할 수 있게 해주는 최소한의 서비스 워커예요.
// 오프라인 저장은 하지 않고, 그냥 요청을 그대로 서버로 흘려보내기만 해요 —
// 브라우저가 "설치 가능한 앱"으로 인식하려면 서비스 워커가 최소 1개는 등록돼 있어야 하거든요.
self.addEventListener("install", (event) => {
    self.skipWaiting();
});

self.addEventListener("activate", (event) => {
    self.clients.claim();
});

self.addEventListener("fetch", (event) => {
    // 아무것도 가로채지 않고 평소처럼 네트워크로 그대로 보냄
    event.respondWith(fetch(event.request));
});