-- 데이터베이스 선택
USE loopers;

-- 기존 데이터 정리 (안전을 위해)
SET FOREIGN_KEY_CHECKS = 0;
truncate likes;
truncate product;
truncate brand;
truncate member;
SET FOREIGN_KEY_CHECKS = 1;

-- 브랜드 데이터 생성 (100개)
INSERT INTO brand (name, description, created_at, updated_at)
SELECT 
    CONCAT('Brand_', n) as name,
    CONCAT('Brand Description ', n) as description,
    NOW() as created_at,
    NOW() as updated_at
FROM (
    SELECT a.N + b.N * 10 + 1 as n
    FROM 
        (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
        (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
    ORDER BY n
) numbers;

-- 상품 데이터 생성 (10만개) - like_count 포함
-- 먼저 프로시저 생성
DELIMITER $$
CREATE PROCEDURE generate_products_with_like_count()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE total_products INT DEFAULT 100000;
    
    WHILE i <= total_products DO
        INSERT INTO product (name, description, price, stock, brand_id, like_count, created_at, updated_at)
        SELECT 
            CONCAT('Product_', i + n - 1) as name,
            CONCAT('Product Description ', i + n - 1) as description,
            FLOOR(1000 + RAND() * 99000) as price,  -- 1,000 ~ 100,000 사이 랜덤 가격
            FLOOR(10 + RAND() * 990) as stock,       -- 10 ~ 1000 사이 랜덤 재고
            FLOOR(1 + RAND() * 100) as brand_id,     -- 1 ~ 100 브랜드 중 랜덤
            0 as like_count,                         -- 초기값 0 (나중에 업데이트)
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as created_at,  -- 최근 1년 내 랜덤 날짜
            NOW() as updated_at
        FROM (
            SELECT a.N + b.N * 10 + c.N * 100 + 1 as n
            FROM 
                (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
                (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
                (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
            WHERE (i + a.N + b.N * 10 + c.N * 100) <= total_products
            ORDER BY n
        ) numbers;
        
        SET i = i + batch_size;
        
        -- 진행 상황 출력
        IF i % 10000 = 1 THEN
            SELECT CONCAT('Generated ', i - 1, ' products...') as progress;
        END IF;
    END WHILE;
    
    SELECT 'Product generation completed!' as result;
END$$
DELIMITER ;

-- 프로시저 실행
CALL generate_products_with_like_count();

-- 프로시저 정리
DROP PROCEDURE generate_products_with_like_count;

-- 사용자 데이터 생성 (1000명)
INSERT INTO member (user_id, gender, birth_date, email, created_at, updated_at)
SELECT
    CONCAT('user_', n) as user_id,
    IF(RAND() > 0.5, 'MALE', 'FEMALE') as gender,
    DATE_SUB(CURDATE(), INTERVAL FLOOR(20 + RAND() * 40) YEAR) as birth_date,
    CONCAT('user', n, '@test.com') as email,
    NOW() as created_at,
    NOW() as updated_at
FROM (
         SELECT a.N + b.N * 10 + c.N * 100 + 1 as n
         FROM
             (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
             (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
             (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
         WHERE (a.N + b.N * 10 + c.N * 100 + 1) <= 1000  -- 1000명의 유저
     ) numbers;

-- 좋아요 데이터 생성 (5만개)
-- 랜덤하게 사용자가 상품에 좋아요를 누름
DELIMITER $$
CREATE PROCEDURE generate_likes_data()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_likes INT DEFAULT 50000;
    DECLARE random_user_id INT;
    DECLARE random_product_id INT;
    DECLARE duplicate_count INT DEFAULT 0;
    
    WHILE i < max_likes DO
        -- 랜덤 사용자와 상품 선택
        SET random_user_id = FLOOR(1 + RAND() * 1000);
        SET random_product_id = FLOOR(1 + RAND() * 100000);
        
        -- 중복 체크 (같은 사용자가 같은 상품에 좋아요를 누를 수 없음)
        SELECT COUNT(*) INTO duplicate_count
        FROM likes 
        WHERE user_id = random_user_id 
        AND target_type = 'PRODUCT' 
        AND target_id = random_product_id;
        
        -- 중복이 아닌 경우에만 삽입
        IF duplicate_count = 0 THEN
            INSERT INTO likes (user_id, target_type, target_id, created_at, updated_at)
            VALUES (random_user_id, 'PRODUCT', random_product_id, NOW(), NOW());
            
            SET i = i + 1;
            
            -- 진행 상황 출력
            IF i % 5000 = 0 THEN
                SELECT CONCAT('Generated ', i, ' likes...') as progress;
            END IF;
        END IF;
    END WHILE;
    
    SELECT 'Likes generation completed!' as result;
END$$
DELIMITER ;

-- 프로시저 실행
CALL generate_likes_data();

-- 프로시저 정리
DROP PROCEDURE generate_likes_data;

-- 중요: Product 테이블의 like_count 업데이트
-- 각 상품의 실제 좋아요 수를 계산하여 like_count에 저장
UPDATE product p 
SET like_count = (
    SELECT COUNT(*) 
    FROM likes l 
    WHERE l.target_type = 'PRODUCT' 
    AND l.target_id = p.id
)
where p.id > 0;

-- Phase 2에서 생성한 인덱스들 재생성
CREATE INDEX idx_product_brand_created ON product(brand_id, created_at DESC);
CREATE INDEX idx_product_brand_price ON product(brand_id, price);
CREATE INDEX idx_like_target ON likes(target_type, target_id);

-- Phase 3을 위한 새로운 인덱스 생성
CREATE INDEX idx_product_like_count ON product(like_count DESC);

-- 데이터 생성 결과 확인
SELECT 'Data generation summary:' as info;
SELECT COUNT(*) as brand_count FROM brand;
SELECT COUNT(*) as product_count FROM product;
SELECT COUNT(*) as user_count FROM member;
SELECT COUNT(*) as likes_count FROM likes;

-- like_count 분포 확인
SELECT 
    MIN(like_count) as min_likes,
    MAX(like_count) as max_likes,
    AVG(like_count) as avg_likes,
    COUNT(CASE WHEN like_count > 0 THEN 1 END) as products_with_likes,
    COUNT(CASE WHEN like_count = 0 THEN 1 END) as products_without_likes
FROM product;

-- 상위 10개 인기 상품 확인
SELECT id, name, like_count, brand_id 
FROM product 
ORDER BY like_count DESC 
LIMIT 10;

SELECT 'Test data generation completed successfully!' as final_message;

