-- 회원 동네 지역 FK 연결
-- 기존 문자열 region 컬럼은 타 영역 전환 완료 전까지 유지한다.

alter table member_locations
    add column region_id bigint;

create index idx_member_locations_region_id
    on member_locations (region_id);

create unique index uk_member_locations_member_region_id
    on member_locations (member_id, region_id);

alter table member_locations
    add constraint fk_member_locations_region
        foreign key (region_id)
            references regions (id);
