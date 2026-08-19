package com.ecommerce.domain.repository;

import com.ecommerce.domain.model.BaseEntity;

import java.util.List;
import java.util.Optional;

// T must extend BaseEntity<ID> — ensures every entity has an identity field we can key on
// ID is the type of the primary key (Long, String, UUID, etc.)
public interface Repository<T extends BaseEntity<ID>, ID> {

    T save(T entity);

    // Optional — forces caller to handle "not found" instead of getting null back
    Optional<T> findById(ID id);

    List<T> findAll();

    void deleteById(ID id);

    boolean existsById(ID id);

    long count();
}
