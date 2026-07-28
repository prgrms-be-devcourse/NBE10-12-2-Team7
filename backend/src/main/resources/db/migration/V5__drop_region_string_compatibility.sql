-- 지역 문자열 호환 컬럼 제거
-- 전제: products.region_id, member_locations.region_id 백필 완료.

drop index idx_products_region on products;
drop index uk_member_locations_member_region on member_locations;
drop index UK1m9qnhbk56c8iskxvfupln9me on regions;

alter table products
    modify column region_id bigint not null;

alter table member_locations
    modify column region_id bigint not null;

alter table products
    drop column region;

alter table member_locations
    drop column region;

alter table regions
    drop column name;
