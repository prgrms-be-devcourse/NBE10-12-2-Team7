# 09. Admin 도메인 Spring AI 도입 조사 (회의 발표용)

> 작성: 팀장 / 조사 기준일 2026-06-29
> 목적: Admin 관리자 페이지에 **Spring AI + Ollama(로컬)** 를 도입해, AI 기반 "자원 관리(신고·회원·상품 운영)" 기능으로 확장.
> 전제: 현재 프로젝트 = Spring Boot 3.5.15 / Java 21 / Gradle / MySQL → **Spring AI 1.0 GA 요구사항(Spring Boot 3.4+, Java 17+) 충족**.

---

## 1. 한 줄 결론 (발표용)

> 기존 Admin REST API 위에 **Spring AI `ChatClient` 한 겹**을 얹으면, 관리자가 직접 데이터를 뒤지는 대신
> **AI가 신고를 분류·요약하고, 위험 상품을 탐지하고, 자연어로 운영 질의에 답하는** 관리자 페이지로 확장 가능하다.
> LLM은 **Ollama(로컬·무료)** 로 시작하되, 코드는 **공급자 비종속(provider-agnostic)** 으로 짜서 나중에 클라우드 모델로 교체 가능하게 설계한다.

---

## 2. Spring AI란 & 왜 우리 프로젝트에 맞나

- **Spring AI** = Spring 진영의 공식 AI 애플리케이션 프레임워크. JPA가 DB를 추상화하듯, **LLM 호출을 추상화**한다.
- 1.0 GA(2025) 기준 핵심 추상화:
  | 기능 | 설명 | Admin 활용 |
  |------|------|-----------|
  | `ChatClient` | LLM 호출 fluent API (RestClient와 유사) | 모든 AI 기능의 진입점 |
  | `PromptTemplate` | 프롬프트 템플릿 + 변수 바인딩 | 신고/상품 분석 프롬프트 표준화 |
  | **Structured Output** | LLM 응답을 **Java DTO/record로 자동 매핑** | `ReportTriageResult` 같은 타입 안전 결과 |
  | **Tool/Function Calling** | LLM이 **우리 Java 메서드(=Repository)** 를 호출 | 자연어 운영 질의 → 실제 DB 조회 |
  | Advisors | 호출 전후 가로채기(로깅·필터·RAG) | 감사 로그, 금칙어 필터 |
  | VectorStore / RAG | 임베딩 검색 (향후) | 정책 문서 기반 답변 |
- **우리 프로젝트 적합성**:
  - 이미 `com.dongnemarket.admin.*` 레이어(Controller/Service/Repository)가 있음 → AI는 그 위 **얇은 레이어**로 추가, 기존 구조 안 깨짐.
  - 공통구조(SecurityConfig `/api/admin/** = ROLE_ADMIN`)가 이미 배선됨 → AI 엔드포인트도 그대로 보호됨.

---

## 3. ⚠️ 가장 중요한 현실 제약 — Ollama + AWS 프리티어 (먼저 짚고 갈 것)

- **Ollama는 모델을 구동할 RAM/CPU(이상적으로 GPU)가 필요**하다. 가장 작은 모델도 실사용엔 최소 **4~8GB RAM** 권장.
- **AWS 프리티어 EC2(t2.micro, 1GB RAM)에서는 Ollama 모델 구동이 사실상 불가능**하다. → 배포 서버에서 직접 AI를 돌리려 하면 막힌다.
- **현실적 대안 (권장 순서):**
  1. **개발·시연(데모)은 로컬 PC의 Ollama로** 구동 → 비용 0, 발표/제출에 충분. (가장 권장)
  2. 코드를 **공급자 비종속**으로 설계 → 운영에서 AI가 필요하면 **호스팅 API(Anthropic/OpenAI 등)로 `application.yml` 한 줄 교체**만으로 전환.
  3. 정말 클라우드에서 로컬 모델을 돌려야 하면 **별도 RAM 큰 인스턴스(프리티어 밖, 유료)** 또는 GPU 인스턴스 필요 → 이번 범위에선 비권장.
- **설계 원칙**: `ChatClient` 추상화 덕분에 **비즈니스 코드는 Ollama인지 Anthropic인지 모른다.** 프로파일/설정으로만 결정 → 위 1·2 전략이 코드 변경 없이 가능.

---

## 4. Admin AI 기능 후보 (우리 도메인 기준, 우선순위 순)

> 모든 기능은 **Human-in-the-loop** — AI는 "제안/분류/요약"만 하고, **최종 처분(정지·삭제)은 관리자가 클릭**한다. (환각 리스크 차단)

### ⭐ 후보 1. 신고 자동 분류·요약·우선순위 (report triage) — **PoC 1순위 추천**
- 입력: `Report`(사유, 타입) + 신고 대상 콘텐츠(상품 설명/회원 정보).
- AI 출력(Structured Output → record): `{ severity: HIGH/MID/LOW, category, summary, suggestedAction }`.
- 가치: 관리자가 신고 목록을 일일이 읽지 않고 **우선순위·요약**부터 본다. 우리 `report` 도메인과 직결, PoC로 가장 명확.

### ⭐ 후보 2. 자연어 운영 질의 (NL Query, **Tool Calling 시연용 추천**)
- 관리자가 "최근 7일간 신고가 3건 이상인 회원 보여줘" 입력.
- Spring AI **Tool Calling** 이 `AdminMemberRepository` / `ReportRepository` 메서드를 자동 호출 → 결과를 자연어+표로 응답.
- 가치: "AI 자원관리" 컨셉을 가장 잘 보여주는 데모. 기존 Repository 재사용.

### 후보 3. 위험·사기성 상품 탐지 / 카테고리 자동 추천
- 상품 등록/수정 시 `description`을 AI가 분석 → 금지품·사기 패턴 플래그, 카테고리 추천.
- 가치: `product` 담당 팀원과 협업 포인트(다른 도메인 확장 주제와 연결).

### 후보 4. 관리자 대시보드 일일 요약 리포트
- 매일/요청 시 플랫폼 상태(신규 가입·신고·거래완료 수)를 AI가 **자연어 브리핑**으로 생성.
- 가치: `lead-only/07-daily-pr-summary`(일일 PR 요약)와 컨셉 일관 → 운영 요약 자동화.

### 후보 5. 이상 회원 활동 탐지·요약 (향후)
- 다수 신고/단기 대량 등록 등 패턴을 AI가 요약 설명. RAG/통계와 결합 시 고도화.

---

## 5. 아키텍처 설계 (기존 구조 보존)

```
com.dongnemarket.admin
├── ai
│   ├── AdminAiConfig.java        # ChatClient 빈 (공급자 비종속)
│   ├── controller/AdminAiController.java   # /api/admin/ai/** (ROLE_ADMIN, 기존 SecurityConfig로 보호됨)
│   ├── service/ReportTriageService.java    # 후보1
│   ├── service/AdminQueryService.java      # 후보2 (Tool Calling)
│   ├── tool/AdminQueryTools.java           # @Tool 메서드 → 기존 Repository 위임
│   └── dto/ReportTriageResult.java         # Structured Output record
└── (기존 controller/service/repository 그대로)
```

- **공급자 비종속 핵심**: 서비스는 `ChatClient`만 주입받음. Ollama↔호스팅 전환은 의존성+`application.yml`만 변경.
- 보안: 새 엔드포인트는 `/api/admin/ai/**` → 이미 `SecurityConfig`의 `/api/admin/** = hasRole("ADMIN")` 로 자동 보호. (공통구조 수정 불필요)

---

## 6. 도입 방법 (의존성 · 설정)

`backend/build.gradle` — BOM + Ollama 스타터:
```gradle
ext {
    springAiVersion = '1.0.0'   // 발표 시 최신 GA 패치 버전 확인
}
dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
    }
}
dependencies {
    implementation 'org.springframework.ai:spring-ai-starter-model-ollama'
}
```

`application.yml` (개발 프로파일, 로컬 Ollama):
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.1        # 또는 qwen2.5 등 한국어 양호한 경량 모델
          temperature: 0.2       # 운영 분류용 → 낮게(일관성)
```
로컬 준비: `ollama pull llama3.1` 후 `ollama serve` (기본 11434 포트).

> 전환 예시: 운영에서 호스팅 모델 필요 시 `spring-ai-starter-model-anthropic` 의존성 추가 + `spring.ai.anthropic.*` 설정 → **비즈니스 코드 변경 0**.

---

## 7. 단계적 도입 로드맵 (발표용)

| 단계 | 내용 | 산출물 |
|------|------|--------|
| Phase 0 | 로컬 Ollama 설치 + Spring AI `ChatClient` "hello" PoC | 동작 확인 |
| Phase 1 | **후보1 신고 triage** (Structured Output) | `/api/admin/ai/reports/{id}/triage` |
| Phase 2 | **후보2 자연어 질의** (Tool Calling, Repository 연동) | `/api/admin/ai/query` |
| Phase 3 | 후보3·4 확장 + Advisor(감사 로그·금칙어) | 대시보드 요약 |
| Phase 4 (향후) | RAG/VectorStore로 정책문서 기반 답변 | 고도화 |

---

## 8. 리스크 & 대응

| 리스크 | 대응 |
|--------|------|
| 환각(잘못된 분류/처분) | **AI는 제안만, 처분은 관리자 수동.** Structured Output + 검증 |
| 응답 지연/모델 부하 | 비동기 처리, temperature 낮춤, 경량 모델 |
| 프리티어에서 Ollama 구동 불가 | §3 — 데모는 로컬, 운영은 호스팅 전환 가능 설계 |
| 민감정보 프롬프트 유출 | 로컬 Ollama는 외부 전송 없음(장점). 호스팅 전환 시 마스킹 검토 |
| 공통구조 영향 | AI는 admin 레이어에 격리, SecurityConfig 수정 불필요 |

---

## 9. 참고 자료
- [Spring AI Reference — Ollama Chat](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- [spring-projects/spring-ai (GitHub)](https://github.com/spring-projects/spring-ai)
- [Spring AI with Ollama Tool Support (공식 블로그)](https://spring.io/blog/2024/07/26/spring-ai-with-ollama-tool-support/)
- [Using Ollama with Spring AI — Piotr Minkowski](https://piotrminkowski.com/2025/03/10/using-ollama-with-spring-ai/)
- [Spring AI Integration Guide: OpenAI/Anthropic/Ollama](https://medium.com/@parsairohit2/spring-ai-integration-guide-connect-llms-openai-anthropic-ollama-to-java-spring-boot-600eb12cc8d0)
