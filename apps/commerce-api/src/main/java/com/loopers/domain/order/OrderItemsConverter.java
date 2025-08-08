package com.loopers.domain.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class OrderItemsConverter implements AttributeConverter<OrderItems, String> {

    private final ObjectMapper objectMapper;
    
    public OrderItemsConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String convertToDatabaseColumn(OrderItems orderItems) {
        if (orderItems == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(orderItems.getItems());
        } catch (JsonProcessingException e) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "주문 항목을 JSON으로 변환하는 중 오류가 발생했습니다.");
        }
    }

    @Override
    public OrderItems convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            List<OrderItem> items = objectMapper.readValue(dbData, new TypeReference<>() {});
            return OrderItems.from(items);
        } catch (JsonProcessingException e) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "JSON을 주문 항목으로 변환하는 중 오류가 발생했습니다.");
        }
    }
}
