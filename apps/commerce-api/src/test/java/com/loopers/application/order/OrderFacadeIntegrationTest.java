package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.OrderCommand;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointReference;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Sql(scripts = {"/brand-test-data.sql", "/product-test-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderFacadeIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    private Brand testBrand;

    @BeforeEach
    @Transactional
    void setUp() {
        testBrand = brandRepository.findById(1L).orElseThrow();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Product createTestProduct(String name, int stock) {
        try {
            Constructor<Product> constructor = Product.class.getDeclaredConstructor(
                    String.class, String.class, BigDecimal.class, Integer.class, Brand.class);
            constructor.setAccessible(true);
            return constructor.newInstance(name, "테스트 상품 설명", new BigDecimal("10000"), stock, testBrand);
        } catch (Exception e) {
            throw new RuntimeException("테스트용 Product 객체 생성 실패", e);
        }
    }

    private User createTestUser(String userId) {
        User user = User.of(new UserCommand.Create(
                userId,
                userId + "@test.com",
                "1996-08-16",
                Gender.MALE
        ));
        return userRepository.save(user);
    }

    @DisplayName("재고가 부족한 경우 주문에 실패한다.")
    @Test
    void throwsInvalidInputFormatException_whenInsufficientStock() {
        // arrange
        User user = createTestUser("test");
        Product product = createTestProduct("재고 부족 상품", 5);
        Product savedProduct = productRepository.save(product);

        pointService.createPointWithInitialAmount(
                user.getId(),
                new BigDecimal("100000"),
                PointReference.welcomeBonus()
        );

        OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderCommand.CreateItem(savedProduct.getId(), 10)),
                BigDecimal.ZERO,
                null
        );

        // act
        CoreException exception = assertThrows(CoreException.class, () -> {
            orderFacade.createOrder(user.getUserId(),  command);
        });

        // assert
        assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);

        Product finalProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(5);
    }

    @DisplayName("포인트가 부족한 경우 주문에 실패한다.")
    @Test
    void throwsPaymentInsufficientPointException_whenInsufficientPoints() {
        // arrange
        User user = createTestUser("poorUser");
        Product product = createTestProduct("일반 상품", 100);
        Product savedProduct = productRepository.save(product);

        pointService.createPointWithInitialAmount(
                user.getId(),
                new BigDecimal("5000"),
                PointReference.welcomeBonus()
        );

        OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderCommand.CreateItem(savedProduct.getId(), 2)),
                BigDecimal.ZERO,
                null
        );

        // act
        CoreException exception = assertThrows(CoreException.class, () -> {
            orderFacade.createOrder(user.getUserId(), command);
        });

        // assert
        assertThat(exception.getErrorType()).isEqualTo(ErrorType.PAYMENT_INSUFFICIENT_POINT);

        Product finalProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(100);

        assertThat(pointService.getPoint(user.getId()).getBalance())
                .isEqualByComparingTo(new  BigDecimal("5000"));
    }

    @DisplayName("주문 처리중 실패 시 모든 작업이 롤백된다.")
    @Test
    void rollbackAllOperations_whenOrderProcessingFails() {
        // arrange
        User user = createTestUser("poorUser");

        Product haveEnoughStockProduct = createTestProduct("EnoughStockProduct", 10);
        Product savedEnoughStockProduct = productRepository.save(haveEnoughStockProduct);

        Product insufficientProduct = createTestProduct("InsufficientProduct", 1);
        Product savedInsufficientProduct = productRepository.save(insufficientProduct);

        pointService.createPointWithInitialAmount(
                user.getId(),
                new BigDecimal("1000000"),
                PointReference.welcomeBonus()
        );

        OrderCommand.Create command = new OrderCommand.Create(
                List.of(
                        new OrderCommand.CreateItem(savedEnoughStockProduct.getId(), 2),
                        new OrderCommand.CreateItem(savedInsufficientProduct.getId(), 5)
                ),
                new  BigDecimal("1000"),
                null
        );

        BigDecimal initialBalance = pointService.getPoint(user.getId()).getBalance();
        int initialStockOfSavedEnoughProduct = savedEnoughStockProduct.getStock();
        int initialStockOfInsufficientProduct = insufficientProduct.getStock();

        // act
        CoreException exception = assertThrows(CoreException.class, () -> {
            orderFacade.createOrder(user.getUserId(), command);
        });

        // assert
        assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);

        Product finalProduct1 = productRepository.findById(savedEnoughStockProduct.getId()).orElseThrow();
        assertThat(finalProduct1.getStock()).isEqualTo(initialStockOfSavedEnoughProduct);

        Product  finalProduct2 = productRepository.findById(savedInsufficientProduct.getId()).orElseThrow();
        assertThat(finalProduct2.getStock()).isEqualTo(initialStockOfInsufficientProduct);

        assertThat(pointService.getPoint(user.getId()).getBalance())
                .isEqualByComparingTo(initialBalance);
    }

    @DisplayName("주문 성공 시 모든 처리가 정상 반영된다.")
    @Test
    void createOrder_success_allChangesArsPersisted() {
        // arrange
        User user = createTestUser("luckyUser");

        Product productA = createTestProduct("A", 50);
        Product savedProductA = productRepository.save(productA);

        Product productB = createTestProduct("B", 30);
        Product savedProductB = productRepository.save(productB);

        BigDecimal initialAmount = new BigDecimal("100000");
        pointService.createPointWithInitialAmount(
                user.getId(),
                initialAmount,
                PointReference.welcomeBonus()
        );

        BigDecimal pointsForDiscount = new BigDecimal("1000");
        OrderCommand.Create command = new OrderCommand.Create(
                List.of(
                        new OrderCommand.CreateItem(savedProductA.getId(), 2),
                        new OrderCommand.CreateItem(savedProductB.getId(), 3)
                ),
                pointsForDiscount,
                null
        );

        int initialStockOfSavedProductA = savedProductA.getStock();
        int initialStockOfSavedProductB = savedProductB.getStock();
        BigDecimal expectedOriginalAmount = savedProductA.getPrice().multiply(BigDecimal.valueOf(2))
                .add(savedProductB.getPrice().multiply(BigDecimal.valueOf(3)));
        BigDecimal expectedFinalAmount = expectedOriginalAmount.subtract(pointsForDiscount);

        // act
        OrderInfo result = orderFacade.createOrder(user.getUserId(), command);

        // assert
        // 1. 주문 정보 검증
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(user.getUserId());
        assertThat(result.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(result.orderItems()).hasSize(2);
        assertThat(result.originalAmount()).isEqualByComparingTo(expectedOriginalAmount); // 주문 총액
        assertThat(result.pointDiscount()).isEqualByComparingTo(pointsForDiscount); // 할인 포인트
        assertThat(result.finalAmount()).isEqualByComparingTo(expectedFinalAmount); // 실제 결제 금액

        // 2. 재고 차감 검증
        Product finalProductA = productRepository.findById(savedProductA.getId()).orElseThrow();
        assertThat(finalProductA.getStock()).isEqualTo(initialStockOfSavedProductA - 2);

        Product finalProductB = productRepository.findById(savedProductB.getId()).orElseThrow();
        assertThat(finalProductB.getStock()).isEqualTo(initialStockOfSavedProductB - 3);

        // 3. 포인트 차감 검증 (할인 포인트 + 결제 금액)
        Point finalPoint = pointService.getPoint(user.getId());
        BigDecimal expectedRemainingPoints = initialAmount
                .subtract(pointsForDiscount) // 할인용 포인트
                .subtract(expectedFinalAmount); // 실제 결제 금액
        assertThat(finalPoint.getBalance()).isEqualByComparingTo(expectedRemainingPoints);
    }

    @DisplayName("쿠폰을 사용한 주문이 성공한다.")
    @Test
    void createOrder_withCoupon_success() {
        // arrange
        User user = createTestUser("couponUser");

        Product product = createTestProduct("쿠폰 적용 상품", 10);
        Product savedProduct = productRepository.save(product);

        pointService.createPointWithInitialAmount(
                user.getId(),
                new BigDecimal("100000"),
                PointReference.welcomeBonus()
        );

        Coupon coupon = Coupon.createFixedAmount(
                "5000원 할인 쿠폰",
                new BigDecimal("5000"),
                new BigDecimal("8000"),
                ZonedDateTime.now().minusDays(1),
                ZonedDateTime.now().plusDays(30)
        );

        Coupon savedCoupon = couponRepository.save(coupon);
        UserCoupon userCoupon = couponService.issueCoupon(user.getId(), savedCoupon.getId());

        BigDecimal pointsForDiscount = new BigDecimal("1000");
        OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderCommand.CreateItem(savedProduct.getId(), 1)),
                pointsForDiscount,
                userCoupon.getId()
        );

        // act
        OrderInfo result = orderFacade.createOrder(user.getUserId(), command);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.originalAmount()).isEqualByComparingTo(savedProduct.getPrice());
        assertThat(result.pointDiscount()).isEqualByComparingTo(pointsForDiscount);
    }

    @DisplayName("존재하지 않는 쿠폰으로 주문하면 실패한다")
    @Test
    void createOrder_withNonExistentCoupon_fails() {
        // arrange
        User user = createTestUser("failUser");
        Product product = createTestProduct("테스트 상품", 10);
        Product savedProduct = productRepository.save(product);

        pointService.createPointWithInitialAmount(
                user.getId(),
                new BigDecimal("100000"),
                PointReference.welcomeBonus()
        );

        OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderCommand.CreateItem(savedProduct.getId(), 1)),
                BigDecimal.ZERO,
                999L // 존재하지 않는 쿠폰 ID
        );

        // act
        CoreException exception = assertThrows(CoreException.class, () ->
                orderFacade.createOrder(user.getUserId(), command)
        );

        // assert
        assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }

    @DisplayName("이미 사용된 쿠폰으로 주문하면 실패하고 모든 작업이 롤백된다")
    @Test
    void createOrder_withUsedCoupon_failsAndRollback() {
        // arrange
        User user = createTestUser("poorUser");
        Product product = createTestProduct("롤백 테스트 상품", 10);
        Product savedProduct = productRepository.save(product);

        pointService.createPointWithInitialAmount(
                user.getId(),
                new BigDecimal("100000"),
                PointReference.welcomeBonus()
        );

        Coupon coupon = Coupon.createFixedAmount(
                "사용될 쿠폰",
                new BigDecimal("3000"),
                new BigDecimal("5000"),
                ZonedDateTime.now().minusDays(1),
                ZonedDateTime.now().plusDays(30)
        );
        Coupon savedCoupon = couponRepository.save(coupon);
        UserCoupon userCoupon = couponService.issueCoupon(user.getId(), savedCoupon.getId());

        userCoupon.use(888L); // 더미 주문 ID로 사용 처리
        couponRepository.save(userCoupon);

        BigDecimal initialBalance = pointService.getPoint(user.getId()).getBalance();
        int initialStock = savedProduct.getStock();

        OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderCommand.CreateItem(savedProduct.getId(), 1)),
                new BigDecimal("1000"),
                userCoupon.getId()
        );

        // act
        CoreException exception = assertThrows(CoreException.class, () ->
                orderFacade.createOrder(user.getUserId(), command)
        );

        // assert
        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        // 롤백 검증
        Product finalProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(initialStock); // 재고 롤백

        assertThat(pointService.getPoint(user.getId()).getBalance())
                .isEqualByComparingTo(initialBalance); // 포인트 롤백
    }
}
