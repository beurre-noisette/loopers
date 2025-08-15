package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Order API", description = "주문 관련 API")
public interface OrderV1ApiSpec {

    @Operation(
        summary = "주문 생성", 
        description = "상품을 주문합니다. 포인트 할인과 쿠폰을 선택적으로 사용할 수 있습니다."
    )
    ApiResponse<OrderV1Dto.OrderCreateResponse> createOrder(
        @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-USER-ID") String userId,
        @Parameter(description = "주문 정보", required = true) @RequestBody OrderV1Dto.OrderCreateRequest request
    );

}
