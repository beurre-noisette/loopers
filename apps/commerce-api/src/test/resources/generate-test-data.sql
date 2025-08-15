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

-- 상품 데이터 생성 (10만개)
-- 먼저 프로시저 생성
DELIMITER $$
CREATE PROCEDURE generate_products()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE total_products INT DEFAULT 100000;
    
    WHILE i <= total_products DO
        INSERT INTO product (name, description, price, stock, brand_id, created_at, updated_at)
        SELECT 
            CONCAT('Product_', i + n - 1) as name,
            CONCAT('Product Description ', i + n - 1) as description,
            FLOOR(1000 + RAND() * 99000) as price,  -- 1,000 ~ 100,000 사이 랜덤 가격
            FLOOR(10 + RAND() * 990) as stock,       -- 10 ~ 1000 사이 랜덤 재고
            FLOOR(1 + RAND() * 100) as brand_id,     -- 1 ~ 100 브랜드 중 랜덤
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
        SELECT CONCAT('Generated ', LEAST(i-1, total_products), ' / ', total_products, ' products') as progress;
    END WHILE;
END$$
DELIMITER ;

-- 프로시저 실행
CALL generate_products();

-- 좋아요 데이터 생성 (랜덤하게 일부 상품에 좋아요 추가)
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

-- 좋아요 데이터 (인기 상품은 더 많은 좋아요를 받도록)
INSERT INTO likes (user_id, target_type, target_id, created_at, updated_at)
SELECT DISTINCT
    m.id as user_id,
    'PRODUCT' as target_type,
    p.id as target_id,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as created_at,
    NOW() as updated_at
FROM member m
CROSS JOIN product p
WHERE 
    -- 상품 ID가 낮을수록(인기 상품) 더 많은 좋아요 확률
    RAND() < (CASE 
        WHEN p.id <= 100 THEN 0.5      -- 상위 100개 상품: 50% 확률
        WHEN p.id <= 1000 THEN 0.1     -- 상위 1000개 상품: 10% 확률  
        WHEN p.id <= 10000 THEN 0.01   -- 상위 10000개 상품: 1% 확률
        ELSE 0.001                      -- 나머지: 0.1% 확률
    END)
LIMIT 50000;  -- 총 5만개 좋아요