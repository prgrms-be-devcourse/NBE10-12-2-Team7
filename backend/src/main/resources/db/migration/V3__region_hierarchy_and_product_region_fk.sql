-- 계층형 지역 마스터 도입 + 상품 지역 FK 연결
-- 기존 평면 regions 기준데이터는 문자열 마스터였고 FK 참조가 없으므로 비운 뒤 RegionSeeder가 CSV로 재주입한다.

delete from regions;

alter table regions
    add column code varchar(10),
    add column level integer not null,
    add column parent_id bigint,
    add column full_name varchar(100) not null,
    add column display_name varchar(50) not null,
    add column latitude decimal(10,7),
    add column longitude decimal(10,7),
    modify column name varchar(100);

alter table regions
    add constraint uk_regions_code unique (code);

create index idx_regions_code
    on regions (code);

create index idx_regions_parent_id
    on regions (parent_id);

create index idx_regions_level
    on regions (level);

alter table regions
    add constraint fk_regions_parent
        foreign key (parent_id)
            references regions (id);

alter table products
    add column region_id bigint;

create index idx_products_region_id
    on products (region_id);

alter table products
    add constraint fk_products_region
        foreign key (region_id)
            references regions (id);
