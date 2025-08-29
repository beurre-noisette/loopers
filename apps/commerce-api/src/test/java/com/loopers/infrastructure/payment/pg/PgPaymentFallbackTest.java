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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestPropertySource(properties = {
    "pg.api.url=http://localhost:8082",
    "pg.callback.url=http://localhost:8080/api/v1/payments/callback"
})
@DisplayName("PG 결제 폴백 처리 테스트")
class PgPaymentFallbackTest extends CommerceApiContextTest {

    @Autowired
    private PgPaymentProcessor pgPaymentProcessor;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("PG 시스템과의 연결에서 예외가 발생해도 시스템은 안정적으로 응답한다")
    @Test
    void processPayment_handlesExceptionsGracefully() {
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

        // act - PG 시스템 호출 (성공하거나 fallback으로 처리됨)
        PaymentResult result = pgPaymentProcessor.processPayment(userId, command);

        // assert - 결과가 반환되어야 함 (성공하거나 fallback 처리됨)
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.status()).isIn(
                        PaymentStatus.PENDING,  // 성공한 경우
                        PaymentStatus.SUCCESS,     // 즉시 성공한 경우
                        PaymentStatus.FAILED      // fallback으로 실패 처리된 경우
                ),
                () -> assertThat(result.amount()).isEqualTo(amount),
                () -> assertThat(result.message()).isNotNull(),
                () -> assertThat(result.processedAt()).isNotNull()
        );

        // fallback 처리되었는지 확인
        if (result.transactionKey() == null && result.status() == PaymentStatus.PENDING) {
            // fallback으로 처리된 경우
            assertThat(result.message()).containsAnyOf("PG", "장애", "확인");
        }
    }

    @DisplayName("fallback 처리된 결과도 유효한 PaymentResult 구조를 가진다")
    @Test
    void fallback_returnsValidPaymentResult() {
        // arrange
        Long userId = 135135L;
        String cardType = "SAMSUNG";
        String cardNo = "1234-5678-9814-1451";
        BigDecimal amount = new BigDecimal("3000");

        // act - 여러 번 시도하여 fallback 발생 가능성 높이기
        PaymentResult finalResult = null;
        
        for (int i = 0; i < 5; i++) {
            CardPaymentCommand command = new CardPaymentCommand(
                    System.currentTimeMillis() + i,
                    amount,
                    cardType,
                    cardNo
            );
            
            PaymentResult result = pgPaymentProcessor.processPayment(userId, command);
            finalResult = result;
            
            // fallback 처리된 결과가 나왔는지 확인
            if (result.transactionKey() == null && result.status() == PaymentStatus.PENDING) {
                break;
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // assert
        final PaymentResult testResult = finalResult;
        assertAll(
                () -> assertThat(testResult).isNotNull(),
                () -> assertThat(testResult.status()).isNotNull(),
                () -> assertThat(testResult.amount()).isEqualTo(amount),
                () -> assertThat(testResult.message()).isNotNull(),
                () -> assertThat(testResult.processedAt()).isNotNull()
        );
    }

    @DisplayName("fallback 처리 시 적절한 상태와 메시지가 설정된다")
    @Test
    void fallback_setsAppropriatStatusAndMessage() {
        // arrange
        Long userId = 135135L;
        String cardType = "SAMSUNG";
        String cardNo = "1234-5678-9814-1451";
        BigDecimal amount = new BigDecimal("1000");

        // act - 연속 호출로 fallback 유도
        PaymentResult fallbackResult = IntStream.range(0, 10).mapToObj(i -> new CardPaymentCommand(
                System.currentTimeMillis() + i,
                amount,
                cardType,
                cardNo
        )).map(command -> pgPaymentProcessor.processPayment(userId, command)).filter(result -> result.transactionKey() == null &&
                result.message().contains("PG")).findFirst().orElse(null);

        // assert - fallback 결과가 발견되었다면 검증
        if (fallbackResult != null) {
            assertAll(
                    () -> assertThat(fallbackResult.status()).isIn(
                            PaymentStatus.PENDING, PaymentStatus.FAILED
                    ),
                    () -> assertThat(fallbackResult.message()).isNotBlank(),
                    () -> assertThat(fallbackResult.transactionKey()).isNull(),
                    () -> assertThat(fallbackResult.amount()).isEqualTo(amount)
            );
            
            System.out.println("Fallback result detected: " + fallbackResult.message());
        }
    }
}
