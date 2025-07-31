-- 브랜드 테스트 데이터
INSERT INTO brand (id, name, description, created_at, updated_at)
VALUES 
    (1, '나이키', '스포츠 브랜드', NOW(), NOW()),
    (2, '아디다스', '독일 스포츠 브랜드', NOW(), NOW());

-- 상품 테스트 데이터
INSERT INTO product (id, name, description, price, stock, brand_id, created_at, updated_at)
VALUES 
    -- 나이키 상품
    (1, '나이키 에어맥스', '편안한 운동화', 150000, 100, 1, '2024-01-01 10:00:00', NOW()),
    (2, '나이키 조던', '농구화', 200000, 50, 1, '2024-01-02 10:00:00', NOW()),
    (3, '나이키 런닝화', '가벼운 런닝화', 120000, 80, 1, '2024-01-03 10:00:00', NOW()),
    
    -- 아디다스 상품
    (4, '아디다스 울트라부스트', '쿠션 좋은 운동화', 180000, 60, 2, '2024-01-04 10:00:00', NOW()),
    (5, '아디다스 스탠스미스', '클래식 스니커즈', 90000, 120, 2, '2024-01-05 10:00:00', NOW()),
    (6, '아디다스 슈퍼스타', '레트로 스니커즈', 100000, 90, 2, '2024-01-06 10:00:00', NOW());

-- 사용자 테스트 데이터
INSERT INTO member (id, user_id, email, birth_date, gender, point, created_at, updated_at)
VALUES 
    (1, 'user1', 'user1@example.com', '1990-01-01', 'MALE', 1000, NOW(), NOW()),
    (2, 'user2', 'user2@example.com', '1995-05-05', 'FEMALE', 2000, NOW(), NOW()),
    (3, 'user3', 'user3@example.com', '1988-12-25', 'MALE', 3000, NOW(), NOW());

-- 좋아요 테스트 데이터
INSERT INTO likes (id, user_id, target_type, target_id, created_at, updated_at)
VALUES 
    -- 상품 1(나이키 에어맥스)에 대한 좋아요 3개
    (1, 1, 'PRODUCT', 1, NOW(), NOW()),
    (2, 2, 'PRODUCT', 1, NOW(), NOW()),
    (3, 3, 'PRODUCT', 1, NOW(), NOW()),
    
    -- 상품 2(나이키 조던)에 대한 좋아요 2개
    (4, 1, 'PRODUCT', 2, NOW(), NOW()),
    (5, 2, 'PRODUCT', 2, NOW(), NOW()),
    
    -- 상품 4(아디다스 울트라부스트)에 대한 좋아요 1개
    (6, 1, 'PRODUCT', 4, NOW(), NOW());