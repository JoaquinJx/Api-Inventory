package com.joaquin.inventory.service;

import com.joaquin.inventory.dto.order.OrderRequest;
import com.joaquin.inventory.dto.order.OrderResponse;
import com.joaquin.inventory.dto.order.OrderStatusUpdateRequest;
import com.joaquin.inventory.dto.order.OrderItemResponse;
import com.joaquin.inventory.entity.CustomerOrder;
import com.joaquin.inventory.entity.OrderItem;
import com.joaquin.inventory.entity.Product;
import com.joaquin.inventory.exception.ResourceNotFoundException;
import com.joaquin.inventory.repository.CustomerOrderRepository;
import com.joaquin.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse create(OrderRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = customerOrderRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        Map<Long, Integer> requestedItems = aggregateItems(request);
        List<Product> products = productRepository.findAllByIdInForUpdate(requestedItems.keySet());
        Map<Long, Product> productsById = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        CustomerOrder order = CustomerOrder.builder()
                .orderNumber(generateOrderNumber())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .idempotencyKey(idempotencyKey == null ? null : idempotencyKey.trim())
                .createdByUsername(resolveAuthenticatedUsername())
                .status(CustomerOrder.Status.CONFIRMED)
                .build();

        requestedItems.forEach((productId, quantity) -> {
            Product product = productsById.get(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Product", productId);
            }
            if (product.getStock() < quantity) {
                throw new IllegalArgumentException("Insufficient stock for product '" + product.getName() + "'");
            }

            product.setStock(product.getStock() - quantity);
            order.addItem(OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(quantity)
                    .build());
        });

        CustomerOrder savedOrder = customerOrderRepository.save(order);

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findMyOrders() {
        String username = getRequiredUsername();
        return customerOrderRepository.findAllByCreatedByUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(Pageable pageable) {
        return customerOrderRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        if (!isCurrentUserAdmin()) {
            String username = getRequiredUsername();
            if (order.getCreatedByUsername() == null || !order.getCreatedByUsername().equals(username)) {
                throw new ResourceNotFoundException("Order", id);
            }
        }

        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        order.setStatus(parseStatus(request.getStatus()));
        return toResponse(customerOrderRepository.save(order));
    }

    private Map<Long, Integer> aggregateItems(OrderRequest request) {
        Map<Long, Integer> items = new LinkedHashMap<>();
        request.getItems().forEach(item -> items.merge(item.getProductId(), item.getQuantity(), Integer::sum));
        return items;
    }

    private String resolveAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    private String getRequiredUsername() {
        String username = resolveAuthenticatedUsername();
        if (username == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return username;
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private CustomerOrder.Status parseStatus(String status) {
        try {
            return CustomerOrder.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid order status '" + status + "'");
        }
    }

    private OrderResponse toResponse(CustomerOrder order) {
        BigDecimal subtotal = calculateSubtotal(order);
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .address(order.getAddress())
                .city(order.getCity())
                .postalCode(order.getPostalCode())
                .createdByUsername(order.getCreatedByUsername())
                .itemCount(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum())
                .subtotal(subtotal)
                .total(subtotal)
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .lineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }

    private BigDecimal calculateSubtotal(CustomerOrder order) {
        return order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDateTime.now().format(ORDER_NUMBER_FORMAT);
    }
}