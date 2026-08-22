package com.ecommerce.domain.repository;

import com.ecommerce.domain.enums.Role;
import com.ecommerce.domain.exception.DuplicateEmailException;
import com.ecommerce.domain.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class UserRepository extends InMemoryRepository<User, Long> {

    // HashSet — O(1) email uniqueness check.
    // Cheaper than scanning all users in the store every time we save.
    private final Set<String> emailIndex = new HashSet<>();

    @Override
    public User save(User user) {
        String email = user.getEmail().toLowerCase();

        // if updating an existing user, allow same email to pass through
        boolean isUpdate = existsById(user.getIdentity());
        if (isUpdate) {
            // get the old email — if it changed, check the new one isn't taken
            String oldEmail = findById(user.getIdentity()).map(u -> u.getEmail().toLowerCase()).orElse("");
            if (!oldEmail.equals(email) && emailIndex.contains(email)) {
                throw new DuplicateEmailException(user.getEmail());
            }
            emailIndex.remove(oldEmail); // remove old email before re-adding
        } else {
            if (emailIndex.contains(email)) {
                throw new DuplicateEmailException(user.getEmail());
            }
        }

        emailIndex.add(email);
        return super.save(user);
    }

    @Override
    public void deleteById(Long id) {
        // keep emailIndex in sync when a user is deleted
        findById(id).ifPresent(u -> emailIndex.remove(u.getEmail().toLowerCase()));
        super.deleteById(id);
    }

    // Optional — caller must handle the "not found" case explicitly
    public Optional<User> findByEmail(String email) {
        String lower = email.toLowerCase();
        for (User u : store.values()) {
            if (u.getEmail().toLowerCase().equals(lower)) return Optional.of(u);
        }
        return Optional.empty();
    }

    public List<User> findByRole(Role role) {
        List<User> result = new ArrayList<>();
        for (User u : store.values()) {
            if (u.getRole() == role) result.add(u);
        }
        return Collections.unmodifiableList(result);
    }
}
