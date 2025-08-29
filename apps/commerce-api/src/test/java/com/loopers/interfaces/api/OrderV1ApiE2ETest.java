package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandCommand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.payment.PaymentDetails;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCommand;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest {
    private static final String ENDPOINT = "/api/v1/orders";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointRepository pointRepository;

    private User testUser;
    private Product testProduct1;
    private Product testProduct2;

    @Autowired
    public OrderV1ApiE2ETest(TestRestTemplate testRestTemplate, DatabaseCleanUp databaseCleanUp) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
    }

    @BeforeEach
    void setUp() {
        testUser = User.of(new UserCommand.Create("testuser", "test@example.com", "1990-01-01", Gender.MALE));
        testUser = userRepository.save(testUser);

        Point userPoint = Point.create(testUser.getId());
        userPoint.charge(BigDecimal.valueOf(50000));
        pointRepository.save(userPoint);

        Brand testBrand = Brand.of(new BrandCommand.Create("Test Brand", "Test Description"));
        testBrand = brandRepository.save(testBrand);

        ProductCommand.Create product1Command = new ProductCommand.Create(
            "Test Product 1", "Test Product 1 Description", BigDecimal.valueOf(10000), 100, testBrand.getId());
        testProduct1 = Product.of(product1Command, testBrand);
        testProduct1 = productRepository.save(testProduct1);

        ProductCommand.Create product2Command = new ProductCommand.Create(
            "Test Product 2", "Test Product 2 Description", BigDecimal.valueOf(15000), 50, testBrand.getId());
        testProduct2 = Product.of(product2Command, testBrand);
        testProduct2 = productRepository.save(testProduct2);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    class CreateOrder {
        @DisplayName("단일 상품 주문에 성공할 경우, 200 ok와 주문 정보를 반환한다.")
        @Test
        void returnOrderInfo_whenCreateSingleProductOrder() {
            // arrange
            OrderV1Dto.OrderCreateRequest request = new OrderV1Dto.OrderCreateRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(testProduct1.getId(), 2)),
                    null,
                    PaymentMethod.POINT,
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUser.getAccountId());

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderCreateResponse>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dto.OrderCreateResponse>> response = testRestTemplate.exchange(
                ENDPOINT, 
                HttpMethod.POST, 
                new HttpEntity<>(request, headers), 
                responseType
            );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals("SUCCESS", response.getBody().meta().result().name()),
                () -> assertNotNull(response.getBody().data().orderId()),
                () -> assertEquals(0, BigDecimal.valueOf(20000).compareTo(response.getBody().data().totalAmount())), // 10000 * 2
                () -> assertTrue(response.getBody().data().status().equals("PAYMENT_PROCESSING") || 
                                response.getBody().data().status().equals("PAYMENT_WAITING") ||
                                response.getBody().data().status().equals("COMPLETED"))
            );
        }

        @DisplayName("여러 상품 주문에 성공할 경우, 200 ok와 주문 정보를 반환한다.")
        @Test
        void returnOrderInfo_whenCreateMultipleProductOrder() {
            // arrange
            OrderV1Dto.OrderCreateRequest request = new OrderV1Dto.OrderCreateRequest(
                    List.of(
                        new OrderV1Dto.OrderItemRequest(testProduct1.getId(), 1), // 10000원
                        new OrderV1Dto.OrderItemRequest(testProduct2.getId(), 2)  // 15000 * 2 = 30000원
                    ),
                    null,
                    PaymentMethod.POINT,
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUser.getAccountId());

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderCreateResponse>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dto.OrderCreateResponse>> response = testRestTemplate.exchange(
                ENDPOINT, 
                HttpMethod.POST, 
                new HttpEntity<>(request, headers), 
                responseType
            );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertNotNull(response.getBody()),
                () -> assertEquals("SUCCESS", response.getBody().meta().result().name()),
                () -> assertNotNull(response.getBody().data().orderId()),
                () -> assertEquals(0, BigDecimal.valueOf(40000).compareTo(response.getBody().data().totalAmount())), // 10000 + 30000
                () -> assertTrue(response.getBody().data().status().equals("PAYMENT_PROCESSING") || 
                                response.getBody().data().status().equals("PAYMENT_WAITING") ||
                                response.getBody().data().status().equals("COMPLETED"))
            );
        }

        @DisplayName("존재하지 않는 상품을 주문할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void return404NotFound_whenOrderNonExistentProduct() {
            // arrange
            OrderV1Dto.OrderCreateRequest request = new OrderV1Dto.OrderCreateRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(99999L, 1)), // 존재하지 않는 상품 ID
                    null,
                    PaymentMethod.POINT,
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUser.getAccountId());

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderCreateResponse>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dto.OrderCreateResponse>> response = testRestTemplate.exchange(
                ENDPOINT, 
                HttpMethod.POST, 
                new HttpEntity<>(request, headers), 
                responseType
            );

            // assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @DisplayName("재고보다 많은 수량을 주문할 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void return400BadRequest_whenOrderExceedsStock() {
            // arrange
            OrderV1Dto.OrderCreateRequest request = new OrderV1Dto.OrderCreateRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(testProduct1.getId(), 200)), // 재고(100)보다 많은 수량
                    null,
                    PaymentMethod.POINT,
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUser.getAccountId());

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderCreateResponse>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dto.OrderCreateResponse>> response = testRestTemplate.exchange(
                ENDPOINT, 
                HttpMethod.POST, 
                new HttpEntity<>(request, headers), 
                responseType
            );

            // assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @DisplayName("주문 항목이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void return400BadRequest_whenEmptyOrderItems() {
            // arrange
            OrderV1Dto.OrderCreateRequest request = new OrderV1Dto.OrderCreateRequest(
                    List.of(), // 빈 주문 항목
                    null,
                    PaymentMethod.POINT,
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUser.getAccountId());

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderCreateResponse>> responseType = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dto.OrderCreateResponse>> response = testRestTemplate.exchange(
                ENDPOINT, 
                HttpMethod.POST, 
                new HttpEntity<>(request, headers), 
                responseType
            );

            // assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}
