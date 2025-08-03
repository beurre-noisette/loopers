package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
class BrandServiceIntegrationTest {
    @Autowired
    private BrandService brandService;

    @MockitoSpyBean
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("브랜드 생성 시")
    @Nested
    class Create {
        @DisplayName("브랜드 정보가 저장된다.")
        @Test
        void saveBrand() {
            // arrange
            String name = "apple";
            String description = "Emotional product";
            BrandCommand.Create command = new BrandCommand.Create(name, description);

            // act
            Brand savedBrand = brandService.save(command);

            // assert
            assertAll(
                    () -> assertThat(savedBrand).isNotNull(),
                    () -> assertThat(savedBrand.getName()).isEqualTo(name),
                    () -> assertThat(savedBrand.getDescription()).isEqualTo(description)

            );

            verify(brandRepository).save(any(Brand.class));
        }

        @DisplayName("이미 존재하는 브랜드 이름이라면 DUPLICATE_VALUE 예외가 발생한다.")
        @Test
        void throwDuplicateValueException_whenProvidedDuplicateBrandName() {
            // arrange
            String brandName = "apple";
            String description = "Emotional product";

            BrandCommand.Create command = new BrandCommand.Create(brandName, description);

            brandService.save(command);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                brandService.save(command);
            });

            // assert
            assertEquals(ErrorType.DUPLICATE_VALUE, exception.getErrorType());
        }
    }

    @DisplayName("브랜드를 조회할 때")
    @Nested
    class GetBrand {
        @DisplayName("브랜드 정보가 반환된다.")
        @Test
        void returnBrand_whenBrandIsExists() {
            // arrange
            BrandCommand.Create command = new BrandCommand.Create("apple", "Emotional product");
            Brand savedBrand = brandService.save(command);

            // act
            Brand foundBrand = brandService.findById(savedBrand.getId());

            // assert
            assertAll(
                    () -> assertThat(savedBrand).isNotNull(),
                    () -> assertThat(foundBrand).isNotNull(),
                    () -> assertThat(foundBrand.getId()).isEqualTo(savedBrand.getId()),
                    () -> assertThat(foundBrand.getName()).isEqualTo(savedBrand.getName()),
                    () -> assertThat(foundBrand.getDescription()).isEqualTo(savedBrand.getDescription())
            );
        }

        @DisplayName("존재하지 않는 브랜드 ID로 조회하면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwNotFoundException_whenBrandIsNotExists() {
            // arrange
            Long nonExistId = 999L;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
               brandService.findById(nonExistId);
            });

            // assert
            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType());
        }
    }
}
