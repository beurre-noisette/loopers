package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

class PointTest {

    @DisplayName("포인트 생성 시")
    @Nested
    class Create {
        @DisplayName("사용자 ID로 포인트를 생성하면 초기 잔액은 0이다.")
        @Test
        void createPoint_withZeroBalance() {
            // arrange
            Long userId = 1L;

            // act
            Point point = Point.create(userId);

            // assert
            assertAll(
                    () -> assertThat(point.getUserId()).isEqualTo(userId),
                    () -> assertThat(point.getBalance()).isEqualTo(BigDecimal.ZERO)
            );
        }

        @DisplayName("사용자 ID가 null 이면 BAD REQUEST 예외가 발생한다.")
        @Test
        void throwBadRequestException_whenUserIdIsNull() {
            // act
            CoreException exception = assertThrows(
                    CoreException.class,
                    () -> Point.create(null)
            );

            // assert
            assertAll(
                    () -> assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST)
            );
        }
    }

    @DisplayName("포인트 충전 시")
    @Nested
    class Charge {
        @DisplayName("양수 금액을 충전하면 잔액이 증가한다.")
        @Test
        void chargePoint_withPositiveAmount() {
            // arrange
            Point point = Point.create(1L);
            BigDecimal chargeAmount = BigDecimal.valueOf(1_000);

            // act
            point.charge(chargeAmount);

            // assert
            assertThat(point.getBalance()).isEqualTo(chargeAmount);
        }

        @DisplayName("여러번 충전하면 누적된다.")
        @Test
        void chargePoint_multipleCharges() {
            // arrange
            Point point = Point.create(1L);

            // act
            point.charge(BigDecimal.valueOf(1_000));
            point.charge(BigDecimal.valueOf(2_000));
            point.charge(BigDecimal.valueOf(3_000));

            // assert
            assertThat(point.getBalance()).isEqualTo(BigDecimal.valueOf(6_000));
        }

        @DisplayName("0 이하의 금액을 충전하면 BAD REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(
                doubles = {
                        0,
                        -1,
                        -100,
                        -1000
                })
        void throwBadRequestException_whenChargeZeroOrNegativeAmount(double amount) {
            // arrange
            Point point = Point.create(1L);

            // act
            CoreException exception = assertThrows(
                    CoreException.class,
                    () -> {
                        point.charge(BigDecimal.valueOf(amount));
                    }
            );

            // assert
            assertAll(
                    () -> assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST)
            );
        }
    }

    @DisplayName("포인트 사용 시")
    @Nested
    class Use {
        @DisplayName("잔액보다 적은 금액을 사용하면 잔액이 감소한다.")
        @Test
        void usePoint_withSufficientBalance() {
            // arrange
            Point point = Point.create(1L);
            point.charge(BigDecimal.valueOf(5_000));

            // act
            point.use(BigDecimal.valueOf(3_000));

            // assert
            assertThat(point.getBalance()).isEqualTo(BigDecimal.valueOf(2_000));
        }

        @DisplayName("잔액과 동일한 금액을 사용하면 잔액이 0이 된다.")
        @Test
        void usePoint_allBalance() {
            // arrange
            Point point = Point.create(1L);
            point.charge(BigDecimal.valueOf(5_000));

            // act
            point.use(BigDecimal.valueOf(5_000));

            // assert
            assertThat(point.getBalance()).isEqualTo(BigDecimal.ZERO);
        }

        @DisplayName("잔액보다 많은 금액을 사용하려하면 NOT ENOUGH 예외가 발생한다.")
        @Test
        void throwNotEnoughException_whenInsufficientBalance() {
            // arrange
            Point point = Point.create(1L);
            point.charge(BigDecimal.valueOf(5_000));

            // act
            CoreException exception = assertThrows(
                    CoreException.class,
                    () -> point.use(BigDecimal.valueOf(6_000))
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_ENOUGH);
        }

        @DisplayName("0이하의 금액을 사용하려 하면 BAD REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(
                doubles = {
                        0,
                        -1
                        -100
                })
        void throwBadRequestException_whenUseZeroOrNegativeAmount(double amount) {
            // arrange
            Point point = Point.create(1L);
            point.charge(BigDecimal.valueOf(5_000));

            // act
            CoreException exception = assertThrows(
                    CoreException.class,
                    () -> point.use(BigDecimal.valueOf(amount))
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

}
