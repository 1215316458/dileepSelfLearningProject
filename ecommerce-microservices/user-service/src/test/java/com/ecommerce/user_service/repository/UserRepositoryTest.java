package com.ecommerce.user_service.repository;

import com.ecommerce.user_service.domain.entity.Address;
import com.ecommerce.user_service.domain.entity.User;
import com.ecommerce.user_service.domain.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class UserRepositoryTest {

    @Autowired UserRepository userRepository;

    User customer;

    @AfterEach
    void tearDown() {
        // explicit cleanup — no @Transactional rollback, so we delete manually
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        customer = new User("john_doe", "john@example.com", "hashed_pw", Set.of(Role.CUSTOMER));
        customer.addAddress(new Address("123 Main St", "NYC", "NY", "10001", "USA", customer));
        customer.addAddress(new Address("456 Oak Ave", "LA",  "CA", "90001", "USA", customer));
        userRepository.save(customer);

        User admin = new User("admin", "admin@example.com", "hashed_pw", Set.of(Role.ADMIN, Role.CUSTOMER));
        userRepository.save(admin);

        User seller = new User("seller", "seller@example.com", "hashed_pw", Set.of(Role.SELLER));
        userRepository.save(seller);
    }

    @Test
    void findByEmail_existingEmail_returnsUser() {
        Optional<User> result = userRepository.findByEmail("john@example.com");
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("john_doe");
    }

    @Test
    void findByEmail_unknownEmail_returnsEmpty() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_returnsCorrectly() {
        assertThat(userRepository.existsByEmail("john@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("ghost@example.com")).isFalse();
    }

    @Test
    void findByIdWithAddresses_loadsAddressesInOneQuery() {
        // @EntityGraph — should load addresses without N+1
        Optional<User> result = userRepository.findByIdWithAddresses(customer.getId());
        assertThat(result).isPresent();
        // addresses are loaded — no LazyInitializationException
        assertThat(result.get().getAddresses()).hasSize(2);
    }

    @Test
    void findByRole_returnsUsersWithThatRole() {
        // admin also has CUSTOMER role — so 2 users have CUSTOMER
        List<User> customers = userRepository.findByRole(Role.CUSTOMER);
        assertThat(customers).hasSize(2);
        assertThat(customers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("john_doe", "admin");
    }

    @Test
    void findByRole_seller_returnsOnlySeller() {
        List<User> sellers = userRepository.findByRole(Role.SELLER);
        assertThat(sellers).hasSize(1);
        assertThat(sellers.get(0).getUsername()).isEqualTo("seller");
    }

    @Test
    void findByUsername_returnsCorrectUser() {
        Optional<User> result = userRepository.findByUsername("admin");
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("admin@example.com");
    }
}
