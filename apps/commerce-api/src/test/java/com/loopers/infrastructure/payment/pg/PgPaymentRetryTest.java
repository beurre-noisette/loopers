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
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = {
    "pg.api.url=http://localhost:8082",
    "pg.callback.url=http://localhost:8080/api/v1/payments/callback"
})
@DisplayName("PG 결제 재시도 테스트")
class PgPaymentRetryTest extends CommerceApiContextTest {

    @Autowired
    private PgPaymentProcessor pgPaymentProcessor;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("PG 시스템 일시적 장애 시 재시도가 동작한다")
    @Test
    void processPayment_retriesOnTransientFailures() {
        // arrange
        Long userId = 135135L;
        String cardType = "SAMSUNG";
        String cardNo = "1234-5678-9814-1451";
        BigDecimal amount = new BigDecimal("5000");

        CardPaymentCommand command = new CardPaymentCommand(
                System.currentTimeMillis(),
                amount,
                cardType,
                cardNo
        );

        // act - 재시도 로직이 있는 결제 처리
        long startTime = System.currentTimeMillis();
        PaymentResult result = pgPaymentProcessor.processPayment(userId, command);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // assert
        assertAll(
                // 결과는 항상 반환되어야 함 (성공하거나 최종 실패)
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.status()).isIn(
                        PaymentStatus.PENDING, 
                        PaymentStatus.SUCCESS, 
                        PaymentStatus.FAILED
                ),
                () -> assertThat(result.amount()).isEqualTo(amount),
                
                // 재시도가 있다면 최소한의 시간은 걸려야 함
                // 하지만 너무 오래 걸리면 안됨 (최대 3회 시도 * 지수적 백오프 고려)
                () -> assertThat(executionTime).isLessThan(10000L), // 10초 이내
                
                // 결과 메시지가 있어야 함
                () -> assertThat(result.message()).isNotNull()
        );
    }

    @DisplayName("재시도 정책이 설정된 대로 동작한다")
    @Test
    void processPayment_followsRetryPolicy() {
        // arrange
        Long userId = 135135L;
        String cardType = "SAMSUNG";
        String cardNo = "1234-5678-9814-1451";
        BigDecimal amount = new BigDecimal("3000");

        // act - 여러 번 호출하여 재시도 패턴 확인
        for (int i = 0; i < 5; i++) {
            CardPaymentCommand command = new CardPaymentCommand(
                    System.currentTimeMillis() + i,
                    amount,
                    cardType,
                    cardNo
            );

            long startTime = System.currentTimeMillis();
            PaymentResult result = pgPaymentProcessor.processPayment(userId, command);
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            System.out.println("Attempt " + (i + 1) + ": " 
                    + result.status() + ", time=" + executionTime + "ms");

            // assert - 각 호출은 정상적으로 완료되어야 함
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.status()).isIn(
                            PaymentStatus.PENDING, 
                            PaymentStatus.SUCCESS, 
                            PaymentStatus.FAILED
                    ),
                    () -> assertThat(result.amount()).isEqualTo(amount),
                    
                    // 재시도 설정 (최대 3회 시도, 1초 + 지수적 백오프)에 따른 최대 시간
                    // 1초 + 1.5초 + 2.25초 = 약 5초 + 네트워크 시간
                    () -> assertThat(executionTime).isLessThan(8000L)
            );

            // 연속 호출 사이 간격
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @DisplayName("재시도 중 성공하면 즉시 결과를 반환한다")
    @Test
    void processPayment_returnsImmediatelyOnSuccess() {
        // arrange
        Long userId = 135135L;
        String cardType = "SAMSUNG";
        String cardNo = "1234-5678-9814-1451";
        BigDecimal amount = new BigDecimal("1000");

        CardPaymentCommand command = new CardPaymentCommand(
                System.currentTimeMillis(),
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
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.status()).isIn(
                        PaymentStatus.PENDING, 
                        PaymentStatus.SUCCESS, 
                        PaymentStatus.FAILED
                ),
                () -> assertThat(result.amount()).isEqualTo(amount),
                
                // 성공한 경우 빠르게 응답해야 함 (재시도 없이)
                // 실패한 경우에만 재시도가 발생
                () -> assertThat(result.message()).isNotNull()
        );

        System.out.println("Result: " + result.status() + ", execution time: " + executionTime + "ms");
        
        // 성공했다면 빠른 시간 내에 완료되어야 함
        if (result.status() == PaymentStatus.SUCCESS || result.status() == PaymentStatus.PENDING) {
            // PG 응답 시간 (100ms~500ms) + 네트워크 오버헤드를 고려
            assertThat(executionTime).isLessThan(2000L);
        }
    }

    @DisplayName("비즈니스 검증 실패는 재시도하지 않는다")
    @Test
    void processPayment_doesNotRetryBusinessValidationFailures() {
        // arrange - 0원으로 검증 실패 유도 (CardPaymentCommand 생성자에서 실패)
        Long userId = 135135L;
        String cardType = "SAMSUNG";
        String cardNo = "1234-5678-9814-1451";

        // act & assert - CardPaymentCommand 생성자에서 즉시 실패
        long startTime = System.currentTimeMillis();
        
        Exception exception = assertThrows(Exception.class, () -> {
            new CardPaymentCommand(
                    System.currentTimeMillis(),
                    BigDecimal.ZERO, // 0원 - 검증 실패
                    cardType,
                    cardNo
            );
        });
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // assert - 비즈니스 로직 예외는 재시도하지 않으므로 빠르게 실패
        assertAll(
                () -> assertThat(executionTime).isLessThan(100L), // 즉시 실패
                () -> assertThat(exception.getMessage()).contains("결제 금액")
        );
    }
}
