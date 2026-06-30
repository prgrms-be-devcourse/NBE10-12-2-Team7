-- 시나리오 테이블만 정리. 시드(관리자 계정 · 카테고리)는 유지.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE favorites;
TRUNCATE TABLE comments;
TRUNCATE TABLE reports;
TRUNCATE TABLE products;
DELETE FROM members WHERE email <> 'admin@dongnemarket.com';
SET FOREIGN_KEY_CHECKS = 1;
