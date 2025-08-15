-- Product 테스트 데이터
-- Brand 테스트 데이터가 먼저 설정되어 있어야 함 (1: Apple, 2: Samsung, 3: Nike)

INSERT INTO product (id, name, description, price, stock, brand_id, like_count, created_at, updated_at, deleted_at) VALUES 
-- Apple 상품들 (brand_id = 1)
(1, 'iPhone 15 Pro', '티타늄 소재로 제작된 프리미엄 스마트폰', 1490000.00, 50, 1, 0, NOW(), NOW(), NULL),
(2, 'MacBook Pro 14', 'M3 Pro 칩셋이 탑재된 고성능 노트북', 2690000.00, 30, 1, 0, NOW(), NOW(), NULL),
(3, 'AirPods Pro', '액티브 노이즈 캔슬링 무선 이어폰', 329000.00, 100, 1, 0, NOW(), NOW(), NULL),

-- Samsung 상품들 (brand_id = 2)
(4, 'Galaxy S24 Ultra', 'S펜이 내장된 프리미엄 안드로이드 스마트폰', 1598000.00, 40, 2, 0, NOW(), NOW(), NULL),
(5, 'Galaxy Book4 Pro', 'AMOLED 디스플레이 탑재 노트북', 1899000.00, 25, 2, 0, NOW(), NOW(), NULL),
(6, 'Galaxy Buds3 Pro', '고음질 무선 이어폰', 269000.00, 80, 2, 0, NOW(), NOW(), NULL),

-- Nike 상품들 (brand_id = 3)
(7, 'Air Jordan 1 High', '클래식한 농구화 디자인', 179000.00, 60, 3, 0, NOW(), NOW(), NULL),
(8, 'Air Max 270', '편안한 일상용 운동화', 149000.00, 120, 3, 0, NOW(), NOW(), NULL),
(9, 'Dri-FIT Running Shirt', '땀 흡수 기능성 러닝 셋츠', 45000.00, 200, 3, 0, NOW(), NOW(), NULL),

-- 재고 부족 테스트용 상품
(10, 'Limited Edition Watch', '한정판 스마트워치', 599000.00, 0, 1, 0, NOW(), NOW(), NULL);