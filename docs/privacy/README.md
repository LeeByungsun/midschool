# 개인정보처리방침 게시 방법 — 웹서버 없이

**권장:** GitHub Pages에 개인정보처리방침만 올립니다. 무료 GitHub 계정의 공개 저장소에서 정적 웹페이지를 게시할 수 있고, 별도 웹서버·도메인·결제는 필요하지 않습니다. 앱 코드 저장소가 비공개라면 **방침 전용 공개 저장소**를 만드세요. 운영자 이메일과 방침 내용은 누구나 볼 수 있으므로 공개해도 되는 문의용 주소를 사용합니다.

## 준비된 문서

- 전체 공개 문안: [`../privacy-policy.ko.md`](../privacy-policy.ko.md)
- 앱 내 간단 안내 문안: [`../privacy-collection-summary.ko.md`](../privacy-collection-summary.ko.md)

## 게시 순서

1. 전체 문안의 대괄호 자리표시자(운영자/개발자 명칭, 문의용 이메일, 실제 Google Analytics 보관 기간)를 **실제 값으로 바꿉니다**. Play Console 개발자 표기와 운영자 명칭을 맞춥니다. 앱의 가정통신문 API 주소도 출시 빌드와 비교합니다.
2. GitHub에 `schoolhelper-privacy` 같은 **공개 저장소**를 만듭니다. 문안을 `index.md`로 저장소 루트에 복사합니다. 필요하면 별도의 `index.html`로 변환해도 됩니다.
3. 저장소 **Settings → Pages → Build and deployment → Deploy from a branch**에서 `main` / `/ (root)`를 선택합니다.
4. 게시된 `https://<GitHub 사용자명>.github.io/schoolhelper-privacy/`를 로그아웃/시크릿 창과 휴대전화에서 열어 확인합니다. GitHub 사용자·저장소 이름이 다르면 URL도 달라집니다.
5. **동일한 공개 URL**을 Play Console의 개인정보처리방침 필드와 앱 내 개인정보처리방침 링크에 연결합니다. 현재 앱에는 방침 링크가 없으므로 출시 전 UI/문자열을 추가해야 합니다. Play Console **데이터 보안(Data safety)** 답변도 문안 및 실제 SDK 동작과 대조합니다.

GitHub 저장소의 소스 페이지나 편집 가능한 문서 링크가 아니라 **게시된 Pages URL**을 사용하세요. Google Play는 누구나 접근 가능한 정상 웹페이지(지역 제한·PDF·수정 가능한 문서 제외)를 요구합니다. GitHub Pages는 `index.md`, `index.html`, `README.md`를 진입 파일로 인식합니다.

## 출시 전 확인

- [ ] 운영자/개발자 명칭·문의 이메일 입력
- [ ] Google Analytics의 **데이터 보관 기간** 실제 설정값 입력
- [ ] 가정통신문 API 및 기타 외부 전송처가 출시 빌드와 일치
- [ ] Pages URL 공개 접속 확인, 앱 내부 링크·Play Console URL 동일
- [ ] Play Console 데이터 보안 답변과 선택 동의·SDK 데이터 처리 일치
- [ ] 청소년 대상 앱의 Play 정책/법적 요건 별도 검토

참고: [Google Play 개인정보처리방침 요건](https://support.google.com/googleplay/android-developer/answer/10144311), [GitHub Pages 시작](https://docs.github.com/en/pages/quickstart), [GitHub Pages 진입 파일](https://docs.github.com/en/pages/getting-started-with-github-pages/creating-a-github-pages-site), [Firebase 데이터 처리](https://firebase.google.com/support/privacy).
