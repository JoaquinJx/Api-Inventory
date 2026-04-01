package com.joaquin.inventory.service;

import com.joaquin.inventory.dto.product.ProductRequest;
import com.joaquin.inventory.entity.Product;
import com.joaquin.inventory.exception.ResourceNotFoundException;
import com.joaquin.inventory.repository.CategoryRepository;
import com.joaquin.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void findAll_returnsEmptyPage() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = productService.findAll(null, null, null, Pageable.unpaged());

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    @Test
    void create_throwsWhenDuplicateName() {
        when(productRepository.existsByNameIgnoreCase("Laptop")).thenReturn(true);

        var request = new ProductRequest();
        request.setName("Laptop");
        request.setPrice(BigDecimal.TEN);
        request.setStock(5);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void create_savesAndReturnsProduct() {
        when(productRepository.existsByNameIgnoreCase(any())).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            return Product.builder()
                    .id(1L)
                    .name(p.getName())
                    .slug(p.getSlug())
                    .price(p.getPrice())
                    .stock(p.getStock())
                    .build();
        });
        when(productRepository.findBySlugIgnoreCase(any())).thenReturn(Optional.empty());

        var request = new ProductRequest();
        request.setName("Monitor");
        request.setPrice(BigDecimal.valueOf(299.99));
        request.setStock(10);

        var result = productService.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Monitor");
        assertThat(result.getSlug()).isEqualTo("monitor");
        assertThat(result.getStock()).isEqualTo(10);
    }

    @Test
    void delete_throwsWhenNotFound() {
        when(productRepository.findById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(55L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
