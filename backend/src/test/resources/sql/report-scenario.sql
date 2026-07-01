-- 신고 테스트 시나리오: 신고자 / 판매자 / 신고대상 회원 / 상품 1개
-- 컨테이너 재사용 환경에서 이전 실행 잔여 데이터 정리
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE reports;
TRUNCATE TABLE products;
DELETE FROM members WHERE email IN ('reporter@test.com', 'seller@test.com', 'target@test.com');
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO members (email, password, nickname, role, status, created_at, updated_at) VALUES
  ('reporter@test.com', '$2a$10$NGovwC5mE9GpilcuwCtMtuuun4DMIDC2DE7G92qoaZE4jCXHHSam2', '신고자',   'ROLE_USER', 'ACTIVE', NOW(), NOW()),
  ('seller@test.com',   '$2a$10$NGovwC5mE9GpilcuwCtMtuuun4DMIDC2DE7G92qoaZE4jCXHHSam2', '판매자',   'ROLE_USER', 'ACTIVE', NOW(), NOW()),
  ('target@test.com',   '$2a$10$NGovwC5mE9GpilcuwCtMtuuun4DMIDC2DE7G92qoaZE4jCXHHSam2', '신고대상', 'ROLE_USER', 'ACTIVE', NOW(), NOW());

INSERT INTO products (member_id, category_id, title, description, price, trade_status, region, view_count, hidden, created_at, updated_at)
SELECT (SELECT id FROM members WHERE email = 'seller@test.com'),
       (SELECT id FROM categories ORDER BY id LIMIT 1),
       '테스트 상품', '신고 통합테스트용 상품', 50000, 'ON_SALE', '서울시 강남구', 0, false, NOW(), NOW();
