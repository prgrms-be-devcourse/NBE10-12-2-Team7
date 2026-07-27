---
id: FEAT-mobile-phase1
type: feature
status: in-progress
author: jomin4
date: 2026-07-27
related: []
tags: [모바일, Android, Compose, 인증, 채팅, 상품]
---

# FEAT-mobile-phase1 — 마켓온 Android 앱 Phase 1 (로그인·홈목록·상세·채팅)

> 기존 Spring 백엔드(`/api/**` + JWT)를 소비하는 **네이티브 Android 클라이언트**의 첫 동작 버전. 백엔드는 수정하지 않았다.
> 설계 정본: Notion [아키텍처](https://app.notion.com/p/3a9cd6a1d0238160aa95c4499792ac66) · [화면설계](https://app.notion.com/p/3a9cd6a1d02381dca5b7d5b952fa341d) · 기능 페이지 4개(03 개발)

## 1. 개요 · 진입점

앱은 `MainActivity` 하나(single-Activity)이고, 화면 전환은 `MarketOnNavHost` 안에서 Composable을 갈아 끼운다. 시작 화면은 **로그인 고정**(자동 로그인은 Phase 3).

| 라우트 | 화면 | 인자 |
| --- | --- | --- |
| `login` | LoginScreen | – |
| `home` | HomeScreen | – |
| `productDetail/{productId}` | ProductDetailScreen | `productId: Long` |
| `chatList` | ChatListScreen | – |
| `chatRoom/{roomId}` | ChatRoomScreen | `roomId: Long` |

경로 인자는 **화면 파라미터로 넘기지 않는다.** 각 ViewModel이 `SavedStateHandle`에서 `MarketOnRoutes.ARG_PRODUCT_ID` / `ARG_ROOM_ID`로 직접 꺼낸다 → NavHost의 `navArgument` 이름이 이 상수와 어긋나면 **컴파일은 되고 런타임에 터진다.**

빌드 변형별 서버: `BuildConfig.BASE_URL` — debug `http://10.0.2.2:8080/`(에뮬에서 본 PC의 localhost), release `https://marketon.inyeon.io/`.

## 2. 계층 구성 (파일 지도)

전부 `mobile/app/src/main/java/com/dongnemarket/mobile/` 아래. Kotlin 102파일.

**공통 기반 (Unit 0)**
- `MarketOnApp.kt`(@HiltAndroidApp) · `MainActivity.kt`(@AndroidEntryPoint)
- `di/NetworkModule.kt` — Json · OkHttpClient · Retrofit 싱글톤
- `data/remote/ApiCall.kt` — `apiCall{}` / `apiCallForUnit{}` (**모든 API 호출의 단일 관문**)
- `data/remote/AuthInterceptor.kt` — Bearer 자동 부착, `/api/auth/{login,signup,reissue}` 제외
- `data/remote/dto/ApiEnvelope.kt` — 백엔드 응답 껍데기 + 에러 껍데기
- `data/remote/BigDecimalSerializer.kt` · `data/local/TokenDataStore.kt`
- `ui/theme/{Color,Theme,Type}.kt` — **라이트 전용**, radius 18dp, `StatusBadgeColors`
- `ui/navigation/{MarketOnRoutes,MarketOnNavHost}.kt`

**Domain** (`domain/`) — 13 모델 · 7 Repository 인터페이스
- `model/`: AppError · Product · ProductDetail · ProductPage · TradeStatus · Category · Region · Member · MemberLocation · ChatRoom · ChatMessage · ChatMessagePage · ChatRoomHeader
- `repository/`: Auth · Member · Product · Category · Region · Favorite · Chat

**Data** (`data/`) — ApiService 9 · DTO 7 · Mapper 5 · RepositoryImpl 7
- `remote/`: Auth · Member · Product · Category · Region · Favorite · Chat ApiService
- `mapper/`: Product · Catalog · Member · Chat · **ImageUrlMapper**(상대경로 → 절대 URL)

**DI** (`di/`) — 도메인마다 `<X>ApiModule`(object·@Provides) + `<X>RepositoryModule`(abstract·@Binds) 2개씩. **한 파일에 섞으면 Dagger 컴파일 에러.**

**UI** (`ui/`)
- `login/` 3 · `home/` 3 + component 4 · `productdetail/` 3 + component 3 · `chat/` 6 + component 5
- `component/` 6 — StatusBadge · ProductPrice · RelativeTime · StateViews · NetworkImage · MarketOnBottomBar (**4화면 공용, 수정 시 파급 주의**)

## 3. 데이터 흐름 (한 바퀴)

```
사용자 액션 → Screen(이벤트 람다) → ViewModel(함수)
  → Repository → apiCall { ApiService } → OkHttp(AuthInterceptor: Bearer 부착)
  → /api/** → ApiEnvelope<Dto> 파싱
  → apiCall이 껍데기 해체 · 실패는 AppError로 번역 → Result<T>
  → Repository가 DTO→도메인 매핑 → ViewModel이 UiState 갱신(StateFlow)
  → Screen 재구성
```

핵심 규약 3개:
- Repository는 **예외를 던지지 않는다.** 항상 `Result<T>`, 실패는 `AppError`.
- 화면은 `AppError.userMessage`만 읽는다. 백엔드 `error` 코드 문자열은 화면에 노출 금지.
- 화면은 NavController를 모른다. `onXxx` 람다로 이동 의도만 알리고 배선은 NavHost가 한다.

## 4. 소비 API

전부 `ApiResponse` 껍데기(`{status, message, data}`)로 감싸여 온다. 경로는 Retrofit 애노테이션에 **선행 `/` 없이** 쓴다(붙이면 baseUrl의 path가 잘린다).

| 메서드 | 경로 | 쓰는 화면 | 비고 |
| --- | --- | --- | --- |
| POST | `api/auth/login` | 로그인 | 요청에 `autoLogin` 필수(§7-1) |
| POST | `api/auth/logout` | – | 응답 `data` 키 없음 |
| GET | `api/members/me` | 채팅·상세 | **세션 확인 + 내 memberId의 유일한 출처** |
| GET | `api/members/me/locations` | 홈 | 미설정이면 `[]`(에러 아님) |
| PUT | `api/members/me/locations` | – | regions 최대 2개 |
| GET | `api/products` | 홈 | **커서 페이징**(`items`/`nextCursor`/`hasNext`) |
| GET | `api/products/search` | 홈(검색·카테고리) | **페이징 없음, 전량 반환** |
| GET | `api/products/{productId}` | 상세 | 조회수를 올리는 **쓰기 동작** |
| GET | `api/categories` | 홈 | 키가 `id` |
| GET | `api/regions` | – | 229건 전량 |
| GET | `api/members/me/favorites` | 상세 | 하트 초기 상태용, **최근 200건 캡** |
| POST/DELETE | `api/products/{productId}/favorites` | 상세 | POST 201, DELETE는 `data` 키 없음 |
| POST | `api/chat-rooms` | 상세 | **get-or-create 멱등** |
| GET | `api/chat-rooms` | 채팅목록·방헤더 | 전량, 페이징 없음 |
| GET/POST | `api/chat-rooms/{roomId}/messages` | 채팅방 | 응답 `messages`는 **id DESC** |
| POST | `api/chat-rooms/{roomId}/read` | 채팅방 | 요청 body·응답 data 모두 없음 |

## 5. 상태 · 에러 매핑

| 화면 | UiState | 형태 |
| --- | --- | --- |
| 로그인 | `LoginUiState` | data class (입력 폼이라 sealed 부적합) |
| 홈 | `HomeUiState` | sealed — Loading / Success / Error |
| 상세 | `ProductDetailUiState` | sealed — Loading / Success / Error |
| 채팅목록 | `ChatListUiState` | sealed — Loading / Success / Error |
| 채팅방 | `ChatRoomUiState` | data class |

`AppError` 5종 → 화면:

| 실패 | AppError | 화면 처리 |
| --- | --- | --- |
| 네트워크 끊김·타임아웃(IOException) | `Network` | "네트워크 연결을 확인해 주세요." |
| HTTP 401 | `Unauthorized` | 사용자 문구 표시 |
| 4xx/5xx | `Api(status, code, message)` | 서버 `message` 표시, `code`는 분기용 |
| `data` 키 부재인데 값이 필요 | `EmptyBody` | "서버 응답이 올바르지 않습니다." |
| 파싱 실패 등 | `Unknown` | "알 수 없는 오류가 발생했습니다." |

**실패해도 화면을 덮지 않는 것들**(의도된 설계 — 되돌리지 말 것):
- 홈: 카테고리·동네 조회 실패 → 그 부분만 비우고 목록은 보여준다
- 홈: 다음 페이지 append 실패 → 조용히 무시(`hasNext` 유지 → 재스크롤 시 재시도)
- 채팅방: 폴링 실패 → **이미 받은 메시지가 있으면 스낵바도 안 띄운다**(3초마다 재등장 방지)
- 채팅방: 헤더(`getRoomHeader`) 실패 → 헤더만 접고 대화는 표시
- 상세: 카테고리 이름·내 프로필 조회 실패 → 본문은 Success 유지

**빈 결과는 Error가 아니다.** 상품 0건·채팅방 0건은 `Success` + 빈 상태 UI.

## 6. 주요 설계 결정 (D#)

- **D1. 찜 상태를 앱 전역 캐시로 둔다** — 상품 상세 응답에 "내가 찜했는지"가 **구조적으로 없다**(상세가 permitAll이고 `@AuthenticationPrincipal`을 받지 않아 서버가 요청자를 모름, 단건 조회 API도 없음). `FavoriteRepository.favoriteProductIds: StateFlow<Set<Long>>`를 `@Singleton`으로 두고 화면이 구독한다.
- **D2. 채팅방 헤더는 목록 조회로 우회** — `GET /api/chat-rooms/{roomId}` 단건 API가 없다. `getRoomHeader(roomId)`가 전량 목록을 받아 찾는다. 방 진입마다 목록 1회 호출이 대가.
- **D3. `ChatRoomHeader`는 두 응답의 교집합** — 방 생성은 `seller`, 목록은 `opponent`로 키가 다르다. 이 모델의 존재 이유가 그 비대칭 흡수다.
- **D4. UseCase 계층 없음** — 현재 모든 화면 로직이 단순 위임이라 ViewModel이 Repository를 직접 쓴다(workflow §2.1 "로직 있을 때만").
- **D5. 폴링은 화면 주도** — ViewModel은 `startPolling()`/`stopPolling()`만 공개하고, 화면이 `PollingEffect`(`repeatOnLifecycle(STARTED)`)로 켜고 끈다. 백그라운드에서 배터리·네트워크를 태우지 않기 위함.
- **D6. 라이트 전용** — `isSystemInDarkTheme()`을 쓰지 않는다. 기기 설정과 무관하게 항상 같은 화면.
- **D7. 구현하지 않은 것** (API가 없어서 — 가짜 데이터로 채우지 않음): 홈 히어로 숫자 통계 · 상품 작성 시각 · "상대가 읽음" 표시 · 상품 등록 FAB · 찜/내정보 탭(`enabled=false`).

## 7. ⚠️ 알려진 함정 · 디버깅 포인트

### 증상별 진입점

| 증상 | 어디부터 보나 |
| --- | --- |
| **로그인이 "네트워크 연결을 확인해 주세요"로 실패** | ① 백엔드가 8080에 떠 있나 ② **Redis가 떠 있나**(없으면 로그인 불가) ③ debug 빌드인가(`10.0.2.2`는 debug 전용) ④ `app/src/debug/AndroidManifest.xml`의 `usesCleartextTraffic`이 병합됐나 |
| **목록이 비어 있음** | ① 서버가 삭제·숨김·거래완료·비활성판매자를 **4중 필터**로 거른다 — DB에 10건이어도 6건만 온다 ② 내 동네가 설정돼 있으면 그 지역으로 필터된다 ③ OkHttp 로그(`BODY`, debug만)에서 실제 응답 확인 |
| **필드가 통째로 null** | DTO 필드명 오타. 서버 키와 1글자만 달라도 `ignoreUnknownKeys=true` 때문에 **예외 없이 조용히 null**이 된다. §7-4의 비대칭 목록부터 확인 |
| **가격이 이상하게 표시** | `BigDecimal` 스케일(§7-3) |
| **이미지가 안 뜸** | ① 서버가 상대경로를 준다 → `ImageUrlMapper`가 BASE_URL을 붙였나 ② 데모 데이터는 `thumbnailUrl=null`·`imageUrls=[]`가 **정상** → 플레이스홀더가 맞는 동작 |
| **채팅이 거꾸로 보임** | 서버는 `messages`를 id DESC로 준다. `ChatMapper`의 뒤집기 확인 |
| **하트가 꺼진 채로 시작** | 찜 200건 캡(§7-5) 또는 `FavoriteRepositoryImpl`의 `@Singleton` 누락(인스턴스가 갈라지면 캐시가 어긋난다) |
| **화면 이탈 후에도 네트워크 호출** | `PollingEffect` 배선 확인. `viewModelScope`에 무한 루프를 직접 띄우면 안 된다 |
| **앱 복귀 시 크래시** | `ChatRoomViewModel`의 `checkNotNull(savedStateHandle.get<Long>(ARG_ROOM_ID))` — 프로세스 사망 복원 경로. `adb shell am kill com.dongnemarket.mobile`로 재현 |

### 백엔드 계약의 지뢰 (전부 원본 대조로 확인함)

1. **`encodeDefaults`** — kotlinx.serialization 기본값이 `false`라 **선언 기본값과 같은 값은 요청 본문에서 통째로 생략**된다. `LoginRequestDto.autoLogin = true`가 안 나가서 서버가 false로 읽고 refreshToken을 Max-Age 없는 세션 쿠키로 내려주고 있었다. `NetworkModule`에 `encodeDefaults = true`로 해결. **기본값을 가진 요청 DTO를 추가할 때 이 설정을 기억할 것.**
2. **`data` 키가 사라진다** — `ApiResponse`에만 `@JsonInclude(NON_NULL)`이 걸려 있어 `data`가 null이면 키 자체가 없다(logout · 찜 취소 · 읽음 처리). 이런 엔드포인트는 **`apiCallForUnit`**을 써야 한다. `apiCall`을 쓰면 성공을 `EmptyBody` 실패로 오판한다.
3. **`price`는 BigDecimal** — GET은 `800000.00`, POST 응답은 `800000`으로 스케일이 다르다. `equals`는 false를 낸다 → **`compareTo == 0`으로 비교**해야 한다.
4. **응답 키 비대칭** — 방 생성 `seller` vs 방 목록 `opponent` / 찜 등록 `id` vs 찜 목록 `favoriteId` / 카테고리 `id` vs 지역 `regionId` / 페이지 아이템 상품 `items` vs 채팅 `messages`. **공용 제네릭 페이지 클래스로 묶으면 깨진다.**
5. **찜 목록은 최근 200건 캡** — 초과 사용자는 하트가 꺼져 보인다. POST의 409("이미 찜함")를 성공으로 흡수해 자기치유하도록 해뒀다.
6. **날짜에 오프셋이 없고 소수부 자릿수가 가변** — `Instant.parse`/`OffsetDateTime.parse`는 **반드시 실패**한다. `ISO_LOCAL_DATE_TIME` + KST 가정(`RelativeTime.kt`). 파싱 실패 시 크래시 대신 원문 표시.
7. **상품에 시간 필드가 없다** — 엔티티엔 있으나 DTO가 노출하지 않는다. 카드·상세에서 "3시간 전"을 만들 수 없어 그 자리에 `region`을 쓴다.
8. **`regions`는 regionId가 아니라 이름 문자열**, 반복 파라미터(`?regions=A&regions=B`), **최대 2개**(3개↑ 400).
9. **permitAll 경로는 만료 토큰에도 200 + 익명** — `GET /api/products`·`/{id}`·`/api/categories/**`·`/api/regions`. 세션 확인은 반드시 `GET /api/members/me`로.
10. **클라이언트 요청 실수가 400이 아니라 500** — `GlobalExceptionHandler`가 `ResponseEntityExceptionHandler`를 상속하지 않아 `cursor=abc`·깨진 JSON·경로변수 타입 불일치가 전부 500이다. **500을 무조건 서버 장애로 단정하지 말 것.**
11. **WebSocket/STOMP/SSE가 백엔드에 0건** — 채팅은 REST 폴링뿐이고, 증분 파라미터(`since`/`after`)도 없어 첫 페이지 전체를 다시 받아 최대 messageId 초과분만 append한다.
12. **판매자는 자기 상품에 채팅방을 못 만든다** — `CANNOT_CHAT_WITH_SELF`(400). 상세에서 내 상품이면 버튼을 아예 비활성으로 둔다.

### 빌드·테스트 환경 함정

13. **`mobile/`에 `gradlew`가 없다.** CLI 빌드는 캐시된 배포판 + JBR 21로:
    `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ~/.gradle/wrapper/dists/gradle-8.10.2-bin/*/gradle-8.10.2/bin/gradle -p mobile :app:assembleDebug`
    (기본 `java`는 JDK 25라 Gradle 8.10.2와 맞지 않는다.)
14. **espresso 3.5.0은 Android 15+에서 죽는다** — Compose BOM이 전이로 끌고 오는 버전인데 `InputManager.getInstance` 리플렉션이 플랫폼에서 사라져 `Espresso.onIdle`이 `NoSuchMethodException`을 던진다. 계기 테스트 64개가 **전부** 이 하나로 실패했었다. `espresso 3.7.0` / `test-runner 1.7.0`을 명시 고정해 해결.
15. **계기 테스트 함수명에 공백·쉼표를 쓸 수 없다** — minSdk 26 → DEX 039의 SimpleName 제약(`Space characters in SimpleName ... not allowed prior to DEX version 040`). 백틱 한국어 테스트명은 `_`로. JVM 단위 테스트는 제약 없음.
16. **Android Lint는 Compose를 거의 못 본다** — `HardcodedText`·접근성 검사가 XML 레이아웃 한정이라, 한국어 하드코딩 310개와 `contentDescription = null` 9곳에 아무 말도 안 한다. Compose 규칙이 필요하면 `compose-lints` 별도 도입.

### 미해결 (팀 합의 필요)

- **`regions` 문자열 trim 정책 충돌** — `CatalogMapper.toRegionDomain()`은 "서버 원문 보존"(§7-18 근거), `ProductRepositoryImpl.normalizeRegions()`는 `trim()`. **양쪽 테스트가 서로 반대 방향을 명시적으로 고정**하고 있어 임의 판정하지 않고 함수 KDoc에 기록만 해뒀다. 지역 마스터 데이터에 앞뒤 공백이 든 이름이 실제로 있는지 확인하면 결론이 난다(없으면 무해, 있으면 조용한 "결과 0건").
- **`ChatInputBar(enabled = !isSending)`** — 전송 왕복 중 입력창까지 비활성이라 키보드가 닫힌다. 중복 전송은 `canSend` + ViewModel 가드로 이미 이중 방어되어 불필요할 수 있으나, 현재 동작을 계기 테스트가 "의도된 설계"로 고정하고 있어 보류.

## 8. 테스트 · 참고

**테스트 395개, 전부 통과** (커밋 `e024859`)

| 위치 | 개수 | 실행 | 도구 |
| --- | --- | --- | --- |
| `app/src/test/.../ui/**ViewModelTest.kt` | 105 | JVM | JUnit4 · MockK · Turbine · coroutines-test |
| `app/src/test/.../data/repository/*Test.kt`, `mapper/ProductMapperTest.kt` | 126 | JVM | 동일 |
| `app/src/test/.../data/remote/*ApiContractTest.kt` | 100 | JVM | **MockWebServer** — 경로·쿼리 직렬화·Bearer 헤더·파싱·에러 매핑 |
| `app/src/androidTest/.../ui/**ScreenTest.kt` | 64 | **에뮬레이터** | Compose UI test (Hilt 미사용, 가짜 Repository 수동 주입) |

실행:
```
:app:testDebugUnitTest              # JVM 331 (--rerun 을 붙여야 캐시를 건너뛴다)
:app:connectedDebugAndroidTest      # 계기 64 (에뮬레이터 필요)
```

**로컬 검수 환경**
- Docker는 `dongne-mysql`(3306)·`dongne-redis`(6379)만. 백엔드는 직접 실행: `backend/gradlew bootRun --args='--spring.profiles.active=dev'`
- **`demo` 프로파일 금지** — `ddl-auto: create`라 스키마를 날린다. DB는 이미 시딩돼 있다(회원 8·상품 10·카테고리 8·지역 229).
- 로그인: `user01@dongnemarket.com` / `user1234!` (user04=정지 403, user05=탈퇴 400은 **의도된 실패 케이스**)
- 채팅 테스트는 **남의 상품**에서 시작할 것(자기 상품은 `CANNOT_CHAT_WITH_SELF`)

**참고**
- 커밋: `8f63c23`(Phase 0 스캐폴딩) → `ce2f42f`(Unit 0 기반) → `fb40029`(화면 4개) → `e024859`(테스트 + 버그 9건 수정)
- 워크플로 정본: `.claude/skills/mobile-workflow/`
