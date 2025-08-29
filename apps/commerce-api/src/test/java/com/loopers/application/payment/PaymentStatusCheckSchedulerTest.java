package com.loopers.application.payment;

import com.loopers.CommerceApiContextTest;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.payment.pg.PgClient;
import com.loopers.infrastructure.payment.pg.PgPaymentDto;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = {
    "pg.api.url=http://localhost:8082",
    "pg.callback.url=http://localhost:8080/api/v1/payments/callback"
})
@DisplayName("결제 상태 확인 스케줄러 테스트")
class PaymentStatusCheckSchedulerTest extends CommerceApiContextTest {

    @Autowired
    private PaymentStatusCheckScheduler paymentStatusCheckScheduler;

    @MockitoBean
    private PgClient pgClient;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean  
    private PaymentFacade paymentFacade;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("PENDING 상태의 결제가 있으면 PG 상태를 확인한다")
    @Test
    void checkPendingPayments_callsPgWhenPendingPaymentsExist() {
        // arrange
        Long orderId = 12345L;
        
        Payment pendingPayment = Payment.create(
                orderId,
                PaymentMethod.CARD,
                new BigDecimal("10000"),
                "20250822:TR:test123",
                PaymentStatus.PENDING
        );

        when(paymentService.findPendingPaymentsBetween(any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(List.of(pendingPayment));

        PgPaymentDto.TransactionDetailResponse pgResponse =
                new PgPaymentDto.TransactionDetailResponse(
                        "20250822:TR:test123",
                        "12345",
                        PgPaymentDto.CardType.SAMSUNG,
                        "1234-5678-9012-3456",
                        10000L,
                        PgPaymentDto.TransactionStatus.SUCCESS,
                        "결제 성공"
                );

        ApiResponse<PgPaymentDto.TransactionDetailResponse> apiResponse = 
                new ApiResponse<>(ApiResponse.Metadata.success(), pgResponse);

        when(pgClient.getTransaction(anyString(), anyString()))
                .thenReturn(apiResponse);

        // act
        paymentStatusCheckScheduler.checkPendingPayments();

        // assert
        verify(pgClient, times(1)).getTransaction("looCommerce", "20250822:TR:test123");
    }

    @DisplayName("PENDING 상태의 결제가 없으면 PG를 호출하지 않는다")
    @Test
    void checkPendingPayments_ignoresWhenNoPendingPayments() {
        // arrange
        when(paymentService.findPendingPaymentsBetween(any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(List.of());

        // act
        paymentStatusCheckScheduler.checkPendingPayments();

        // assert
        verify(pgClient, never()).getTransaction(anyString(), anyString());
    }

    @DisplayName("PG 호출 실패 시에도 스케줄러는 예외 없이 완료된다")
    @Test
    void checkPendingPayments_handlesExceptionsGracefully() {
        // arrange
        Long orderId = 12345L;
        
        Payment pendingPayment = Payment.create(
                orderId,
                PaymentMethod.CARD,
                new BigDecimal("10000"),
                "20250822:TR:test123",
                PaymentStatus.PENDING
        );

        when(paymentService.findPendingPaymentsBetween(any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(List.of(pendingPayment));

        when(pgClient.getTransaction(anyString(), anyString()))
                .thenThrow(new RuntimeException("PG 서버 연결 실패"));

        // act & assert
        assertAll(
                () -> paymentStatusCheckScheduler.checkPendingPayments(),
                () -> verify(pgClient, times(1)).getTransaction("looCommerce", "20250822:TR:test123")
        );
    }


    @DisplayName("여러개의 Pending 결제가 있으면 모두 확인한다")
    @Test
    void checkPendingPayments_checksAllPendingPayments() {
        // arrange
        
        Payment payment1 = Payment.create(
                100L, PaymentMethod.CARD, new BigDecimal("10000"),
                "20250822:TR:test1", PaymentStatus.PENDING
        );
        
        Payment payment2 = Payment.create(
                200L, PaymentMethod.CARD, new BigDecimal("20000"),
                "20250822:TR:test2", PaymentStatus.PENDING
        );

        when(paymentService.findPendingPaymentsBetween(any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(List.of(payment1, payment2));

        PgPaymentDto.TransactionDetailResponse pgResponse1 =
                new PgPaymentDto.TransactionDetailResponse(
                        "20250822:TR:test1", "100",
                        PgPaymentDto.CardType.SAMSUNG, "1234-5678-9012-3456", 10000L,
                        PgPaymentDto.TransactionStatus.SUCCESS, "결제 성공"
                );
        
        PgPaymentDto.TransactionDetailResponse pgResponse2 = 
                new PgPaymentDto.TransactionDetailResponse(
                        "20250822:TR:test2", "200",
                        PgPaymentDto.CardType.SAMSUNG, "1234-5678-9012-3456", 20000L,
                        PgPaymentDto.TransactionStatus.FAILED, "결제 실패"
                );

        when(pgClient.getTransaction("looCommerce", "20250822:TR:test1"))
                .thenReturn(new ApiResponse<>(ApiResponse.Metadata.success(), pgResponse1));
        when(pgClient.getTransaction("looCommerce", "20250822:TR:test2"))
                .thenReturn(new ApiResponse<>(ApiResponse.Metadata.success(), pgResponse2));

        // act
        paymentStatusCheckScheduler.checkPendingPayments();

        // assert
        assertAll(
                () -> verify(pgClient, times(1)).getTransaction("looCommerce", "20250822:TR:test1"),
                () -> verify(pgClient, times(1)).getTransaction("looCommerce", "20250822:TR:test2")
        );
    }

    @DisplayName("스케줄러는 PG 연동 중 예외 발생 시 로깅하고 계속 진행한다")
    @Test
    void checkPendingPayments_continuesOnPgError() {
        // arrange
        Long orderId1 = 100L;
        Long orderId2 = 200L;
        
        Payment payment1 = Payment.create(orderId1, PaymentMethod.CARD, 
                new BigDecimal("1000"), "tx1", PaymentStatus.PENDING);
        Payment payment2 = Payment.create(orderId2, PaymentMethod.CARD, 
                new BigDecimal("2000"), "tx2", PaymentStatus.PENDING);

        when(paymentService.findPendingPaymentsBetween(any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(List.of(payment1, payment2));

        when(pgClient.getTransaction("looCommerce", "tx1"))
                .thenThrow(new RuntimeException("PG 서버 연결 실패"));
        when(pgClient.getTransaction("looCommerce", "tx2"))
                .thenReturn(new ApiResponse<>(ApiResponse.Metadata.success(), 
                        new PgPaymentDto.TransactionDetailResponse("tx2", "200",
                                PgPaymentDto.CardType.SAMSUNG, "1234-5678-9012-3456", 2000L,
                                PgPaymentDto.TransactionStatus.SUCCESS, "성공")));

        // act & assert
        assertAll(
                () -> paymentStatusCheckScheduler.checkPendingPayments(),
                () -> verify(pgClient, times(1)).getTransaction("looCommerce", "tx1"),
                () -> verify(pgClient, times(1)).getTransaction("looCommerce", "tx2")
        );
    }
}
