-- 소셜 로그인(카카오/구글) 도입을 위한 스키마 변경.
--   - members.local_login_enabled: 비밀번호 기반 로그인 가능 여부. 기존 로컬 회원은 true로 백필한 뒤
--     DEFAULT를 제거해, 이후 모든 INSERT가 이 값을 명시적으로 지정하도록 강제한다.
--   - member_social_accounts: 회원과 소셜 제공자 계정의 연동 정보. 별도 테이블로 분리해 향후 한 회원이
--     복수 provider를 연결할 수 있게 하고, 로컬 인증정보(members.password)와 소셜 인증정보를 분리한다.

-- 컬럼 타입(bit(1))은 Hibernate의 Java boolean -> MySQL 매핑 관례를 따른 것으로, 이 프로젝트에 boolean
-- 컬럼 선례가 아직 없다. prod 반영 전 dev 프로필(ddl-auto=update)로 실제 생성 타입을 한 번 확인해 필요하면 맞춘다
-- (prod의 ddl-auto=validate가 타입 불일치 시 기동 실패로 잡아준다 — ADR 0003/0004).
alter table members
    add column local_login_enabled bit(1) not null default true;

alter table members
    alter column local_login_enabled drop default;

create table member_social_accounts (
    id               bigint not null auto_increment,
    created_at       datetime(6),
    updated_at       datetime(6),
    member_id        bigint not null,
    provider         enum ('KAKAO','GOOGLE') not null,
    provider_user_id varchar(255) not null,
    primary key (id),
    unique key uk_social_account_provider_provider_user_id (provider, provider_user_id),
    unique key uk_social_account_member_provider (member_id, provider),
    constraint fk_social_account_member
        foreign key (member_id) references members (id) on delete restrict
) engine=InnoDB;
