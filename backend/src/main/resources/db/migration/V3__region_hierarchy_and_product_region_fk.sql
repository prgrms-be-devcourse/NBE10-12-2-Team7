-- 위치 기능 계층화(docs/superpowers/specs/2026-07-26-region-hierarchy-design.md).
-- 지역 마스터를 flat(name)에서 법정동 계층(code/level/parent_id/full_name/display_name)으로 재구성하고,
-- 상품이 지역을 문자열이 아니라 동(level3) FK로 참조하도록 전환한다.
--
-- 하드컷 전환: 기존 문자열 region 데이터는 보존하지 않는다(사전 합의된 결정). 이 마이그레이션은
-- 상품 데이터가 없는(또는 재시드 대상) 상태를 전제로 한다. regions는 RegionSeeder가 재적재한다.
-- member_locations.region은 이번 범위 밖으로 문자열을 유지한다.

-- 1. regions 재구성: 기존 flat 구조를 버리고 계층 구조로 재생성한다.
--    (regions.id를 참조하는 인바운드 FK가 없어 안전하게 재생성 가능)
drop table if exists regions;

create table regions (
    id           bigint       not null auto_increment,
    code         char(10)     not null,
    level        int          not null,
    parent_id    bigint       null,
    full_name    varchar(100) not null,
    display_name varchar(50)  not null,
    primary key (id)
) engine=InnoDB;

alter table regions
    add constraint uk_regions_code unique (code);

create index idx_regions_parent
    on regions (parent_id);

alter table regions
    add constraint fk_regions_parent
    foreign key (parent_id) references regions (id);

-- 2. products: 문자열 region → region_id FK(동, NOT NULL) 전환.
drop index idx_products_region on products;

alter table products
    drop column region;

alter table products
    add column region_id bigint not null;

create index idx_products_region
    on products (region_id);

alter table products
    add constraint fk_products_region
    foreign key (region_id) references regions (id);
