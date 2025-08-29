package com.loopers.infrastructure.payment.pg;

import com.loopers.CommerceApiContextTest;
import com.loopers.domain.payment.PaymentResult;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.command.CardPaymentCommand;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestPropertySource(properties = {
    "pg.api.url=http://localhost:8082",
    "pg.callback.url=http://localhost:8080/api/v1/payments/callback"
})
@DisplayName("PG 결제 타임아웃 테스트")
class PgPaymentTimeoutTest extends CommerceApiContextTest {

    @Autowired
    private PgPaymentProcessor pgPaymentProcessor;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("PG 응답 지연 시 타임아웃 설정이 동작한다")
    @Test
    void processPayment_timeout_whenPgResponseIsDelayed() {
        // arrange
        Long userId = 135135L;
        Long orderId = System.currentTimeMillis();
        BigDecimal amount = new BigDecimal("5000");
        String cardType = "SAMSUNG";
        String cardNo = "1234-5678-9814-1451";

        CardPaymentCommand command = new CardPaymentCommand(
                orderId,
                amount,
                cardType,
                cardNo
        );

        // act
        long startTime = System.currentTimeMillis();
        PaymentResult result = pgPaymentProcessor.processPayment(userId, command);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // assert
        assertAll(
                () -> assertThat(result.status()).isIn(
                        PaymentStatus.PENDING, 
                        PaymentStatus.SUCCESS, 
                        PaymentStatus.FAILED
                ),
                () -> assertThat(result.amount()).isEqualTo(amount),
                
                () -> assertThat(executionTime).isLessThan(8000L),
                
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.message()).isNotNull()
        );

        if (result.transactionKey() == null) {
            assertThat(result.message()).containsAnyOf("PG", "장애", "타임아웃");
        }
    }

    @DisplayName("여러 번의 PG 요청에서 각각 타임아웃 처리가 독립적으로 동작한다")
    @Test
    void processPayment_timeout_multipleRequestsHandledIndependently() {
        // arrange
        Long userId = 135135L;
        String cardType = "SAMSUNG";
        String cardNo = "1234-5678-9814-1451";
        BigDecimal amount = new BigDecimal("3000");

        // act - 연속된 3번의 결제 요청으로 타임아웃 동작 확인
        PaymentResult result1 = processWithTimeCheck(userId, amount, cardType, cardNo);
        PaymentResult result2 = processWithTimeCheck(userId, amount, cardType, cardNo);
        PaymentResult result3 = processWithTimeCheck(userId, amount, cardType, cardNo);

        // assert - 각 요청이 독립적으로 타임아웃 처리되어야 함
        assertAll(
                () -> assertThat(result1.status()).isIn(PaymentStatus.PENDING, PaymentStatus.SUCCESS, PaymentStatus.FAILED),
                () -> assertThat(result2.status()).isIn(PaymentStatus.PENDING, PaymentStatus.SUCCESS, PaymentStatus.FAILED),
                () -> assertThat(result3.status()).isIn(PaymentStatus.PENDING, PaymentStatus.SUCCESS, PaymentStatus.FAILED),
                
                () -> assertThat(result1.amount()).isEqualTo(amount),
                () -> assertThat(result2.amount()).isEqualTo(amount),
                () -> assertThat(result3.amount()).isEqualTo(amount)
        );
    }

    private PaymentResult processWithTimeCheck(Long userId, BigDecimal amount, String cardType, String cardNo) {
        long startTime = System.currentTimeMillis();
        
        CardPaymentCommand command = new CardPaymentCommand(
                System.currentTimeMillis(),
                amount,
                cardType,
                cardNo
        );
        
        PaymentResult result = pgPaymentProcessor.processPayment(userId, command);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        assertThat(executionTime).isLessThan(8000L);
        
        return result;
    }
}
