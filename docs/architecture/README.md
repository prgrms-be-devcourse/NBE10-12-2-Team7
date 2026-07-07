# 아키텍처 (architecture)

마켓온 시스템을 **구조적으로 이해**하기 위한 문서. [C4 모델](https://c4model.com/)의 상위 3개 레벨과 데이터 모델(ERD)로 구성한다.

> 최종 수정일: 2026-07-07 · 상태: draft
> 코드가 정본이다. 구조가 바뀌면 관련 코드 PR에서 이 문서도 함께 갱신한다.

| 문서 | 레벨 | 내용 |
| --- | --- | --- |
| [01-context.md](01-context.md) | C4 L1 · System Context | 시스템 경계 — 누가 쓰고, 무엇과 연동되나 |
| [02-container.md](02-container.md) | C4 L2 · Container | 배포 단위(컨테이너)와 통신 — nginx·next·app·mysql·관측 |
| [03-component.md](03-component.md) | C4 L3 · Component | 백엔드 도메인/계층 구조 |
| [04-erd.md](04-erd.md) | Data Model | 테이블·관계·주요 enum |

## 한 문단 요약

**마켓온**은 지역 기반 중고거래 REST API 서비스다. 사용자는 브라우저(Next.js)로 접근하고, 모든 트래픽은 **nginx 단일 현관**을 지나 프론트(`/`)와 백엔드 API(`/api`)로 갈린다. 백엔드는 **Spring Boot 3.5 / Java 21** 단일 애플리케이션이며 도메인별 계층형(Controller·Service·Repository) 구조다. 데이터는 **MySQL 8**에 저장한다. 이메일 인증·비밀번호 재설정은 **Gmail SMTP**, 관리자 AI 어시스턴트는 **사내 Ollama(Spring AI)** 를 외부 의존으로 사용한다. 관측(Prometheus·Loki·Grafana)과 임시 외부 노출(Cloudflare Tunnel)은 선택 프로파일로 붙는다.
