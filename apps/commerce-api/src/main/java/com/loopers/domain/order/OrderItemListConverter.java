package com.loopers.domain.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Converter
public class OrderItemListConverter implements AttributeConverter<List<OrderItem>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(orderItems);
        } catch (JsonProcessingException e) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, 
                "OrderItem 리스트를 JSON으로 변환할 수 없습니다: " + e.getMessage());
        }
    }

    @Override
    public List<OrderItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(dbData, new TypeReference<List<OrderItem>>() {});
        } catch (IOException e) {
            throw new CoreException(ErrorType.INVALID_INPUT_FORMAT, 
                "JSON을 OrderItem 리스트로 변환할 수 없습니다: " + e.getMessage());
        }
    }
}