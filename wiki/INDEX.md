# 📇 wiki 색인 (INDEX)

> **자동 생성 파일입니다.** 손으로 고치지 마세요 — 문서를 추가/수정한 뒤 색인을 다시 만드세요.
> 이 파일의 목적은 LLM이 `wiki/` 전체를 grep하지 않고 **한 번 읽어 후보를 좁히는 것**입니다.
> 규칙은 [CLAUDE.md](CLAUDE.md), 폴더 설명은 [README.md](README.md) 참고.

- 총 문서: **150개** (feature 138 · bug 12)
- 대상 PR: **#2 ~ #256**
- 기간: 2026-06-25 ~ 2026-07-28

---

## 전체 문서 (PR 번호 내림차순)

| id | 상태 | 날짜 | 작성자 | 제목 | 태그 | 문서 |
|---|---|---|---|---|---|---|
| FEAT-256 | done | 2026-07-28 | han95white | refactor: 지역 문자열 호환 코드 제거 | admin chat favorite global member product | [FEAT-256-remove-region-string-compat.md](features/FEAT-256-remove-region-string-compat.md) |
| FEAT-254 | done | 2026-07-27 | NextWave-Dev-Space | feat(frontend): 상품/지역 API를 regionCode 기반 계층 구조로 전환 | admin my-profile products frontend | [FEAT-254-region-code-frontend-transition.md](features/FEAT-254-region-code-frontend-transition.md) |
| FEAT-252 | done | 2026-07-27 | NextWave-Dev-Space | feat(frontend): 탈퇴 상대 채팅 UX + 자기 상품 찜 버튼 가드 | chat products frontend | [FEAT-252-chat-product-frontend-updates.md](features/FEAT-252-chat-product-frontend-updates.md) |
| FEAT-250 | done | 2026-07-27 | han95white | feat: 회원 동네 regionCode 전환 및 상품 지역 응답 DTO 확장 | admin chat favorite member trade backend | [FEAT-250-member-region-code.md](features/FEAT-250-member-region-code.md) |
| FEAT-248 | done | 2026-07-27 | han95white | feat: 계층형 지역 마스터 및 상품 regionCode 연동 | global product region notification backend | [FEAT-248-region-hierarchy.md](features/FEAT-248-region-hierarchy.md) |
| BUG-246 | done | 2026-07-27 | Crispy-down | fix(notification): 채팅 알림 문구에서 상대 닉네임 제거 | 알림 채팅 탈퇴 드리프트 backend | [BUG-246-chat-notification-nickname.md](bugs/BUG-246-chat-notification-nickname.md) |
| FEAT-244 | done | 2026-07-27 | Crispy-down | feat(chat): 채팅 상대 탈퇴 여부(withdrawn) 응답 노출 | chat backend | [FEAT-244-chat-partner-withdrawn-flag.md](features/FEAT-244-chat-partner-withdrawn-flag.md) |
| FEAT-242 | done | 2026-07-27 | Crispy-down | feat(favorite): 자기 상품 관심(찜) 등록 차단 | favorite global backend | [FEAT-242-favorite-block-own-product.md](features/FEAT-242-favorite-block-own-product.md) |
| FEAT-240 | done | 2026-07-27 | NextWave-Dev-Space | feat(frontend): 신규 백엔드 기능 프론트엔드 화면 일괄 구현 | admin escrow globals layout products frontend | [FEAT-240-new-features-frontend.md](features/FEAT-240-new-features-frontend.md) |
| FEAT-238 | done | 2026-07-27 | han95white | test(category): CategorySeederTest 시드 보장 견고화 | global backend test | [FEAT-238-category-seeder-test-isolation.md](features/FEAT-238-category-seeder-test-isolation.md) |
| FEAT-233 | done | 2026-07-27 | Crispy-down | feat(notification): 찜한 사용자에게 상품 가격 변경 알림 발송 | favorite notification backend | [FEAT-233-notification-favorite-price-alert.md](features/FEAT-233-notification-favorite-price-alert.md) |
| FEAT-231 | in-progress | 2026-07-27 | jomin4 | feat(mobile): 마켓온 Android 앱 Phase 1 — 로그인·홈목록·상세·채팅 | 모바일 Android Compose 인증 채팅 상품 | [FEAT-231-mobile-phase1.md](features/FEAT-231-mobile-phase1.md) |
| FEAT-230 | done | 2026-07-27 | jomin4 | feat: 안심결제(에스크로) 거래 도입 v0 | 에스크로 안심결제 거래 상태머신 BE | [FEAT-230-escrow.md](features/FEAT-230-escrow.md) |
| FEAT-229 | done | 2026-07-26 | jomin4 | docs(wiki): FEAT-228 거래 법률 도우미 문서화 | docs | [FEAT-229-ai-legal-helper-wiki.md](features/FEAT-229-ai-legal-helper-wiki.md) |
| FEAT-228 | done | 2026-07-26 | jomin4 | feat(agent): 거래 법률 도우미 — 민사 RAG 챗봇 (인제스트 파이프라인 + 런타임 그래프 + /agent/legal/ask) | AI agent RAG LangGraph 거래법률 민사 | [FEAT-228-ai-legal-helper.md](features/FEAT-228-ai-legal-helper.md) |
| FEAT-227 | done | 2026-07-24 | NextWave-Dev-Space | feat(trade): 거래내역 기능 구현 | trade my-profile backend frontend | [FEAT-227-trade-history.md](features/FEAT-227-trade-history.md) |
| FEAT-222 | done | 2026-07-23 | NextWave-Dev-Space | feat(product): 저신뢰 판매자 상품 노출 우선순위 하락 적용 | admin manner product backend | [FEAT-222-product-exposure-priority.md](features/FEAT-222-product-exposure-priority.md) |
| FEAT-220 | done | 2026-07-23 | NextWave-Dev-Space | feat(frontend): 신고 상세 조회 화면 구현 | 신고 프론트엔드 모달 my-reports | [FEAT-220-report-detail-modal.md](features/FEAT-220-report-detail-modal.md) |
| FEAT-218 | done | 2026-07-23 | Crispy-down | [feat] k6 API 성능테스트 - 2차 프로젝트 기준 | infra | [FEAT-218-k6-test-ver-07-21.md](features/FEAT-218-k6-test-ver-07-21.md) |
| FEAT-217 | done | 2026-07-23 | NextWave-Dev-Space | feat(report): 신뢰도 연동·신고 상세 조회·API 보안 강화 구현 | admin global manner product report backend | [FEAT-217-report-trust-security.md](features/FEAT-217-report-trust-security.md) |
| FEAT-215 | done | 2026-07-23 | NextWave-Dev-Space | feat(frontend): 내상품/관심상품을 나의 마켓온 허브로 통합 | globals frontend | [FEAT-215-marketon-hub-frontend.md](features/FEAT-215-marketon-hub-frontend.md) |
| FEAT-213 | done | 2026-07-23 | NextWave-Dev-Space | feat(manner): 매너온도 시스템 프론트엔드 구현 | admin chat global manner product my-reports | [FEAT-213-manner-score-frontend.md](features/FEAT-213-manner-score-frontend.md) |
| FEAT-211 | done | 2026-07-23 | NextWave-Dev-Space | feat(manner): 매너온도 시스템 구현 | admin global manner product backend | [FEAT-211-manner-score-system.md](features/FEAT-211-manner-score-system.md) |
| FEAT-209 | done | 2026-07-10 | jomin4 | feat(admin): 저장소 고아파일 조회·삭제 API | 관리자 저장소 고아파일 이미지 보안 | [FEAT-209-admin-storage-orphan.md](features/FEAT-209-admin-storage-orphan.md) |
| FEAT-208 | done | 2026-07-09 | jomin4 | ai 화면 수정 | 관리자AI 프론트엔드 관리자 ui | [FEAT-208-admin-ai-ui.md](features/FEAT-208-admin-ai-ui.md) |
| BUG-207 | done | 2026-07-09 | jomin4 | chore(dev): 로컬 CPU 환경에서 관리자 AI 응답 반환되도록 튜닝 | 관리자AI ollama 타임아웃 성능 dev | [BUG-207-admin-ai-dev-latency.md](bugs/BUG-207-admin-ai-dev-latency.md) |
| BUG-206 | done | 2026-07-09 | jomin4 | chore(dev): 관리자 AI dev 프로파일을 로컬 Ollama로 오버라이드 | 관리자AI ollama dev 설정 | [BUG-206-dev-ollama-localhost.md](bugs/BUG-206-dev-ollama-localhost.md) |
| FEAT-205 | done | 2026-07-09 | horangnabi97 | feat(frontend): 상품 목록 실시간성 개선 + 프로필/신고/OG 공유 UX 정비 | 프론트엔드 게시글 프로필 신고 폴링 | [FEAT-205-frontend-ux-improvements.md](features/FEAT-205-frontend-ux-improvements.md) |
| FEAT-204 | done | 2026-07-09 | horangnabi97 | feat(auth): Redis 기반 인증 TTL 데이터 전환 | 인증 redis 리프레시토큰 브루트포스 보안 | [FEAT-204-redis-auth-ttl.md](features/FEAT-204-redis-auth-ttl.md) |
| FEAT-203 | done | 2026-07-08 | jomin4 | refactor(infra): 인프라 경계 재정리 — 2축(프로파일×배포지형) + Flyway 도입 (P1~P4) | product report backend infra docs refactor | [FEAT-203-infra-boundary.md](features/FEAT-203-infra-boundary.md) |
| BUG-202 | done | 2026-07-08 | han95white | fix(product): 상품 수정 시 대표이미지 변경 미반영 수정 | product backend | [BUG-202-product-thumbnail-bug-repro.md](bugs/BUG-202-product-thumbnail-bug-repro.md) |
| FEAT-200 | done | 2026-07-08 | NextWave-Dev-Space | feat: 최종 보완 — 관리자/채팅/내 상품/관심상품 UI 개선 및 관심상품 카테고리 필터 추가 | favorite admin chat favorites my-products backend | [FEAT-200-final-supplement.md](features/FEAT-200-final-supplement.md) |
| FEAT-198 | done | 2026-07-08 | horangnabi97 | feat(storage): 신고 증빙/상품 이미지 저장소를 설정 기반(local/S3)으로 전환 | global product report backend infra docs | [FEAT-198-s3-image-storage.md](features/FEAT-198-s3-image-storage.md) |
| FEAT-197 | done | 2026-07-08 | Crispy-down | feat(chat): 탈퇴한 상대에게 메시지 전송 차단 (읽기는 유지) | chat global member backend | [FEAT-197-chat-block-withdrawn-partner.md](features/FEAT-197-chat-block-withdrawn-partner.md) |
| FEAT-196 | done | 2026-07-08 | jomin4 | 문서정정 | docs | [FEAT-196-ci-cd-deploy-drift-sync.md](features/FEAT-196-ci-cd-deploy-drift-sync.md) |
| BUG-195 | done | 2026-07-08 | horangnabi97 | fix(auth): 회원가입 비밀번호 정책을 변경/재설정 정책과 통일 | auth member signup backend frontend | [BUG-195-auth-signup-password-policy.md](bugs/BUG-195-auth-signup-password-policy.md) |
| FEAT-193 | done | 2026-07-07 | NextWave-Dev-Space | feat: 최종 점검 — 버그 수정, 신고내역 통계 UI, 댓글 닉네임/알림 문구 연동 | notification admin chat favorites find-password globals | [FEAT-193-final-check.md](features/FEAT-193-final-check.md) |
| FEAT-191 | done | 2026-07-07 | Crispy-down | feat(comment): 댓글 목록에 작성자 닉네임 노출 및 탈퇴 사용자 마스킹 | comment backend | [FEAT-191-comment-author-nickname.md](features/FEAT-191-comment-author-nickname.md) |
| FEAT-189 | done | 2026-07-07 | NextWave-Dev-Space | style(frontend): 고객 화면 API 힌트 제거 및 로그인/회원가입 인트로 폰트 확대 | chat favorites find-password login my-products my-profile | [FEAT-189-customer-ui-polish.md](features/FEAT-189-customer-ui-polish.md) |
| FEAT-187 | done | 2026-07-07 | jomin4 | 팔요없는 문서제거 |  | [FEAT-187-context.md](features/FEAT-187-context.md) |
| FEAT-186 | done | 2026-07-07 | jomin4 | ãconfig 수정 | global backend | [FEAT-186-security-product-image-public.md](features/FEAT-186-security-product-image-public.md) |
| FEAT-185 | done | 2026-07-07 | jomin4 | feat: 시더 공통화 - DataSeeder/SeedOrchestrator로 init 통합 | category global region comment favorite notification | [FEAT-185-baseinidata.md](features/FEAT-185-baseinidata.md) |
| FEAT-184 | done | 2026-07-07 | jomin4 | docs: 인프라·CI/CD 문서화 + Testcontainers 통합 테스트 제거 | product report support backend docs | [FEAT-184-gap-and-test-cleanup.md](features/FEAT-184-gap-and-test-cleanup.md) |
| BUG-183 | done | 2026-07-07 | Crispy-down | fix(member): 탈퇴한 사용자 닉네임을 채팅·알림에서 마스킹 | chat member backend | [BUG-183-withdrawn-member-nickname-mask.md](bugs/BUG-183-withdrawn-member-nickname-mask.md) |
| FEAT-182 | done | 2026-07-07 | NextWave-Dev-Space | feat(product): 상품 등록/수정 이미지를 파일 업로드 방식으로 전환 | frontend | [FEAT-182-product-image-upload-frontend.md](features/FEAT-182-product-image-upload-frontend.md) |
| FEAT-180 | done | 2026-07-07 | horangnabi97 | feat(frontend): 회원가입 약관 동의 UI 및 정책 문서 추가 | signup frontend | [FEAT-180-auth-signup-agreement-frontend.md](features/FEAT-180-auth-signup-agreement-frontend.md) |
| FEAT-179 | done | 2026-07-07 | horangnabi97 | feat(auth): 회원가입 약관 동의 저장 및 탈퇴 회원 표시명 추가 | auth global member backend | [FEAT-179-auth-signup-agreement-backend.md](features/FEAT-179-auth-signup-agreement-backend.md) |
| FEAT-177 | done | 2026-07-07 | jomin4 | docs: docs 폴더 Docs-as-Code 재편 + AGENTS.md 신설 | infra docs | [FEAT-177-restructure.md](features/FEAT-177-restructure.md) |
| FEAT-176 | done | 2026-07-07 | han95white | feat(product): 상품 이미지 업로드 API 추가 | product backend | [FEAT-176-product-image-upload.md](features/FEAT-176-product-image-upload.md) |
| FEAT-174 | done | 2026-07-07 | NextWave-Dev-Space | feat(header): 알림 목록 패널을 헤더 벨 아이콘에 연동 | globals frontend | [FEAT-174-notification-panel.md](features/FEAT-174-notification-panel.md) |
| FEAT-172 | done | 2026-07-07 | jomin4 | docs: 사용하지 않는 문서 폴더 정리 (ai·postman·README만 유지) | docs | [FEAT-172-cleanup-unused-folders.md](features/FEAT-172-cleanup-unused-folders.md) |
| FEAT-171 | done | 2026-07-06 | NextWave-Dev-Space | feat(header): 알림 배지를 실제 API와 연동 | frontend | [FEAT-171-header-notification-badge.md](features/FEAT-171-header-notification-badge.md) |
| FEAT-169 | done | 2026-07-06 | NextWave-Dev-Space | feat(auth): 비밀번호 찾기/재설정 화면 연동 | find-password login password-reset frontend | [FEAT-169-password-reset-frontend.md](features/FEAT-169-password-reset-frontend.md) |
| FEAT-167 | done | 2026-07-06 | NextWave-Dev-Space | feat(auth): 로그아웃 이동 처리, 회원가입 이메일 인증·비밀번호 확인, 비밀번호 변경 기능 추가 | globals my-profile signup frontend | [FEAT-167-auth-frontend-improvements.md](features/FEAT-167-auth-frontend-improvements.md) |
| FEAT-165 | done | 2026-07-06 | Crispy-down | feat(notification): 구매 상품 가격 수정 알림 (PR-N3) | chat global notification product backend | [FEAT-165-notification-price-change.md](features/FEAT-165-notification-price-change.md) |
| BUG-164 | done | 2026-07-06 | NextWave-Dev-Space | fix(theme): 다크모드 전환 시 body 배경/글자색이 갱신되지 않던 버그 수정 | globals frontend | [BUG-164-theme-body-transition-bug.md](bugs/BUG-164-theme-body-transition-bug.md) |
| FEAT-162 | done | 2026-07-06 | han95white | feat(product): 공개 상품 조회 노출 정책 적용 | product admin backend | [FEAT-162-product-public-visibility-policy.md](features/FEAT-162-product-public-visibility-policy.md) |
| FEAT-160 | done | 2026-07-06 | NextWave-Dev-Space | feat(member): 내정보 동네 설정 UI를 검색 모달 방식으로 통일 및 테스트 | my-profile frontend | [FEAT-160-my-profile-region-modal.md](features/FEAT-160-my-profile-region-modal.md) |
| FEAT-158 | done | 2026-07-06 | Crispy-down | feat(notification): 알림 피드에 채팅 파생 병합 + 안읽음 카운트 (PR-N2) | notification backend | [FEAT-158-notification-chat-feed.md](features/FEAT-158-notification-chat-feed.md) |
| BUG-157 | done | 2026-07-06 | NextWave-Dev-Space | fix(favorite): 관심 상품 목록에 실제 썸네일 이미지 표시 및 테스트 | favorites frontend | [BUG-157-favorite-thumbnail-frontend.md](bugs/BUG-157-favorite-thumbnail-frontend.md) |
| FEAT-155 | done | 2026-07-06 | NextWave-Dev-Space | feat(chat): 채팅 안읽음 배지 프론트 연동 및 테스트 | chat frontend | [FEAT-155-chat-unread-badge-frontend.md](features/FEAT-155-chat-unread-badge-frontend.md) |
| FEAT-153 | done | 2026-07-06 | horangnabi97 | feat: 비밀번호 재설정 기능 추가 | auth global backend docs | [FEAT-153-auth-password-reset.md](features/FEAT-153-auth-password-reset.md) |
| BUG-152 | done | 2026-07-06 | jomin4 | fix(ci): deploy job 들여쓰기 수정 (2칸 잡 레벨) |  | [BUG-152-cd-deploy-indent.md](bugs/BUG-152-cd-deploy-indent.md) |
| FEAT-151 | done | 2026-07-06 | jomin4 | feat(ci): cd-app에 EC2 자동배포(deploy) job 추가 |  | [FEAT-151-cd-deploy-job.md](features/FEAT-151-cd-deploy-job.md) |
| FEAT-150 | done | 2026-07-06 | jomin4 | feat(infra): 클라우드 모니터링(prometheus·loki·grafana) + app promtail/MAIL/증빙볼륨 | 모니터링 prometheus loki grafana infra | [FEAT-150-cloud-monitoring-stack.md](features/FEAT-150-cloud-monitoring-stack.md) |
| FEAT-149 | done | 2026-07-06 | Crispy-down | feat(notification): 댓글 알림 발행·핸들러·조회 API (PR-N1b) | comment notification backend | [FEAT-149-notification-comment.md](features/FEAT-149-notification-comment.md) |
| BUG-148 | done | 2026-07-06 | jomin4 | fix(ci): cd-app buildx 드라이버 추가 (gha 캐시 export 오류 해결) |  | [BUG-148-cd-buildx.md](bugs/BUG-148-cd-buildx.md) |
| FEAT-147 | done | 2026-07-06 | horangnabi97 | feat: 로그인 사용자 비밀번호 변경 기능 추가 | global member backend docs | [FEAT-147-member-password-change.md](features/FEAT-147-member-password-change.md) |
| FEAT-146 | done | 2026-07-06 | horangnabi97 | feat: 회원가입 이메일 인증 기능 추가 | auth global member backend docs | [FEAT-146-auth-signup-email-verified-check.md](features/FEAT-146-auth-signup-email-verified-check.md) |
| BUG-145 | done | 2026-07-06 | NextWave-Dev-Space | fix(frontend): CI 차단 lint 에러(react-hooks/set-state-in-effect) 제거 | admin products frontend | [BUG-145-frontend-lint-cleanup.md](bugs/BUG-145-frontend-lint-cleanup.md) |
| FEAT-143 | done | 2026-07-06 | han95white | feat(product): 상품 목록 커서 페이지네이션 구현 | product products backend frontend | [FEAT-143-product-cursor-pagination.md](features/FEAT-143-product-cursor-pagination.md) |
| FEAT-141 | done | 2026-07-06 | Crispy-down | feat(notification): 알림 도메인 골격 및 댓글 생성 이벤트 (PR-N1a) | global notification backend | [FEAT-141-notification-skeleton.md](features/FEAT-141-notification-skeleton.md) |
| FEAT-136 | done | 2026-07-06 | jomin4 | 인프라세팅 | backend infra docs | [FEAT-136-cloud-deploy-infra.md](features/FEAT-136-cloud-deploy-infra.md) |
| FEAT-135 | done | 2026-07-06 | han95white | feat(region): 지역 마스터 시드 전국 시군구 확장 | region backend | [FEAT-135-region-seed-expansion.md](features/FEAT-135-region-seed-expansion.md) |
| FEAT-133 | done | 2026-07-06 | Crispy-down | feat(favorite): 관심 목록 상품 요약에 썸네일 URL 추가 | favorite backend | [FEAT-133-favorite-thumbnail.md](features/FEAT-133-favorite-thumbnail.md) |
| FEAT-131 | done | 2026-07-05 | NextWave-Dev-Space | [Report] 신고 기능 고도화 및 사용자·관리자 UI 개선 | global report admin globals my-reports page | [FEAT-131-report-enhancements-and-ui.md](features/FEAT-131-report-enhancements-and-ui.md) |
| FEAT-129 | done | 2026-07-05 | Crispy-down | feat(chat): 채팅방 안읽음 메시지 수 및 읽음 처리 (PR1c) | chat backend | [FEAT-129-chat-unread.md](features/FEAT-129-chat-unread.md) |
| FEAT-128 | done | 2026-07-04 | Crispy-down | [Feature] 채팅방 목록을 마지막 메시지 시각순으로 정렬 | chat backend | [FEAT-128-chat-room-sort.md](features/FEAT-128-chat-room-sort.md) |
| FEAT-125 | done | 2026-07-03 | NextWave-Dev-Space | [Frontend] 상품 목록 페이지 디자인 리뉴얼 및 내 동네 설정 버튼 추가 | globals products frontend | [FEAT-125-products-page-design.md](features/FEAT-125-products-page-design.md) |
| FEAT-123 | done | 2026-07-03 | NextWave-Dev-Space | [Frontend] 채팅·내 동네 설정·상품 지역 선택 UI 백엔드 연동 | chat my-profile products frontend | [FEAT-123-chat-location-integration.md](features/FEAT-123-chat-location-integration.md) |
| FEAT-121 | done | 2026-07-03 | horangnabi97 | feat(auth): Refresh Token HttpOnly Cookie 전환 및 자동 로그인 구현 | auth admin favorites layout login my-products | [FEAT-121-auth-autologin.md](features/FEAT-121-auth-autologin.md) |
| FEAT-120 | done | 2026-07-03 | Crispy-down | feat(chat): 1:1 채팅 서비스·컨트롤러·REST API (PR1b, 폴링) | chat backend | [FEAT-120-chat-polling.md](features/FEAT-120-chat-polling.md) |
| FEAT-118 | done | 2026-07-03 | jomin4 | chore: local-deploy 환경(compose/nginx/observability/edge) + 프로파일 정리 + 문서 | backend frontend docs chore | [FEAT-118-local-deploy.md](features/FEAT-118-local-deploy.md) |
| FEAT-117 | done | 2026-07-03 | han95white | feat: 회원 내 동네 설정 및 조회 기능 추가 | member backend | [FEAT-117-member-locations.md](features/FEAT-117-member-locations.md) |
| FEAT-116 | done | 2026-07-03 | horangnabi97 | feat(auth): 로그아웃 API 추가 | auth backend docs | [FEAT-116-auth-logout.md](features/FEAT-116-auth-logout.md) |
| FEAT-115 | done | 2026-07-03 | NextWave-Dev-Space | [Frontend] 관리자 페이지를 실제 백엔드 admin API와 연동 | admin globals frontend | [FEAT-115-admin-pages.md](features/FEAT-115-admin-pages.md) |
| FEAT-114 | done | 2026-07-03 | han95white | feat: 지역 마스터 및 상품 지역 필터 추가 | global product region backend | [FEAT-114-region-filter.md](features/FEAT-114-region-filter.md) |
| FEAT-112 | done | 2026-07-03 | horangnabi97 | feat(auth): Refresh Token 기반 토큰 재발급 기능 추가 | auth global backend docs | [FEAT-112-auth-refreshtoken.md](features/FEAT-112-auth-refreshtoken.md) |
| FEAT-109 | done | 2026-07-03 | NextWave-Dev-Space | [Frontend] 상품 이미지 등록/표시 기능 프론트엔드 연동 | my-products products frontend | [FEAT-109-product-image-integration.md](features/FEAT-109-product-image-integration.md) |
| FEAT-107 | done | 2026-07-03 | Crispy-down | feat(chat): 1:1 채팅 코어 골격 — 엔티티·Repository·ErrorCode (PR1a) | chat global backend | [FEAT-107-chat-core.md](features/FEAT-107-chat-core.md) |
| FEAT-105 | done | 2026-07-03 | jomin4 | 필요없는 문서 삭제 | docs | [FEAT-105-docs-cleanup.md](features/FEAT-105-docs-cleanup.md) |
| FEAT-104 | done | 2026-07-03 | jomin4 | 인프라 환경구성 | global backend | [FEAT-104-operator.md](features/FEAT-104-operator.md) |
| FEAT-103 | done | 2026-07-03 | Crispy-down | refactor(favorite): 관심목록 조회 N+1 제거 — fetch join + 상한 200 | favorite backend refactor | [FEAT-103-favorite-service.md](features/FEAT-103-favorite-service.md) |
| FEAT-101 | done | 2026-07-03 | NextWave-Dev-Space | [Frontend] 프론트엔드 페이지를 실제 백엔드 API와 연동 | favicon favorites globals layout login my-products | [FEAT-101-frontend-api-integration.md](features/FEAT-101-frontend-api-integration.md) |
| FEAT-100 | done | 2026-07-03 | han95white | feat: 상품 이미지 등록 및 대표이미지 설정 구현 | product backend | [FEAT-100-product-image.md](features/FEAT-100-product-image.md) |
| FEAT-097 | done | 2026-07-02 | jomin4 | docs: MVP 문서 정리 (구 제안서 폐기 · 배포/테스트/유스케이스 추가) | docs | [FEAT-097-cleanup-mvp-docs.md](features/FEAT-097-cleanup-mvp-docs.md) |
| FEAT-096 | done | 2026-07-02 | han95white | [Product] 관심 수 favoriteCount 이벤트 리스너 및 엣지 케이스 테스트 추가 | product backend | [FEAT-096-product-favorite-count.md](features/FEAT-096-product-favorite-count.md) |
| FEAT-095 | done | 2026-07-02 | jomin4 | ai기능구현 ë | admin global backend | [FEAT-095-devops.md](features/FEAT-095-devops.md) |
| FEAT-094 | done | 2026-07-02 | Crispy-down | test(favorite·comment): 테스트 유스케이스 중심 재구성 + 계층 책임 정리 | comment favorite backend test | [FEAT-094-favorite-commenttest.md](features/FEAT-094-favorite-commenttest.md) |
| FEAT-093 | done | 2026-07-02 | han95white |  [Product] 관심 수 favoriteCount 이벤트 리스너 및 원자 업데이트 추가 | product backend | [FEAT-093-product-favorite-count.md](features/FEAT-093-product-favorite-count.md) |
| FEAT-090 | done | 2026-07-02 | Crispy-down | feat(favorite): 찜 등록·취소 시 카운트 이벤트 발행 (PR B) | 찜 이벤트 트랜잭션 카운트 backend | [FEAT-090-favorite-count-event.md](features/FEAT-090-favorite-count-event.md) |
| FEAT-089 | done | 2026-07-01 | Crispy-down | [Feature] 찜 카운트용 도메인 이벤트 2종 추가 + Report 리팩터 빌드 복구 (#86) | admin global backend | [FEAT-089-favoritecount-addevent.md](features/FEAT-089-favoritecount-addevent.md) |
| BUG-088 | done | 2026-07-01 | jomin4 | fix: 신고 리팩터로 깨진 develop 빌드 복구 (긴급) | admin global backend | [BUG-088-base-init-data-report-api.md](bugs/BUG-088-base-init-data-report-api.md) |
| FEAT-087 | done | 2026-07-01 | jomin4 | feat: 개발/검증용 BaseInitData 시더 추가 | global backend | [FEAT-087-base-init-data.md](features/FEAT-087-base-init-data.md) |
| FEAT-085 | done | 2026-07-01 | NextWave-Dev-Space | [Refactor] 신고 기능 리팩토링 — JPA 엔티티 매핑, DB 인덱스 추가, 서비스/통합 테스트 코드 정리 및 작성 | report backend | [FEAT-085-report-refactor.md](features/FEAT-085-report-refactor.md) |
| FEAT-084 | done | 2026-07-01 | jomin4 | test(admin): 서비스 단위 테스트 재설계 — 선별적 mock 테스트 | admin backend test | [FEAT-084-admin-redesign.md](features/FEAT-084-admin-redesign.md) |
| FEAT-082 | done | 2026-07-01 | Crispy-down | [Refactor] Favorite·Comment 연관관계 도입 + 검증 중앙화 + 관심목록 상품요약 (#80) | comment favorite backend docs | [FEAT-082-issue-80.md](features/FEAT-082-issue-80.md) |
| FEAT-081 | done | 2026-07-01 | han95white | test(product): organize product service tests | product backend test | [FEAT-081-product-service-test-refactor.md](features/FEAT-081-product-service-test-refactor.md) |
| FEAT-077 | done | 2026-07-01 | horangnabi97 | test: auth/member 서비스 테스트 mock 정리 | auth member backend test | [FEAT-077-auth-member-test.md](features/FEAT-077-auth-member-test.md) |
| FEAT-076 | done | 2026-07-01 | han95white | refactor(product): price 타입 BigDecimal 전환 | admin product category comment favorite report | [FEAT-076-product-code-cleanup.md](features/FEAT-076-product-code-cleanup.md) |
| FEAT-075 | done | 2026-06-30 | jomin4 | 테스트 서버 구축 | product admin support backend | [FEAT-075-initialze.md](features/FEAT-075-initialze.md) |
| FEAT-074 | done | 2026-06-29 | han95white | feat(product): 내 상품 조회 API 주소 이관 | product backend docs | [FEAT-074-product-my-products-url.md](features/FEAT-074-product-my-products-url.md) |
| FEAT-072 | done | 2026-06-29 | jomin4 | feat: ê관리자 api 구현 및 문서 수정 | admin global backend docs | [FEAT-072-admin-status.md](features/FEAT-072-admin-status.md) |
| FEAT-070 | done | 2026-06-29 | NextWave-Dev-Space | [Report] 관리자용 신고 상태 변경 메서드 추가 | report backend | [FEAT-070-report-change-status.md](features/FEAT-070-report-change-status.md) |
| FEAT-068 | done | 2026-06-29 | han95white | feat: 상품 검색 API 구현 | product backend | [FEAT-068-product-search.md](features/FEAT-068-product-search.md) |
| FEAT-067 | done | 2026-06-29 | horangnabi97 | test(member): Member 관련 테스트 보강 | member backend test | [FEAT-067-member-test.md](features/FEAT-067-member-test.md) |
| FEAT-066 | done | 2026-06-29 | jomin4 |  관리자 api 기능구현 | admin backend | [FEAT-066-admin-product.md](features/FEAT-066-admin-product.md) |
| FEAT-064 | done | 2026-06-29 | horangnabi97 | feat(member): Admin용 Member 엔티티 메서드 2개 추가 | member backend | [FEAT-064-member.md](features/FEAT-064-member.md) |
| FEAT-063 | done | 2026-06-29 | han95white | feat: 내 상품 목록 조회 API 구현 | product backend docs | [FEAT-063-product-my-list.md](features/FEAT-063-product-my-list.md) |
| FEAT-062 | done | 2026-06-29 | Crispy-down | feat: 내 관심 상품 목록 조회 API 구현 | favorite backend docs | [FEAT-062-favorite-list.md](features/FEAT-062-favorite-list.md) |
| FEAT-060 | done | 2026-06-28 | jomin4 | docs: admin 에이전트 작업방식·git/PR 절차 명시 | docs | [FEAT-060-admin-agent-workflow.md](features/FEAT-060-admin-agent-workflow.md) |
| FEAT-059 | done | 2026-06-28 | jomin4 | feat: 관리자 회원 조회 API 구현 | admin backend docs | [FEAT-059-admin2.md](features/FEAT-059-admin2.md) |
| FEAT-056 | done | 2026-06-27 | han95white | feat: 상품 거래 상태 변경 API 구현 | product backend docs | [FEAT-056-product-status-update.md](features/FEAT-056-product-status-update.md) |
| FEAT-055 | done | 2026-06-27 | jomin4 | Feat/global1 | docs | [FEAT-055-global1.md](features/FEAT-055-global1.md) |
| FEAT-053 | done | 2026-06-27 | Crispy-down | feat: 댓글 목록 조회 API 구현 | comment backend docs | [FEAT-053-comment-list.md](features/FEAT-053-comment-list.md) |
| FEAT-051 | done | 2026-06-27 | horangnabi97 | feat(member): 내 정보 조회/수정/탈퇴 API 상태 체크 및 Postman 문서 추가 | member backend docs | [FEAT-051-member-me.md](features/FEAT-051-member-me.md) |
| FEAT-050 | done | 2026-06-26 | Crispy-down | [Comment] 댓글 삭제 API 구현 | comment backend docs | [FEAT-050-comment-delete.md](features/FEAT-050-comment-delete.md) |
| FEAT-048 | done | 2026-06-26 | jomin4 | global security 변경 | global backend | [FEAT-048-admin.md](features/FEAT-048-admin.md) |
| FEAT-047 | done | 2026-06-26 | han95white | feat: 카테고리별 상품 목록 조회 API 구현 | category product backend docs | [FEAT-047-category-products.md](features/FEAT-047-category-products.md) |
| FEAT-046 | done | 2026-06-26 | Crispy-down | [Comment] 댓글 수정 API 구현 | comment backend docs | [FEAT-046-comment-update.md](features/FEAT-046-comment-update.md) |
| FEAT-042 | done | 2026-06-26 | han95white | feat: 접근 가능한 상품 검증 추가 | product backend | [FEAT-042-product-validate-accessible.md](features/FEAT-042-product-validate-accessible.md) |
| FEAT-040 | done | 2026-06-26 | NextWave-Dev-Space | [Report] 상품 신고 API Repository·Service·Controller 구현 및 단위 테스트 작성 | report backend docs | [FEAT-040-report-product-create.md](features/FEAT-040-report-product-create.md) |
| FEAT-039 | done | 2026-06-26 | Crispy-down | [Favorite] 관심 상품 취소 API 구현 | favorite backend docs | [FEAT-039-favorite-delete.md](features/FEAT-039-favorite-delete.md) |
| FEAT-036 | done | 2026-06-26 | horangnabi97 | feat(member): 내 정보 조회/수정/탈퇴 API 구현 | member backend | [FEAT-036-member-me.md](features/FEAT-036-member-me.md) |
| FEAT-035 | done | 2026-06-26 | han95white | feat: 상품 삭제 API 구현 | product backend docs | [FEAT-035-product-delete.md](features/FEAT-035-product-delete.md) |
| FEAT-032 | done | 2026-06-26 | han95white | feat: 상품 수정 API 구현 | global product backend docs | [FEAT-032-product-update.md](features/FEAT-032-product-update.md) |
| FEAT-031 | done | 2026-06-26 | Crispy-down | [Comment] 댓글 작성 API 구현 | comment backend docs | [FEAT-031-comment-create.md](features/FEAT-031-comment-create.md) |
| FEAT-029 | done | 2026-06-26 | han95white | feat: 상품 상세 조회 API 구현 | product backend docs | [FEAT-029-product-detail.md](features/FEAT-029-product-detail.md) |
| FEAT-028 | done | 2026-06-26 | jomin4 | 시스템 구성도 v1 | docs | [FEAT-028-admin.md](features/FEAT-028-admin.md) |
| FEAT-023 | done | 2026-06-26 | han95white | feat: 상품 목록 조회 API 구현 | product backend docs | [FEAT-023-product-list.md](features/FEAT-023-product-list.md) |
| FEAT-021 | done | 2026-06-26 | Crispy-down | [Favorite] 관심 상품 등록 API 구현 | favorite backend docs | [FEAT-021-favorite-create.md](features/FEAT-021-favorite-create.md) |
| FEAT-020 | done | 2026-06-26 | horangnabi97 | feat(auth): 로그인 API 구현 | auth member backend | [FEAT-020-auth-login-api.md](features/FEAT-020-auth-login-api.md) |
| FEAT-018 | done | 2026-06-26 | han95white | feat: 상품 등록 API 구현 | global product backend docs | [FEAT-018-product-base.md](features/FEAT-018-product-base.md) |
| FEAT-015 | done | 2026-06-25 | jomin4 | 문서수정 |  | [FEAT-015-lead-only-workflow.md](features/FEAT-015-lead-only-workflow.md) |
| FEAT-014 | done | 2026-06-25 | Crispy-down | [Comment] 댓글 엔티티 & DTO 골격 구현 | comment backend | [FEAT-014-comment-base.md](features/FEAT-014-comment-base.md) |
| FEAT-013 | done | 2026-06-25 | han95white | feat: 카테고리 목록 조회 API 구현 | category backend docs | [FEAT-013-category-list.md](features/FEAT-013-category-list.md) |
| FEAT-012 | done | 2026-06-25 | NextWave-Dev-Space | [Report] 상품 신고 API Entity·Enum·DTO 골격 구현 | global report backend | [FEAT-012-report-product-report.md](features/FEAT-012-report-product-report.md) |
| FEAT-011 | done | 2026-06-25 | horangnabi97 | [Auth] 회원가입(Signup) API 구현 | auth member backend docs | [FEAT-011-auth-signup-api.md](features/FEAT-011-auth-signup-api.md) |
| FEAT-007 | done | 2026-06-25 | Crispy-down | feat: [Favorite] 관심 상품 엔티티 & DTO 골격 구현 #5 | favorite backend | [FEAT-007-favorite-base.md](features/FEAT-007-favorite-base.md) |
| FEAT-002 | done | 2026-06-25 | jomin4 | Feature/global monorepo | global backend docs | [FEAT-002-global-monorepo.md](features/FEAT-002-global-monorepo.md) |

---

## 태그 역색인

- **backend** (96) — BUG-088, BUG-183, BUG-195, BUG-202, BUG-246, FEAT-002, FEAT-007, FEAT-011, FEAT-012, FEAT-013, FEAT-014, FEAT-018, FEAT-020, FEAT-021, FEAT-023, FEAT-029, FEAT-031, FEAT-032, FEAT-035, FEAT-036, FEAT-039, FEAT-040, FEAT-042, FEAT-046, FEAT-047, FEAT-048, FEAT-050, FEAT-051, FEAT-053, FEAT-056, FEAT-059, FEAT-062, FEAT-063, FEAT-064, FEAT-066, FEAT-067, FEAT-068, FEAT-070, FEAT-072, FEAT-074, FEAT-075, FEAT-077, FEAT-081, FEAT-082, FEAT-084, FEAT-085, FEAT-087, FEAT-089, FEAT-090, FEAT-093, FEAT-094, FEAT-095, FEAT-096, FEAT-100, FEAT-103, FEAT-104, FEAT-107, FEAT-112, FEAT-114, FEAT-116, FEAT-117, FEAT-118, FEAT-120, FEAT-128, FEAT-129, FEAT-133, FEAT-135, FEAT-136, FEAT-141, FEAT-143, FEAT-146, FEAT-147, FEAT-149, FEAT-153, FEAT-158, FEAT-162, FEAT-165, FEAT-176, FEAT-179, FEAT-184, FEAT-186, FEAT-191, FEAT-197, FEAT-198, FEAT-200, FEAT-203, FEAT-211, FEAT-217, FEAT-222, FEAT-227, FEAT-233, FEAT-238, FEAT-242, FEAT-244, FEAT-248, FEAT-250
- **docs** (43) — FEAT-002, FEAT-011, FEAT-013, FEAT-018, FEAT-021, FEAT-023, FEAT-028, FEAT-029, FEAT-031, FEAT-032, FEAT-035, FEAT-039, FEAT-040, FEAT-046, FEAT-047, FEAT-050, FEAT-051, FEAT-053, FEAT-055, FEAT-056, FEAT-059, FEAT-060, FEAT-062, FEAT-063, FEAT-072, FEAT-074, FEAT-082, FEAT-097, FEAT-105, FEAT-112, FEAT-116, FEAT-118, FEAT-136, FEAT-146, FEAT-147, FEAT-153, FEAT-172, FEAT-177, FEAT-184, FEAT-196, FEAT-198, FEAT-203, FEAT-229
- **global** (32) — BUG-088, FEAT-002, FEAT-012, FEAT-018, FEAT-032, FEAT-048, FEAT-072, FEAT-087, FEAT-089, FEAT-095, FEAT-104, FEAT-107, FEAT-112, FEAT-114, FEAT-131, FEAT-141, FEAT-146, FEAT-147, FEAT-153, FEAT-165, FEAT-179, FEAT-185, FEAT-186, FEAT-197, FEAT-198, FEAT-211, FEAT-213, FEAT-217, FEAT-238, FEAT-242, FEAT-248, FEAT-256
- **product** (32) — BUG-202, FEAT-018, FEAT-023, FEAT-029, FEAT-032, FEAT-035, FEAT-042, FEAT-047, FEAT-056, FEAT-063, FEAT-068, FEAT-074, FEAT-075, FEAT-076, FEAT-081, FEAT-093, FEAT-096, FEAT-100, FEAT-114, FEAT-143, FEAT-162, FEAT-165, FEAT-176, FEAT-184, FEAT-198, FEAT-203, FEAT-211, FEAT-213, FEAT-217, FEAT-222, FEAT-248, FEAT-256
- **admin** (24) — BUG-088, BUG-145, FEAT-059, FEAT-066, FEAT-072, FEAT-075, FEAT-076, FEAT-084, FEAT-089, FEAT-095, FEAT-115, FEAT-121, FEAT-131, FEAT-162, FEAT-193, FEAT-200, FEAT-211, FEAT-213, FEAT-217, FEAT-222, FEAT-240, FEAT-250, FEAT-254, FEAT-256
- **frontend** (23) — BUG-145, BUG-157, BUG-164, BUG-195, FEAT-109, FEAT-115, FEAT-118, FEAT-123, FEAT-125, FEAT-143, FEAT-155, FEAT-160, FEAT-167, FEAT-169, FEAT-171, FEAT-174, FEAT-180, FEAT-182, FEAT-215, FEAT-227, FEAT-240, FEAT-252, FEAT-254
- **chat** (17) — BUG-183, FEAT-107, FEAT-120, FEAT-123, FEAT-128, FEAT-129, FEAT-155, FEAT-165, FEAT-189, FEAT-193, FEAT-197, FEAT-200, FEAT-213, FEAT-244, FEAT-250, FEAT-252, FEAT-256
- **member** (16) — BUG-183, BUG-195, FEAT-011, FEAT-020, FEAT-036, FEAT-051, FEAT-064, FEAT-067, FEAT-077, FEAT-117, FEAT-146, FEAT-147, FEAT-179, FEAT-197, FEAT-250, FEAT-256
- **favorite** (15) — FEAT-007, FEAT-021, FEAT-039, FEAT-062, FEAT-076, FEAT-082, FEAT-094, FEAT-103, FEAT-133, FEAT-185, FEAT-200, FEAT-233, FEAT-242, FEAT-250, FEAT-256
- **comment** (11) — FEAT-014, FEAT-031, FEAT-046, FEAT-050, FEAT-053, FEAT-076, FEAT-082, FEAT-094, FEAT-149, FEAT-185, FEAT-191
- **auth** (10) — BUG-195, FEAT-011, FEAT-020, FEAT-077, FEAT-112, FEAT-116, FEAT-121, FEAT-146, FEAT-153, FEAT-179
- **globals** (10) — BUG-164, FEAT-101, FEAT-115, FEAT-125, FEAT-131, FEAT-167, FEAT-174, FEAT-193, FEAT-215, FEAT-240
- **report** (10) — FEAT-012, FEAT-040, FEAT-070, FEAT-076, FEAT-085, FEAT-131, FEAT-184, FEAT-198, FEAT-203, FEAT-217
- **notification** (8) — FEAT-141, FEAT-149, FEAT-158, FEAT-165, FEAT-185, FEAT-193, FEAT-233, FEAT-248
- **products** (8) — BUG-145, FEAT-109, FEAT-123, FEAT-125, FEAT-143, FEAT-240, FEAT-252, FEAT-254
- **favorites** (6) — BUG-157, FEAT-101, FEAT-121, FEAT-189, FEAT-193, FEAT-200
- **infra** (6) — FEAT-136, FEAT-150, FEAT-177, FEAT-198, FEAT-203, FEAT-218
- **my-profile** (6) — FEAT-123, FEAT-160, FEAT-167, FEAT-189, FEAT-227, FEAT-254
- **test** (6) — FEAT-067, FEAT-077, FEAT-081, FEAT-084, FEAT-094, FEAT-238
- **my-products** (5) — FEAT-101, FEAT-109, FEAT-121, FEAT-189, FEAT-200
- **category** (4) — FEAT-013, FEAT-047, FEAT-076, FEAT-185
- **login** (4) — FEAT-101, FEAT-121, FEAT-169, FEAT-189
- **manner** (4) — FEAT-211, FEAT-213, FEAT-217, FEAT-222
- **region** (4) — FEAT-114, FEAT-135, FEAT-185, FEAT-248
- **관리자AI** (3) — BUG-206, BUG-207, FEAT-208
- **프론트엔드** (3) — FEAT-205, FEAT-208, FEAT-220
- **find-password** (3) — FEAT-169, FEAT-189, FEAT-193
- **layout** (3) — FEAT-101, FEAT-121, FEAT-240
- **my-reports** (3) — FEAT-131, FEAT-213, FEAT-220
- **signup** (3) — BUG-195, FEAT-167, FEAT-180
- **관리자** (2) — FEAT-208, FEAT-209
- **보안** (2) — FEAT-204, FEAT-209
- **신고** (2) — FEAT-205, FEAT-220
- **인증** (2) — FEAT-204, FEAT-231
- **채팅** (2) — BUG-246, FEAT-231
- **dev** (2) — BUG-206, BUG-207
- **ollama** (2) — BUG-206, BUG-207
- **refactor** (2) — FEAT-103, FEAT-203
- **support** (2) — FEAT-075, FEAT-184
- **trade** (2) — FEAT-227, FEAT-250
- **거래** (1) — FEAT-230
- **거래법률** (1) — FEAT-228
- **게시글** (1) — FEAT-205
- **고아파일** (1) — FEAT-209
- **드리프트** (1) — BUG-246
- **리프레시토큰** (1) — FEAT-204
- **모니터링** (1) — FEAT-150
- **모달** (1) — FEAT-220
- **모바일** (1) — FEAT-231
- **민사** (1) — FEAT-228
- **브루트포스** (1) — FEAT-204
- **상태머신** (1) — FEAT-230
- **상품** (1) — FEAT-231
- **설정** (1) — BUG-206
- **성능** (1) — BUG-207
- **안심결제** (1) — FEAT-230
- **알림** (1) — BUG-246
- **에스크로** (1) — FEAT-230
- **이미지** (1) — FEAT-209
- **이벤트** (1) — FEAT-090
- **저장소** (1) — FEAT-209
- **찜** (1) — FEAT-090
- **카운트** (1) — FEAT-090
- **타임아웃** (1) — BUG-207
- **탈퇴** (1) — BUG-246
- **트랜잭션** (1) — FEAT-090
- **폴링** (1) — FEAT-205
- **프로필** (1) — FEAT-205
- **agent** (1) — FEAT-228
- **AI** (1) — FEAT-228
- **Android** (1) — FEAT-231
- **BE** (1) — FEAT-230
- **chore** (1) — FEAT-118
- **Compose** (1) — FEAT-231
- **escrow** (1) — FEAT-240
- **favicon** (1) — FEAT-101
- **grafana** (1) — FEAT-150
- **LangGraph** (1) — FEAT-228
- **loki** (1) — FEAT-150
- **page** (1) — FEAT-131
- **password-reset** (1) — FEAT-169
- **prometheus** (1) — FEAT-150
- **RAG** (1) — FEAT-228
- **redis** (1) — FEAT-204
- **ui** (1) — FEAT-208

---

## 상호 참조가 있는 문서

- FEAT-254 ↔ FEAT-248, FEAT-250
- FEAT-250 ↔ FEAT-254
- FEAT-248 ↔ FEAT-254
- FEAT-242 ↔ FEAT-233
- FEAT-233 ↔ FEAT-242
- FEAT-229 ↔ FEAT-228
- FEAT-228 ↔ FEAT-208
- FEAT-208 ↔ BUG-206, BUG-207, FEAT-228
- BUG-207 ↔ BUG-206, FEAT-208
- BUG-206 ↔ BUG-207, FEAT-208
- FEAT-182 ↔ FEAT-176
- FEAT-180 ↔ FEAT-179
- FEAT-179 ↔ FEAT-180
- FEAT-176 ↔ FEAT-182
- FEAT-149 ↔ FEAT-141
- FEAT-147 ↔ FEAT-146
- FEAT-146 ↔ FEAT-147
- FEAT-141 ↔ FEAT-089, FEAT-107, FEAT-149
- FEAT-120 ↔ FEAT-100, FEAT-107
- FEAT-107 ↔ FEAT-120, FEAT-141
- FEAT-100 ↔ FEAT-120
- FEAT-089 ↔ FEAT-085, FEAT-141
- BUG-088 ↔ FEAT-085, FEAT-087
- FEAT-087 ↔ BUG-088
- FEAT-085 ↔ BUG-088, FEAT-089
