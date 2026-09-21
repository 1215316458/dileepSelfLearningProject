package com.ecommerce.user_service.repository;

import com.ecommerce.user_service.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    // derived query — find all addresses for a given user
    List<Address> findByUserId(Long userId);
}
