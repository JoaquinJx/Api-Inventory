package com.joaquin.inventory.service;

import com.joaquin.inventory.dto.order.OrderItemRequest;
import com.joaquin.inventory.dto.order.OrderRequest;
import com.joaquin.inventory.dto.order.OrderStatusUpdateRequest;
import com.joaquin.inventory.entity.CustomerOrder;
import com.joaquin.inventory.entity.OrderItem;
import com.joaquin.inventory.entity.Product;
import com.joaquin.inventory.exception.ResourceNotFoundException;
import com.joaquin.inventory.repository.CustomerOrderRepository;
import com.joaquin.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void create_savesOrderAndReturnsResponse() {
        Product product = Product.builder()
                .id(1L)
                .name("Monitor")
                .price(BigDecimal.valueOf(299.99))
                .stock(10)
                .build();

        when(productRepository.findAllByIdInForUpdate(any(java.util.Set.class))).thenReturn(List.of(product));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setId(5L);
            return order;
        });

        OrderRequest request = buildRequest(1L, 2);

        var response = orderService.create(request, null);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getOrderNumber()).startsWith("ORD-");
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(product.getStock()).isEqualTo(8);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void create_throwsWhenProductDoesNotExist() {
        when(productRepository.findAllByIdInForUpdate(any(java.util.Set.class))).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.create(buildRequest(99L, 1), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    @Test
    void create_throwsWhenStockIsInsufficient() {
        Product product = Product.builder()
                .id(1L)
                .name("Monitor")
                .price(BigDecimal.valueOf(299.99))
                .stock(1)
                .build();

        when(productRepository.findAllByIdInForUpdate(any(java.util.Set.class))).thenReturn(List.of(product));

        assertThatThrownBy(() -> orderService.create(buildRequest(1L, 2), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

        @Test
        void create_returnsExistingOrderForIdempotencyKey() {
        CustomerOrder existing = CustomerOrder.builder()
            .id(9L)
            .orderNumber("ORD-EXISTING")
            .customerName("Ada Lovelace")
            .customerEmail("ada@example.com")
            .address("123 Main St")
            .city("Madrid")
            .postalCode("28001")
            .status(CustomerOrder.Status.CONFIRMED)
            .items(new ArrayList<>())
            .build();

        when(customerOrderRepository.findByIdempotencyKey("idem-1"))
            .thenReturn(java.util.Optional.of(existing));

        var response = orderService.create(buildRequest(1L, 1), "idem-1");

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getOrderNumber()).isEqualTo("ORD-EXISTING");
        }

    @Test
    void updateStatus_updatesExistingOrder() {
        CustomerOrder order = CustomerOrder.builder()
                .id(3L)
                .orderNumber("ORD-123")
                .customerName("Ada Lovelace")
                .customerEmail("ada@example.com")
                .address("123 Main St")
                .city("Madrid")
                .postalCode("28001")
                .status(CustomerOrder.Status.CONFIRMED)
                .items(new ArrayList<>())
                .build();

        Product product = Product.builder().id(1L).name("Monitor").price(BigDecimal.TEN).build();
        order.addItem(OrderItem.builder().product(product).productName("Monitor").unitPrice(BigDecimal.TEN).quantity(1).build());

        when(customerOrderRepository.findById(3L)).thenReturn(java.util.Optional.of(order));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setStatus("shipped");

        var response = orderService.updateStatus(3L, request);

        assertThat(response.getStatus()).isEqualTo("SHIPPED");
    }

    private OrderRequest buildRequest(Long productId, Integer quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);

        OrderRequest request = new OrderRequest();
        request.setCustomerName("Ada Lovelace");
        request.setCustomerEmail("ada@example.com");
        request.setAddress("123 Main St");
        request.setCity("Madrid");
        request.setPostalCode("28001");
        request.setItems(List.of(item));
        return request;
    }
}