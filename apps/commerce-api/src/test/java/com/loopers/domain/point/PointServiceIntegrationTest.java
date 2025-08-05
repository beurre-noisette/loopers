package com.loopers.domain.point;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserCommand;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User testUser;

    @BeforeEach
    void setUp() {
        UserCommand.Create command = new UserCommand.Create(
                "testUser",
                "test@example.com",
                "1990-01-01",
                Gender.MALE);
        testUser = userRepository.save(User.of(command));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트 생성")
    @Nested
    class CreatePointWithInitialAmount {

        @Test
        @DisplayName("초기 금액이 0인 포인트를 생성하면 잔액은 0이고 히스토리는 기록되지 않는다.")
        void createPoint_withZeroAmount_doesNotCreateHistory() {
            // act
            Point point = pointService.createPointWithInitialAmount(
                    testUser.getId(),
                    BigDecimal.ZERO,
                    PointReference.welcomeBonus()
            );

            // assert
            assertThat(point.getUserId()).isEqualTo(testUser.getId());
            assertThat(point.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

            List<PointHistory> histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
            assertThat(histories).isEmpty();
        }

        @Test
        @DisplayName("초기 금액과 함께 포인트를 생성하면 잔액이 설정되고 히스토리가 기록된다.")
        void createPoint_setsBalanceAndCreatesHistory_whenInitialAmountIsGiven() {
            // arrange
            BigDecimal initialAmount = BigDecimal.valueOf(1000);

            // act
            Point point = pointService.createPointWithInitialAmount(
                    testUser.getId(),
                    initialAmount,
                    PointReference.welcomeBonus()
            );

            // assert
            assertThat(point.getBalance()).isEqualByComparingTo(initialAmount);

            List<PointHistory> histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
            assertThat(histories).hasSize(1);
            assertThat(histories.get(0).getAmount()).isEqualByComparingTo(initialAmount);
            assertThat(histories.get(0).getBalanceAfter()).isEqualByComparingTo(initialAmount);
            assertThat(histories.get(0).getType()).isEqualTo(PointTransactionType.CHARGE);
            assertThat(histories.get(0).getReference().getType()).isEqualTo(ReferenceType.WELCOME_BONUS);
        }

        @Test
        @DisplayName("이미 포인트가 존재하는 유저에게 다시 생성하면 CONFLICT 예외가 발생한다.")
        void throwConflictException_whenPointAlreadyExists() {
            // arrange
            pointService.createPointWithInitialAmount(testUser.getId(), BigDecimal.ZERO, PointReference.welcomeBonus());

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                pointService.createPointWithInitialAmount(testUser.getId(), BigDecimal.valueOf(1000), PointReference.welcomeBonus());
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            assertThat(exception.getMessage()).contains("이미 포인트가 존재합니다");
        }
    }

    @DisplayName("포인트 충전")
    @Nested
    class ChargePoint {

        @Test
        @DisplayName("포인트를 충전하면 잔액이 증가하고 히스토리가 기록된다.")
        void increasesBalanceAndCreatesHistory_whenChargingPoint() {
            // arrange
            pointService.createPointWithInitialAmount(testUser.getId(), BigDecimal.ZERO, PointReference.welcomeBonus());
            BigDecimal chargeAmount = BigDecimal.valueOf(5000);

            // act
            pointService.chargePoint(testUser.getId(), chargeAmount, PointReference.userCharge());

            // assert
            Point point = pointService.getPoint(testUser.getId());
            assertThat(point.getBalance()).isEqualByComparingTo(chargeAmount);

            List<PointHistory> histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
            assertThat(histories).hasSize(1);
            assertThat(histories.get(0).getAmount()).isEqualByComparingTo(chargeAmount);
            assertThat(histories.get(0).getBalanceAfter()).isEqualByComparingTo(chargeAmount);
            assertThat(histories.get(0).getType()).isEqualTo(PointTransactionType.CHARGE);
            assertThat(histories.get(0).getReference().getType()).isEqualTo(ReferenceType.USER_CHARGE);
        }

        @Test
        @DisplayName("포인트가 없는 유저가 충전하려고 하면 예외가 발생한다.")
        void throwNotFoundException_whenUserWithoutPointAttemptsCharge() {
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                pointService.chargePoint(testUser.getId(), BigDecimal.valueOf(1000), PointReference.userCharge());
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("포인트 정보를 찾을 수 없습니다");
        }
    }

    @DisplayName("포인트 사용")
    @Nested
    class UsePoint {

        @Test
        @DisplayName("포인트를 사용하면 잔액이 감소하고 히스토리가 기록된다.")
        void decreasesBalanceAndCreatesHistory_whenUsingPoint() {
            // arrange
            pointService.createPointWithInitialAmount(testUser.getId(), BigDecimal.valueOf(10000), PointReference.welcomeBonus());
            BigDecimal useAmount = BigDecimal.valueOf(3000);

            // act
            pointService.usePoint(testUser.getId(), useAmount, PointReference.order(123L));

            // assert
            Point point = pointService.getPoint(testUser.getId());
            assertThat(point.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(7000));

            List<PointHistory> histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
            assertThat(histories).hasSize(2);

            PointHistory useHistory = histories.get(0);
            assertThat(useHistory.getAmount()).isEqualByComparingTo(useAmount.negate());
            assertThat(useHistory.getBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(7000));
            assertThat(useHistory.getType()).isEqualTo(PointTransactionType.USE);
            assertThat(useHistory.getReference().getType()).isEqualTo(ReferenceType.ORDER);
            assertThat(useHistory.getReference().getReferenceId()).isEqualTo(123L);
        }

        @Test
        @DisplayName("포인트가 없는 유저가 사용하려고 하면 NOT FOUND 예외가 발생한다.")
        void throwNotFoundException_whenUsingPointWithoutExistingUserPoint() {
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                pointService.usePoint(testUser.getId(), BigDecimal.valueOf(1000), PointReference.order(null));
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("잔액이 부족하면 NOTENOUGH 예외가 발생한다.")
        void throwNotEnoughException_whenInsufficientBalance() {
            // arrange
            pointService.createPointWithInitialAmount(testUser.getId(), BigDecimal.valueOf(1000), PointReference.welcomeBonus());

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                pointService.usePoint(testUser.getId(), BigDecimal.valueOf(2000), PointReference.order(null));
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_ENOUGH);
        }
    }
}
