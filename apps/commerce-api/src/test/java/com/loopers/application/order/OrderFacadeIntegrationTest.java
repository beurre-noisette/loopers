package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.OrderCommand;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.PaymentDetails;
import com.loopers.domain.payment.PaymentMethod;
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

    private User createTestUser(String accountId) {
        User user = User.of(new UserCommand.Create(
                accountId,
                accountId + "@test.com",
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
                null,
                new PaymentDetails.Point()
        );

        // act
        CoreException exception = assertThrows(CoreException.class, () -> {
            orderFacade.createOrder(user.getAccountId(), command);
        });

        // assert
        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        Product finalProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(5);
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
                null,
                new PaymentDetails.Point()
        );

        BigDecimal initialBalance = pointService.getPoint(user.getId()).getBalance();
        int initialStockOfSavedEnoughProduct = savedEnoughStockProduct.getStock();
        int initialStockOfInsufficientProduct = insufficientProduct.getStock();

        // act
        CoreException exception = assertThrows(CoreException.class, () -> {
            orderFacade.createOrder(user.getAccountId(), command);
        });

        // assert
        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        Product finalProduct1 = productRepository.findById(savedEnoughStockProduct.getId()).orElseThrow();
        assertThat(finalProduct1.getStock()).isEqualTo(initialStockOfSavedEnoughProduct);

        Product  finalProduct2 = productRepository.findById(savedInsufficientProduct.getId()).orElseThrow();
        assertThat(finalProduct2.getStock()).isEqualTo(initialStockOfInsufficientProduct);

        assertThat(pointService.getPoint(user.getId()).getBalance())
                .isEqualByComparingTo(initialBalance);
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
                userCoupon.getId(),
                new PaymentDetails.Point()
        );

        // act
        OrderInfo result = orderFacade.createOrder(user.getAccountId(), command);

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
                999L, // 존재하지 않는 쿠폰 ID
                new PaymentDetails.Point()
        );

        // act
        CoreException exception = assertThrows(CoreException.class, () ->
                orderFacade.createOrder(user.getAccountId(), command)
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
                userCoupon.getId(),
                new PaymentDetails.Point()
        );

        // act
        CoreException exception = assertThrows(CoreException.class, () ->
                orderFacade.createOrder(user.getAccountId(), command)
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
