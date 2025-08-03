package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrandTest {

    @DisplayName("브랜드를 생성할 때")
    @Nested
    class Create {
        @DisplayName("브랜드 이름과 설명을 제공하면 정상적으로 생성된다.")
        @Test
        void createBrand_whenProvidedCorrectNameAndDescription() {
            // arrange
            String name = "apple";
            String description = "gamsung pangpang";
            BrandCommand.Create command = new BrandCommand.Create(name, description);

            // act
            Brand brand = Brand.of(command);

            // assert
            assertAll(
                    () -> assertThat(brand).isNotNull(),
                    () -> assertThat(brand.getName()).isEqualTo(name),
                    () -> assertThat(brand.getDescription()).isEqualTo(description)
            );
        }

        @DisplayName("이름에 공백이나 null이 제공되면 Bad Request 예외가 발생한다.")
        @ParameterizedTest
        @NullAndEmptySource
        void throwBadRequest_whenProvidedNullOrEmpty(String name) {
            // arrange
            BrandCommand.Create command = new BrandCommand.Create(name, "gamsung pangpang");

            // act
            CoreException exception = assertThrows(CoreException.class, () -> Brand.of(command));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

}
