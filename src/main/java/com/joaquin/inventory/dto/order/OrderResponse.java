package com.joaquin.inventory.dto.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private String status;
    private String customerName;
    private String customerEmail;
    private String address;
    private String city;
    private String postalCode;
    private String createdByUsername;
    private Integer itemCount;
    private BigDecimal subtotal;
    private BigDecimal total;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}