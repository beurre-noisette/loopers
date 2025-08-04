package com.loopers.domain.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class OrderItemsConverter implements AttributeConverter<OrderItems, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(OrderItems orderItems) {
        if (orderItems == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(orderItems.getItems());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert OrderItems to JSON", e);
        }
    }

    @Override
    public OrderItems convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            List<OrderItem> items = objectMapper.readValue(dbData, new TypeReference<List<OrderItem>>() {});
            return OrderItems.from(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON to OrderItems", e);
        }
    }
}