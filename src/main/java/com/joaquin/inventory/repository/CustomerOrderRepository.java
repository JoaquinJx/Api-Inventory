package com.joaquin.inventory.repository;

import com.joaquin.inventory.entity.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

	Optional<CustomerOrder> findByIdempotencyKey(String idempotencyKey);

	@Override
	@EntityGraph(attributePaths = {"items", "items.product"})
	Optional<CustomerOrder> findById(Long id);

	@EntityGraph(attributePaths = {"items", "items.product"})
	List<CustomerOrder> findAllByCreatedByUsernameOrderByCreatedAtDesc(String createdByUsername);

	@EntityGraph(attributePaths = {"items", "items.product"})
	Page<CustomerOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
}