package com.joaquin.inventory.dto.product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private String imageUrl;
    private List<String> imageUrls;
    private BigDecimal price;
    private Integer stock;
    private Boolean featured;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
