package com.joaquin.inventory.service;

import com.joaquin.inventory.dto.product.ProductRequest;
import com.joaquin.inventory.dto.product.ProductResponse;
import com.joaquin.inventory.entity.Category;
import com.joaquin.inventory.entity.Product;
import com.joaquin.inventory.exception.ResourceNotFoundException;
import com.joaquin.inventory.repository.CategoryRepository;
import com.joaquin.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Page<ProductResponse> findAll(String search, Long categoryId, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> specification = Specification.where(null);

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
            ));
        }

        if (categoryId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }

        if (maxPrice != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        return productRepository.findAll(specification, pageable).map(this::toResponse);
    }

    public ProductResponse findById(Long id) {
        return toResponse(getProduct(id));
    }

    public ProductResponse findBySlug(String slug) {
        return toResponse(productRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug)));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Product '" + request.getName() + "' already exists");
        }
        Product product = Product.builder()
                .name(request.getName())
                .slug(generateUniqueSlug(request.getSlug(), request.getName(), null))
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .imageUrls(copyImageUrls(request.getImageUrls()))
                .price(request.getPrice())
                .stock(request.getStock())
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .category(resolveCategory(request.getCategoryId()))
                .build();
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        product.setName(request.getName());
        product.setSlug(generateUniqueSlug(request.getSlug(), request.getName(), product.getId()));
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setImageUrls(copyImageUrls(request.getImageUrls()));
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        product.setCategory(resolveCategory(request.getCategoryId()));
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(getProduct(id));
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .shortDescription(p.getShortDescription())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .imageUrls(copyImageUrls(p.getImageUrls()))
                .price(p.getPrice())
                .stock(p.getStock())
                .featured(Boolean.TRUE.equals(p.getFeatured()))
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private List<String> copyImageUrls(List<String> imageUrls) {
        return imageUrls == null ? new ArrayList<>() : new ArrayList<>(imageUrls);
    }

    private String generateUniqueSlug(String requestedSlug, String name, Long currentProductId) {
        String baseSlug = slugify(requestedSlug != null && !requestedSlug.isBlank() ? requestedSlug : name);
        String candidate = baseSlug;
        int counter = 2;

        while (true) {
            var existing = productRepository.findBySlugIgnoreCase(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(currentProductId)) {
                return candidate;
            }
            candidate = baseSlug + "-" + counter++;
        }
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        return normalized.isBlank() ? "product" : normalized;
    }
}
