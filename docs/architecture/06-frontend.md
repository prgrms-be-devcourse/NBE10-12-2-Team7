# 프론트엔드 개요

> 최종 수정일: 2026-07-07 · 상태: draft

`next` 컨테이너(Next.js 16, App Router)의 구조. 백엔드 API와의 연동·배포 위치는 [02-container.md](02-container.md), 컨테이너 관점은 상위 문서 참고.

## 스택

- **Next.js 16** (App Router) · React 19 · TypeScript · Tailwind
- API 호출은 **같은 origin `/api`** 로 (nginx 또는 dev 프록시가 백엔드로 전달 — CORS 없음)

## 디렉터리

```
frontend/src/
├── app/          라우트 (App Router)
├── components/   공용 컴포넌트
└── lib/          API 클라이언트·유틸
```

## 주요 라우트 (`app/`)

| 라우트 | 화면 | 대응 도메인(백엔드) |
| --- | --- | --- |
| `signup` · `login` | 회원가입·로그인 | auth |
| `find-password` · `password-reset` | 비밀번호 찾기·재설정 | auth |
| `my-profile` | 내 정보 | member |
| `products` | 상품 목록·상세 | product · category |
| `my-products` | 내 상품 관리 | product |
| `favorites` | 관심 목록 | favorite |
| `chat` | 1:1 채팅 | chat |
| `report` · `my-reports` | 신고·내 신고 내역 | report |
| `admin` | 관리자 화면 | admin |

> 라우트↔도메인 매핑은 참고용이다. 백엔드 API 정본은 Swagger, 리소스 지도는 [api/README.md](../api/README.md).

## 후속

- 컴포넌트 계층·상태관리·API 클라이언트(`lib/`) 규약은 필요 시 확장한다(현재는 라우트 수준 개요).
