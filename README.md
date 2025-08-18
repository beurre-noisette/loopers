# 🛍️ LooCommerce - 이커머스 시스템 구현 프로젝트

+ 개발방법론과 아키텍쳐에 대한 숙련, 다양한 도전 과제를 이커머스라는 주제 안에서 해결해 나가는 프로젝트입니다. 

## 💡 핵심 구현 사항

### 1. 아키텍처 설계 및 구현
- **레이어드 아키텍처 + DDD 적용**
    - Domain Layer: 비즈니스 로직 캡슐화
    - Application Layer: 유스케이스 오케스트레이션
    - Infrastructure Layer: 외부 시스템 연동
    - 클라이언트의 요청 관심사에 따른 Command/Query 계층 분리

### 2. 동시성 제어 및 트랜잭션 관리
- **재고 관리 시 동시성 문제 해결**
    - [비관적 락을 활용한 재고 차감 정합성 보장](https://github.com/beurre-noisette/loopers/blob/main/apps/commerce-api/src/test/java/com/loopers/domain/product/StockConcurrencyTest.java#L49-L97)
    - [데드락 방지를 위한 정렬 기반 락 획득 전략 구현](https://github.com/beurre-noisette/loopers/blob/main/apps/commerce-api/src/test/java/com/loopers/domain/product/DeadlockPreventionTest.java#L60-L154)
- **좋아요 동시성 테스트**
  - [동일 사용자가 동시에 여러번 좋아요를 눌러도 하나만 생성](https://github.com/beurre-noisette/loopers/blob/main/apps/commerce-api/src/test/java/com/loopers/domain/like/LikeConcurrencyTest.java#L65-L92)
- **쿠폰 동시성 테스트**
  - [동일한 쿠폰을 여러 쓰레드에서 동시에 사용해도 단 한번만 사용되는지 검증](https://github.com/beurre-noisette/loopers/blob/main/apps/commerce-api/src/test/java/com/loopers/domain/coupon/CouponConcurrencyTest.java#L58-L103)

- **포인트/쿠폰 사용 시 원자성 보장**
    - [주문 흐름 중 하나라도 실패 시 전체 롤백 처리](https://github.com/beurre-noisette/loopers/blob/main/apps/commerce-api/src/test/java/com/loopers/application/order/OrderFacadeIntegrationTest.java#L170-L217)
    - [실패한 주문은 완전히 롤백되고, 성공한 주문만 커밋함](https://github.com/beurre-noisette/loopers/blob/main/apps/commerce-api/src/test/java/com/loopers/domain/order/OrderConcurrencyTest.java#L191-L249)

### 3. 인덱스 최적화와 캐싱을 통한 조회 성능 향상
- [좋아요순 정렬 1.2초 -> 9ms 성능 개선기](https://berrue.tistory.com/9)

### 4. 도메인 모델링
- **주요 도메인 설계**
    - [Product](https://github.com/beurre-noisette/loopers/blob/main/apps/commerce-api/src/main/java/com/loopers/domain/product/Product.java#L12-L61): 재고 관리 및 비즈니스 규칙
    - [Order](https://github.com/beurre-noisette/loopers/blob/main/apps/commerce-api/src/main/java/com/loopers/domain/order/Order.java#L11-L69): 주문 생성 및 결제 처리
      - [OrderItem](https://github.com/beurre-noisette/loopers/blob/main/apps/commerce-api/src/main/java/com/loopers/domain/order/OrderItem.java#L8-L41)
      - [OrderItems](https://github.com/beurre-noisette/loopers/blob/main/apps/commerce-api/src/main/java/com/loopers/domain/order/OrderItems.java#L12-L74)
    - [Coupon](https://github.com/beurre-noisette/loopers/blob/main/apps/commerce-api/src/main/java/com/loopers/domain/coupon/Coupon.java#L13-L104): 정액/정률 할인 및 사용 처리
