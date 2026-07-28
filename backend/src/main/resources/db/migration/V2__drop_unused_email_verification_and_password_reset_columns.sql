-- 인증 관련 TTL 데이터를 Redis로 이전하며 미사용이 된
-- 컬럼/테이블을 정리한다.
--   - email_verifications: 인증 코드(code/sent_at/expires_at)는 Redis(TTL 5분)로 이전.
--     "인증 완료" 여부(verified/verified_at)만 이 테이블에 남는다.
--   - password_reset_tokens: Redis 2-key 구조(auth:password:reset:member:{id} / ...:token:{hash})로
--     완전히 대체되어 테이블 자체가 미사용.

alter table email_verifications
    drop column code,
    drop column sent_at,
    drop column expires_at;

drop table password_reset_tokens;
