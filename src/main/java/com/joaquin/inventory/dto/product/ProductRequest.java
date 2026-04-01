package com.joaquin.inventory.dto.product;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 120)
    private String slug;

    @Size(max = 180)
    private String shortDescription;

    @Size(max = 500)
    private String description;

    @Size(max = 500)
    private String imageUrl;

    private List<@Size(max = 500) String> imageUrls;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    private Boolean featured;

    private Long categoryId;
}
