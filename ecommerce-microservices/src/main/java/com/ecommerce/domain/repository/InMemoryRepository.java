package com.ecommerce.domain.repository;

import com.ecommerce.domain.model.BaseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Abstract — subclasses get all CRUD for free, only add domain-specific queries on top.
// HashMap — O(1) average for save/findById/deleteById. Perfect for in-memory lookup by id.
public abstract class InMemoryRepository<T extends BaseEntity<ID>, ID> implements Repository<T, ID> {

    // protected — subclasses can read the store for custom queries (e.g. filter by category)
    protected final Map<ID, T> store = new HashMap<>();

    @Override
    public T save(T entity) {
        store.put(entity.getIdentity(), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        // Optional.ofNullable — wraps null safely; caller uses .orElseThrow() or .ifPresent()
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        // unmodifiableList — callers can iterate but cannot mutate the backing collection
        return Collections.unmodifiableList(new java.util.ArrayList<>(store.values()));
    }

    @Override
    public void deleteById(ID id) {
        store.remove(id);
    }

    @Override
    public boolean existsById(ID id) {
        return store.containsKey(id);
    }

    @Override
    public long count() {
        return store.size();
    }
}
