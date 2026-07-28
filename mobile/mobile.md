# mobile — Android 네이티브 클라이언트

마켓온 Android 앱. **Kotlin + Jetpack Compose**, 백엔드 API를 그대로 사용하는 독립 클라이언트다.

> 이 문서는 `mobile/`를 이해하는 진입점이다. 리포 전체는 [../README.md](../README.md).
> **백엔드 불가침** — 모바일 작업으로 `backend/`를 수정하지 않는다. API가 부족하면 백엔드 쪽에 별도로 요청한다.

## 스택

| | |
|---|---|
| 언어 | Kotlin |
| UI | Jetpack Compose |
| DI | Hilt |
| 네트워크 | Retrofit (API 인터페이스는 손으로 작성) |
| 빌드 | Gradle (Kotlin DSL, `libs.versions.toml` 버전 카탈로그) |

## 구조 — 3계층

```
mobile/app/src/main/java/com/dongnemarket/mobile/
├── data/
│   ├── remote/       Retrofit API 인터페이스 · DTO
│   ├── local/        로컬 저장 (토큰 등)
│   ├── mapper/       DTO ↔ 도메인 모델 변환
│   └── repository/   Repository 구현
├── domain/
│   ├── model/        도메인 모델 (UI가 쓰는 형태)
│   └── repository/   Repository 인터페이스 (data가 구현)
├── di/               Hilt 모듈
└── ui/
    ├── login/ home/ productdetail/ chat/    화면별 Composable + ViewModel
    ├── component/    공용 컴포넌트
    ├── navigation/   화면 이동
    └── theme/        테마
```

의존 방향은 **`ui → domain ← data`** 다. `ui`는 `domain`의 인터페이스만 알고 `data` 구현을 모른다.

## 화면 (Phase 1)

| 화면 | 내용 |
|---|---|
| `login` | 로그인 |
| `home` | 상품 목록 |
| `productdetail` | 상품 상세 |
| `chat` | 채팅 목록 · 채팅방 |

## 빌드 · 테스트

```bash
cd mobile && ./gradlew assembleDebug
```

```bash
cd mobile && ./gradlew test        # ViewModel · Repository 단위 테스트
```

실행은 Android Studio에서 에뮬레이터로 확인한다.

## 주의

- API 스펙의 정본은 백엔드 **Swagger**다. DTO를 손으로 작성하므로 백엔드 변경 시 어긋날 수 있다.
- 화면·계층이 추가되면 이 문서의 구조를 **같은 PR에서** 갱신한다.
