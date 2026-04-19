# `.codex/skills` 네이밍 규칙

이 문서는 저장소 안에서 **실행용 custom skill** 이름을 어떻게 정할지 정리한 규칙 문서입니다.

기본 원칙:
- 실행되는 skill의 실제 위치는 `.codex/skills/`
- skill 이름은 **짧고, 검색 가능하고, 역할이 분명해야 함**
- 이름만 보고도 **플랫폼 / 목적 / 범위**를 어느 정도 알 수 있어야 함

---

## 1. 기본 규칙

### 1) kebab-case 사용
- 디렉터리 이름과 skill 이름은 모두 `kebab-case`를 사용합니다.

예:
- `web-bootstrap`
- `android-retrofit`
- `code-review`

피해야 할 예:
- `WebBootstrap`
- `android_retrofit`
- `androidRetrofit`

### 2) 디렉터리명과 skill 이름을 맞춘다

```text
.codex/skills/<skill-name>/SKILL.md
```

예:
```text
.codex/skills/web-bootstrap/SKILL.md
```

그리고 파일 내부 frontmatter도 같은 이름을 사용합니다.

```yaml
name: web-bootstrap
```

### 3) “무엇을 하는지”가 이름에 드러나야 한다
- 추상적 이름보다 **행동/영역이 보이는 이름**을 우선합니다.

좋은 예:
- `web-neis-bff`
- `android-viewmodel`
- `web-ui-builder`

나쁜 예:
- `helper`
- `builder`
- `project-skill`

---

## 2. 추천 네이밍 패턴

### 패턴 A: 플랫폼 + 주제
가장 추천하는 기본 패턴입니다.

```text
<platform>-<topic>
```

예:
- `android-architecture`
- `android-retrofit`
- `web-bootstrap`
- `web-pwa`

언제 쓰나:
- 특정 플랫폼에 묶인 반복 작업일 때
- Android / Web 규칙을 명확히 분리하고 싶을 때

### 패턴 B: 플랫폼 + 주제 + 역할
조금 더 세분화가 필요할 때 씁니다.

```text
<platform>-<topic>-<action>
```

예:
- `web-dashboard-builder`
- `web-neis-response-mapper`
- `android-widget-review`

언제 쓰나:
- 같은 주제 안에서도 여러 스킬이 생길 때
- 범위를 더 분명히 구분해야 할 때

### 패턴 C: 범용 작업 이름
플랫폼 독립 스킬은 플랫폼 접두사 없이 둡니다.

예:
- `analyze`
- `plan`
- `code-review`
- `security-review`

언제 쓰나:
- Android/Web 모두에서 공통으로 쓰는 흐름일 때
- 저장소 전체 자동화 성격일 때

---

## 3. 플랫폼별 권장 접두사

### Android 계열
- `android-*`

예:
- `android-architecture`
- `android-data-layer`
- `android-viewmodel`
- `android-coroutines`
- `android-retrofit`
- `android-accessibility`

### Web 계열
- `web-*`

예:
- `web-bootstrap`
- `web-neis-bff`
- `web-ui-builder`
- `web-pwa`

### Compose / Kotlin 특화
완전히 Android 하위 개념이라면 `android-*`로 합칠 수도 있지만,  
특정 기술 자체를 직접 다루는 범용성이 있으면 기술명을 유지할 수 있습니다.

현재 예:
- `compose-navigation`
- `compose-ui`
- `kotlin-concurrency-expert`

권장 기준:
- 프로젝트 안에서 Android 전용으로만 쓸 거면 `android-compose-navigation`처럼 통합할 수 있음
- 기술 중심 skill로 따로 의미가 있으면 현재처럼 유지 가능

---

## 4. 이름을 정할 때 체크리스트

새 skill을 만들기 전에 아래를 확인합니다.

1. 이 skill은 **플랫폼 전용**인가?
   - 예 → `android-*`, `web-*`
   - 아니오 → 범용 이름 고려

2. 이 skill은 **무엇을 하는지 이름만으로 보이는가?**
   - 안 보이면 더 구체적으로 바꾼다

3. 같은 주제의 skill이 앞으로 늘어날 가능성이 있는가?
   - 있으면 `<platform>-<topic>-<action>` 패턴 고려

4. 기존 skill 이름들과 검색/정렬상 일관적인가?
   - 예: `web-*`끼리 같이 모여 보이는가

---

## 5. 현재 저장소 기준 추천 분류

### 범용
- `analyze`
- `plan`
- `code-review`
- `security-review`
- `trace`

### Android
- `android-architecture`
- `android-data-layer`
- `android-viewmodel`
- `android-coroutines`
- `android-retrofit`
- `android-accessibility`

### Web
- `web-bootstrap`
- `web-neis-bff`
- `web-ui-builder`
- `web-pwa`

### 기술 특화
- `compose-navigation`
- `compose-ui`
- `kotlin-concurrency-expert`

---

## 6. 피해야 할 네이밍 패턴

- 너무 포괄적인 이름
  - `web-tools`
  - `android-helper`
- 목적이 안 보이는 이름
  - `starter`
  - `standard`
  - `default`
- 서로 다른 계층이 뒤섞인 이름
  - `web-android-shared-builder`
- 실행 동작이 아닌 문서 제목 같은 이름
  - `web-notes`
  - `android-guide`

이런 이름은 실제로 “언제 써야 하는지”가 흐려집니다.

---

## 7. 최종 권장안

새 custom skill은 기본적으로 아래 우선순위로 이름을 정합니다.

1. **범용이면 단순 명사/행동 이름**
   - `analyze`, `plan`
2. **플랫폼 전용이면 `<platform>-<topic>`**
   - `web-pwa`, `android-retrofit`
3. **주제가 커지면 `<platform>-<topic>-<action>`**
   - `web-dashboard-builder`

핵심은:
- 짧게
- 구체적으로
- 플랫폼이 보이게
- 검색하기 쉽게

이 문서는 `.codex/skills/` 아래 custom skill을 추가하거나 이름을 바꿀 때 기준 문서로 사용합니다.
