package com.ecommerce.user_service.seeder;

import com.ecommerce.user_service.domain.entity.Address;
import com.ecommerce.user_service.domain.entity.User;
import com.ecommerce.user_service.domain.enums.Role;
import com.ecommerce.user_service.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Profile("dev")  // only runs in dev — not during tests
@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void seed() {
        if (userRepository.count() > 0) return;

        // admin user
        User admin = new User("admin", "admin@ecommerce.com",
                passwordEncoder.encode("admin123"),
                Set.of(Role.ADMIN, Role.CUSTOMER));
        admin.addAddress(new Address("123 Admin St", "New York", "NY", "10001", "USA", admin));
        userRepository.save(admin);

        // customer user
        User customer = new User("john_doe", "john@example.com",
                passwordEncoder.encode("password123"),
                Set.of(Role.CUSTOMER));
        customer.addAddress(new Address("456 Main St", "Los Angeles", "CA", "90001", "USA", customer));
        customer.addAddress(new Address("789 Oak Ave", "San Francisco", "CA", "94102", "USA", customer));
        userRepository.save(customer);

        // seller user
        User seller = new User("seller_jane", "jane@shop.com",
                passwordEncoder.encode("seller123"),
                Set.of(Role.SELLER, Role.CUSTOMER));
        userRepository.save(seller);

        log.info("========== DataSeeder: seeded {} users ==========", userRepository.count());

        // demonstrate @EntityGraph — loads user + addresses in one JOIN query (no N+1)
        userRepository.findByIdWithAddresses(customer.getId())
                .ifPresent(u -> log.info("Customer '{}' has {} address(es)",
                        u.getUsername(), u.getAddresses().size()));

        // demonstrate findByRole
        userRepository.findByRole(Role.CUSTOMER)
                .forEach(u -> log.info("CUSTOMER role: {}", u.getUsername()));
    }
}
