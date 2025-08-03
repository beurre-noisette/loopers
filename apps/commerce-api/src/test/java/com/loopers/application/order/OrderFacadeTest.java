package com.loopers.application.order;

import com.loopers.domain.order.*;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

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

            List<OrderItem> orderItems = List.of(
                    new OrderItem(1L, 2, new BigDecimal("10000")),
                    new OrderItem(2L, 1, new BigDecimal("5000"))
            );

            Order expectedOrder = Order.create(userId, orderItems);

            when(userService.findByUserId(userId)).thenReturn(user);
            when(productService.findProductsForOrder(command.items())).thenReturn(products);
            when(orderService.createOrderItems(command.items(), products)).thenReturn(orderItems);
            when(orderService.calculateTotalAmount(orderItems)).thenReturn(new BigDecimal("25000"));
            when(orderService.createOrder(userId, orderItems)).thenReturn(expectedOrder);

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
            verify(productService).findProductsForOrder(command.items());
            verify(orderService).createOrderItems(command.items(), products);
            verify(orderService).calculateTotalAmount(orderItems);
            verify(productService).validateAndDecreaseStocks(products, command.items());
            verify(userService).usePoint(user, 25000);
            verify(orderService).createOrder(userId, orderItems);
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
            verifyNoInteractions(productService, orderService);
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
            when(productService.findProductsForOrder(command.items()))
                    .thenThrow(new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> orderFacade.createOrder(userId, command));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            verify(userService).findByUserId(userId);
            verify(productService).findProductsForOrder(command.items());
            verifyNoMoreInteractions(productService, orderService);
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
            
            List<OrderItem> orderItems = List.of(new OrderItem(1L, 10, new BigDecimal("10000")));

            when(userService.findByUserId(userId)).thenReturn(user);
            when(productService.findProductsForOrder(command.items())).thenReturn(products);
            when(orderService.createOrderItems(command.items(), products)).thenReturn(orderItems);
            when(orderService.calculateTotalAmount(orderItems)).thenReturn(new BigDecimal("100000"));
            doThrow(new CoreException(ErrorType.INVALID_INPUT_FORMAT, "재고가 부족합니다."))
                    .when(productService).validateAndDecreaseStocks(products, command.items());

            // act & assert
            CoreException exception = assertThrows(CoreException.class,
                    () -> orderFacade.createOrder(userId, command));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");
            
            verify(userService).findByUserId(userId);
            verify(productService).findProductsForOrder(command.items());
            verify(orderService).createOrderItems(command.items(), products);
            verify(orderService).calculateTotalAmount(orderItems);
            verify(productService).validateAndDecreaseStocks(products, command.items());
            verify(userService, never()).usePoint(any(), anyInt());
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
            
            List<OrderItem> orderItems = List.of(new OrderItem(1L, 1, new BigDecimal("10000")));

            when(userService.findByUserId(userId)).thenReturn(user);
            when(productService.findProductsForOrder(command.items())).thenReturn(products);
            when(orderService.createOrderItems(command.items(), products)).thenReturn(orderItems);
            when(orderService.calculateTotalAmount(orderItems)).thenReturn(new BigDecimal("10000"));
            doThrow(new CoreException(ErrorType.INVALID_INPUT_FORMAT, "포인트가 부족합니다."))
                    .when(userService).usePoint(user, 10000);

            // act
            CoreException exception = assertThrows(CoreException.class,
                    () -> orderFacade.createOrder(userId, command));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_FORMAT);
            assertThat(exception.getMessage()).contains("포인트가 부족합니다");
            
            verify(userService).findByUserId(userId);
            verify(productService).findProductsForOrder(command.items());
            verify(orderService).createOrderItems(command.items(), products);
            verify(orderService).calculateTotalAmount(orderItems);
            verify(productService).validateAndDecreaseStocks(products, command.items());
            verify(userService).usePoint(user, 10000);
            verify(orderService, never()).createOrder(any(), any());
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
            
            List<OrderItem> orderItems = List.of(new OrderItem(1L, 1, new BigDecimal("10000")));
            Order expectedOrder = Order.create(userId, orderItems);

            when(userService.findByUserId(userId)).thenReturn(user);
            when(productService.findProductsForOrder(command.items())).thenReturn(products);
            when(orderService.createOrderItems(command.items(), products)).thenReturn(orderItems);
            when(orderService.calculateTotalAmount(orderItems)).thenReturn(new BigDecimal("10000"));
            when(orderService.createOrder(userId, orderItems)).thenReturn(expectedOrder);

            // act
            orderFacade.createOrder(userId, command);

            // assert - 호출 순서 검증
            var inOrder = inOrder(userService, productService, orderService);
            inOrder.verify(userService).findByUserId(userId);
            inOrder.verify(productService).findProductsForOrder(command.items());
            inOrder.verify(orderService).createOrderItems(command.items(), products);
            inOrder.verify(orderService).calculateTotalAmount(orderItems);
            inOrder.verify(productService).validateAndDecreaseStocks(products, command.items());
            inOrder.verify(userService).usePoint(user, 10000);
            inOrder.verify(orderService).createOrder(userId, orderItems);
        }

        @DisplayName("OrderItem이 올바른 정보로 생성된다.")
        @Test
        void createOrder_createsCorrectOrderItems() {
            // arrange
            String userId = "testUser";
            OrderCommand.CreateItem item = new OrderCommand.CreateItem(1L, 3);
            OrderCommand.Create command = new OrderCommand.Create(List.of(item));

            User user = createTestUser(userId, 50000);
            Product product = createTestProduct(1L, "상품1", new BigDecimal("15000"));
            List<Product> products = List.of(product);
            
            List<OrderItem> orderItems = List.of(new OrderItem(1L, 3, new BigDecimal("15000")));
            Order expectedOrder = Order.create(userId, orderItems);
            
            when(userService.findByUserId(userId)).thenReturn(user);
            when(productService.findProductsForOrder(command.items())).thenReturn(products);
            when(orderService.createOrderItems(command.items(), products)).thenReturn(orderItems);
            when(orderService.calculateTotalAmount(orderItems)).thenReturn(new BigDecimal("45000"));
            when(orderService.createOrder(userId, orderItems)).thenReturn(expectedOrder);

            // act
            orderFacade.createOrder(userId, command);

            // assert
            verify(orderService).createOrderItems(command.items(), products);
            verify(orderService).createOrder(userId, orderItems);
        }
    }

    private User createTestUser(String userId, int point) {
        UserCommand.Create command = new UserCommand.Create(userId, "test@example.com", "1990-01-01", Gender.MALE);
        User user = User.of(command);
        
        // 포인트 충전 (테스트용)
        if (point > 0) {
            user.chargePoint(point);
        }
        
        return user;
    }

    private Product createTestProduct(Long id, String name, BigDecimal price) {
        Product product = mock(Product.class);
        lenient().when(product.getId()).thenReturn(id);
        lenient().when(product.getName()).thenReturn(name);
        lenient().when(product.getPrice()).thenReturn(price);
        return product;
    }
}
