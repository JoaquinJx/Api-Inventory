package com.joaquin.inventory.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joaquin.inventory.dto.order.OrderItemRequest;
import com.joaquin.inventory.dto.order.OrderRequest;
import com.joaquin.inventory.entity.Product;
import com.joaquin.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void createOrder_withSameIdempotencyKey_returnsSameOrder() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("Laptop")
                .slug("laptop")
                .price(BigDecimal.valueOf(999.99))
                .stock(20)
                .featured(false)
                .build());

        OrderRequest request = new OrderRequest();
        request.setCustomerName("John Doe");
        request.setCustomerEmail("john@example.com");
        request.setAddress("Main Street 1");
        request.setCity("Madrid");
        request.setPostalCode("28001");

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(product.getId());
        item.setQuantity(2);
        request.setItems(List.of(item));

        String payload = objectMapper.writeValueAsString(request);

        String response1 = mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "idem-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String response2 = mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", "idem-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json1 = objectMapper.readTree(response1);
        JsonNode json2 = objectMapper.readTree(response2);

        org.assertj.core.api.Assertions.assertThat(json1.get("id").asLong())
                .isEqualTo(json2.get("id").asLong());
    }

    @Test
    void myOrders_withoutAuth_returns401StandardError() throws Exception {
        mockMvc.perform(get("/api/orders/my-orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void myOrders_withUserToken_returns200() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/orders/my-orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String registerAndGetToken() throws Exception {
        com.joaquin.inventory.dto.auth.RegisterRequest register = new com.joaquin.inventory.dto.auth.RegisterRequest();
        register.setUsername("order-user");
        register.setEmail("order-user@example.com");
        register.setPassword("Password1!");

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(registerResponse).get("token").asText();
    }
}
