package com.loopers.application.order;

import com.loopers.domain.order.*;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.StockManagementService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private StockManagementService stockManagementService;

    @Mock
    private PointService pointService;

    @InjectMocks
    private OrderFacade orderFacade;

    @DisplayName("주문 생성 시")
    @Nested
    class CreateOrder {

        @DisplayName("올바른 정보가 주어지면 주문이 성공적으로 생성된다.")
        @Test
        void createOrder_success() {
            // arrange
            String userId = "testUser";
            OrderCommand.CreateItem item1 = new OrderCommand.CreateItem(1L, 2);
            OrderCommand.CreateItem item2 = new OrderCommand.CreateItem(2L, 1);
            OrderCommand.Create command = new OrderCommand.Create(List.of(item1, item2));

            User user = createTestUser(userId, 50000);
            Product product1 = createTestProduct(1L, "상품1", new BigDecimal("10000"));
            Product product2 = createTestProduct(2L, "상품2", new BigDecimal("5000"));
            List<Product> products = List.of(product1, product2);

            OrderItems orderItems = OrderItems.create(command.items(), products);
            Order expectedOrder = Order.create(userId, orderItems);

            when(userService.findByUserId(userId)).thenReturn(user);
            when(productService.findProductsByIds(List.of(1L, 2L))).thenReturn(products);
            when(orderService.createOrder(eq(userId), any(OrderItems.class))).thenReturn(expectedOrder);

            // act
            OrderInfo result = orderFacade.createOrder(userId, command);

            // assert
            assertAll(
                    () -> assertThat(result.userId()).isEqualTo(userId),
                    () -> assertThat(result.orderItems()).hasSize(2),
                    () -> assertThat(result.status()).isEqualTo(OrderStatus.PENDING),
                    () -> assertThat(result.totalAmount()).isEqualTo(new BigDecimal("25000"))
            );

            verify(userService).findByUserId(userId);
            verify(productService).findProductsByIds(List.of(1L, 2L));
            verify(stockManagementService).validateStock(eq(products), any(OrderItems.class));
            verify(stockManagementService).decreaseStock(eq(products), any(OrderItems.class));
            verify(pointService).usePoint(eq(user.getId()), eq(BigDecimal.valueOf(25000)), any());
            verify(orderService).createOrder(eq(userId), any(OrderItems.class));
        }

        @DisplayName("사용자가 존재하지 않으면 예외가 발생한다.")
        @Test
        void createOrder_throwsException_whenUserNotFound() {
            // arrange
            String userId = "nonExistentUser";
            OrderCommand.CreateItem item = new OrderCommand.CreateItem(1L, 1);
            OrderCommand.Create command = new OrderCommand.Create(List.of(item));

            when(userService.findByUserId(userId))
                    .thenThrow(new CoreException(ErrorType.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> orderFacade.createOrder(userId, command));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.USER_NOT_FOUND);
            verify(userService).findByUserId(userId);
            verifyNoInteractions(productService, stockManagementService, orderService);
        }

        @DisplayName("상품이 존재하지 않으면 예외가 발생한다.")
        @Test
        void createOrder_throwsException_whenProductNotFound() {
            // arrange
            String userId = "testUser";
            OrderCommand.CreateItem item = new OrderCommand.CreateItem(999L, 1);
            OrderCommand.Create command = new OrderCommand.Create(List.of(item));

            User user = createTestUser(userId, 50000);

            when(userService.findByUserId(userId)).thenReturn(user);
            when(productService.findProductsByIds(List.of(999L)))
                    .thenThrow(new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> orderFacade.createOrder(userId, command));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            verify(userService).findByUserId(userId);
            verify(productService).findProductsByIds(List.of(999L));
            verifyNoMoreInteractions(productService, stockManagementService, orderService);
        }

        @DisplayName("재고가 부족하면 예외가 발생한다.")
        @Test
        void createOrder_throwsException_whenInsufficientStock() {
            // arrange
            String userId = "testUser";
            OrderCommand.CreateItem item = new OrderCommand.CreateItem(1L, 10);
            OrderCommand.Create command = new OrderCommand.Create(List.of(item));

            User user = createTestUser(userId, 50000);
            Product product = createTestProduct(1L, "상품1", new BigDecimal("10000"));
            List<Product> products = List.of(product);
            
            OrderItems orderItems = OrderItems.create(command.items(), products);

            when(userService.findByUserId(userId)).thenReturn(user);
            when(productService.findProductsByIds(List.of(1L))).thenReturn(products);
            doThrow(new CoreException(ErrorType.INVALID_INPUT_FORMAT, "재고가 부족합니다."))
                    .when(stockManagementService).validateStock(eq(products), any(OrderItems.class));

            // act
            CoreException exception = assertThrows(CoreException.class,
                    () -> orderFacade.createOrder(userId, command));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");
            
            verify(userService).findByUserId(userId);
            verify(productService).findProductsByIds(List.of(1L));
            verify(stockManagementService).validateStock(eq(products), any(OrderItems.class));
            verify(stockManagementService, never()).decreaseStock(any(), any());
            verify(pointService, never()).usePoint(any(), any(), any());
            verify(orderService, never()).createOrder(any(), any());
        }

        @DisplayName("포인트가 부족하면 예외가 발생한다.")
        @Test
        void createOrder_throwsException_whenInsufficientPoint() {
            // arrange
            String userId = "testUser";
            OrderCommand.CreateItem item = new OrderCommand.CreateItem(1L, 1);
            OrderCommand.Create command = new OrderCommand.Create(List.of(item));

            User user = createTestUser(userId, 5000);
            Product product = createTestProduct(1L, "상품1", new BigDecimal("10000"));
            List<Product> products = List.of(product);
            
            OrderItems orderItems = OrderItems.create(command.items(), products);

            when(userService.findByUserId(userId)).thenReturn(user);
            when(productService.findProductsByIds(List.of(1L))).thenReturn(products);
            Order mockOrder = mock(Order.class);
            when(mockOrder.getId()).thenReturn(1L);
            when(orderService.createOrder(eq(userId), any(OrderItems.class))).thenReturn(mockOrder);
            doThrow(new CoreException(ErrorType.INVALID_INPUT_FORMAT, "포인트가 부족합니다."))
                    .when(pointService).usePoint(eq(user.getId()), eq(BigDecimal.valueOf(10000)), any());

            // act
            CoreException exception = assertThrows(CoreException.class,
                    () -> orderFacade.createOrder(userId, command));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("포인트가 부족합니다");
            
            verify(userService).findByUserId(userId);
            verify(productService).findProductsByIds(List.of(1L));
            verify(stockManagementService).validateStock(eq(products), any(OrderItems.class));
            verify(stockManagementService).decreaseStock(eq(products), any(OrderItems.class));
            verify(orderService).createOrder(eq(userId), any(OrderItems.class));
            verify(pointService).usePoint(eq(user.getId()), eq(BigDecimal.valueOf(10000)), any());
        }
    }

    @DisplayName("도메인 서비스 상호작용 테스트")
    @Nested
    class DomainServiceInteraction {

        @DisplayName("모든 도메인 서비스가 올바른 순서로 호출된다.")
        @Test
        void createOrder_callsServicesInCorrectOrder() {
            // arrange
            String userId = "testUser";
            OrderCommand.CreateItem item = new OrderCommand.CreateItem(1L, 1);
            OrderCommand.Create command = new OrderCommand.Create(List.of(item));

            User user = createTestUser(userId, 50000);
            Product product = createTestProduct(1L, "상품1", new BigDecimal("10000"));
            List<Product> products = List.of(product);
            
            OrderItems orderItems = OrderItems.create(command.items(), products);
            Order expectedOrder = Order.create(userId, orderItems);

            when(userService.findByUserId(userId)).thenReturn(user);
            when(productService.findProductsByIds(List.of(1L))).thenReturn(products);
            when(orderService.createOrder(eq(userId), any(OrderItems.class))).thenReturn(expectedOrder);

            // act
            orderFacade.createOrder(userId, command);

            // assert - 호출 순서 검증
            var inOrder = inOrder(userService, productService, stockManagementService, pointService, orderService);
            inOrder.verify(userService).findByUserId(userId);
            inOrder.verify(productService).findProductsByIds(List.of(1L));
            inOrder.verify(stockManagementService).validateStock(eq(products), any(OrderItems.class));
            inOrder.verify(stockManagementService).decreaseStock(eq(products), any(OrderItems.class));
            inOrder.verify(orderService).createOrder(eq(userId), any(OrderItems.class));
            inOrder.verify(pointService).usePoint(eq(user.getId()), eq(BigDecimal.valueOf(10000)), any());
        }

        @DisplayName("OrderItems가 올바른 정보로 생성되고 주문에 사용된다.")
        @Test
        void createOrder_usesCorrectOrderItems() {
            // arrange
            String userId = "testUser";
            OrderCommand.CreateItem item = new OrderCommand.CreateItem(1L, 3);
            OrderCommand.Create command = new OrderCommand.Create(List.of(item));

            User user = createTestUser(userId, 50000);
            Product product = createTestProduct(1L, "상품1", new BigDecimal("15000"));
            List<Product> products = List.of(product);
            
            OrderItems orderItems = OrderItems.create(command.items(), products);
            Order expectedOrder = Order.create(userId, orderItems);
            
            when(userService.findByUserId(userId)).thenReturn(user);
            when(productService.findProductsByIds(List.of(1L))).thenReturn(products);
            when(orderService.createOrder(eq(userId), any(OrderItems.class))).thenReturn(expectedOrder);

            // act
            OrderInfo result = orderFacade.createOrder(userId, command);

            // assert
            assertAll(
                    () -> assertThat(result.totalAmount()).isEqualTo(new BigDecimal("45000")),
                    () -> assertThat(result.orderItems()).hasSize(1),
                    () -> assertThat(result.orderItems().get(0).quantity()).isEqualTo(3),
                    () -> assertThat(result.orderItems().get(0).unitPrice()).isEqualTo(new BigDecimal("15000"))
            );
            verify(orderService).createOrder(eq(userId), any(OrderItems.class));
        }
    }

    private User createTestUser(String userId, int point) {
        UserCommand.Create command = new UserCommand.Create(userId, "test@example.com", "1990-01-01", Gender.MALE);

        return User.of(command);
    }

    private Product createTestProduct(Long id, String name, BigDecimal price) {
        Product product = mock(Product.class);
        lenient().when(product.getId()).thenReturn(id);
        lenient().when(product.getName()).thenReturn(name);
        lenient().when(product.getPrice()).thenReturn(price);
        return product;
    }
}
