-- 시드전략 2: 특정 테스트 전용 시나리오 데이터 (@Sql 로 주입)
-- 판매자 1명 + 상품 1개. 카테고리는 CategoryInitializer 가 시드한 첫 행을 사용.
INSERT INTO members (email, password, nickname, role, status, created_at, updated_at)
VALUES ('seller@test.com', '{noop}pw', '판매자', 'ROLE_USER', 'ACTIVE', NOW(), NOW());

INSERT INTO products (member_id, category_id, title, description, price, trade_status, region, view_count, hidden, created_at, updated_at)
SELECT (SELECT id FROM members WHERE email = 'seller@test.com'),
       (SELECT id FROM categories ORDER BY id LIMIT 1),
       '아이폰 15', '미개봉입니다', 900000, 'ON_SALE', '서울시 강남구', 0, false, NOW(), NOW();
