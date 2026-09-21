# Day 17 — User Service: Entity + Security Config

---

## 1. Project Structure

```
user-service/
├── src/main/java/com/ecommerce/user_service/
│   ├── UserServiceApplication.java          @SpringBootApplication + @EnableJpaAuditing
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── User.java                    JPA entity + implements UserDetails
│   │   │   └── Address.java                 @ManyToOne → User
│   │   └── enums/
│   │       └── Role.java                    CUSTOMER, ADMIN, SELLER
│   ├── repository/
│   │   ├── UserRepository.java              JpaRepository + @EntityGraph queries
│   │   └── AddressRepository.java           findByUserId
│   ├── security/
│   │   └── CustomUserDetailsService.java    implements UserDetailsService
│   ├── config/
│   │   └── SecurityConfig.java              SecurityFilterChain + BCrypt + AuthManager
│   └── seeder/
│       └── DataSeeder.java                  @Profile("dev") seed data
└── src/test/java/com/ecommerce/user_service/
    ├── UserServiceApplicationTests.java     context load test
    ├── repository/
    │   └── UserRepositoryTest.java          7 tests — derived queries + @EntityGraph
    └── security/
        └── SecurityConfigTest.java          2 tests — BCrypt encoding
```

---

## 2. JPA Relationships

### @OneToMany — User → Addresses

```java
// User.java — the "one" side
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
private List<Address> addresses = new ArrayList<>();
```

```java
// Address.java — the "many" side (owns the FK column)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

**Key rules:**
- `mappedBy = "user"` → tells JPA that Address.user field owns the FK — avoids duplicate FK column
- `cascade = ALL` → saving/deleting User also saves/deletes its Addresses
- `orphanRemoval = true` → removing an Address from the list deletes it from DB
- `FetchType.LAZY` → addresses are NOT loaded unless explicitly accessed (prevents N+1)

### @ElementCollection — User → Roles

```java
@ElementCollection(fetch = FetchType.EAGER)  // roles always needed for auth
@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
@Enumerated(EnumType.STRING)
@Column(name = "role")
private Set<Role> roles = new HashSet<>();
```

**Why @ElementCollection instead of @ManyToMany:**
- Roles don't need their own entity (no extra fields like `assignedAt`)
- Simpler — stored in a join table `user_roles(user_id, role)` automatically
- Use `@ManyToMany` only when the join table needs its own fields

---

## 3. N+1 Problem and @EntityGraph

**The N+1 problem:**

```
Loading 10 users fires:
  SELECT * FROM users          → 1 query
  SELECT * FROM addresses WHERE user_id = 1   → 1 query per user
  SELECT * FROM addresses WHERE user_id = 2
  ...
  SELECT * FROM addresses WHERE user_id = 10
= 11 queries total (1 + N)
```

**Fix with @EntityGraph — forces a JOIN:**

```java
@EntityGraph(attributePaths = "addresses")  // JOIN fetch addresses
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findByIdWithAddresses(@Param("id") Long id);
```

This generates one query:
```sql
SELECT u.*, a.* FROM users u
LEFT JOIN addresses a ON a.user_id = u.id
WHERE u.id = ?
```

**When to use @EntityGraph vs EAGER loading:**
- EAGER: always loads — even when you don't need it (wastes resources)
- @EntityGraph: loads on demand — only for specific queries that need it

---

## 4. User implements UserDetails

Spring Security's authentication pipeline calls `loadUserByUsername()` which returns a `UserDetails`.
By making `User` implement `UserDetails` directly, no conversion is needed:

```java
public class User implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // "ROLE_" prefix is Spring Security convention for hasRole() checks
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
    }

    @Override public String getPassword()              { return password; }
    @Override public String getUsername()              { return username; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return enabled; }
}
```

---

## 5. CustomUserDetailsService

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // supports login by username OR email
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
```

Spring Security calls this during authentication to load the user, then compares the submitted password against the stored BCrypt hash.

---

## 6. SecurityFilterChain

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)          // stateless API — no CSRF needed
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // no HttpSession
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()              // public
            .requestMatchers("/actuator/health").permitAll()          // public
            .requestMatchers("/api/admin/**").hasRole("ADMIN")        // ADMIN only
            .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
            .anyRequest().authenticated()                             // everything else needs auth
        )
        .authenticationProvider(authenticationProvider());
    return http.build();
}
```

**Why CSRF is disabled:**
- CSRF attacks exploit browser auto-sending session cookies
- Our API uses JWT in the `Authorization` header — browsers don't auto-send headers
- Stateless APIs with JWT don't need CSRF protection

**Authentication vs Authorization:**
- Authentication: who are you? (verify identity via username + password)
- Authorization: what can you do? (check roles/permissions)

---

## 7. BCryptPasswordEncoder

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // cost factor 12 (default is 10)
}
```

**Why BCrypt:**
- Auto-salts every password — same input produces different hashes each time
- Cost factor is adjustable — increase it as hardware gets faster
- Resistant to rainbow table attacks (due to salt)
- `matches(raw, encoded)` handles the salt extraction automatically

```java
String hash1 = encoder.encode("password");  // "$2a$12$abc..."
String hash2 = encoder.encode("password");  // "$2a$12$xyz..." — different!
encoder.matches("password", hash1);  // true
encoder.matches("password", hash2);  // true — both valid
```

---

## 8. DaoAuthenticationProvider — Spring Security 7.x Change

Spring Security 7.x changed the constructor — `UserDetailsService` is now required:

```java
// Spring Security 6.x (old)
DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
provider.setUserDetailsService(userDetailsService);  // setter

// Spring Security 7.x (new — used in Spring Boot 4.1.1)
DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);  // constructor
provider.setPasswordEncoder(passwordEncoder());
```

---

## 9. Testing Notes

**Why @Transactional doesn't work cleanly for repository tests with unique constraints:**

When `@Transactional` is on the test class, each test runs in a transaction that rolls back after the test. But `@BeforeEach` runs inside the same transaction — so the second test's `@BeforeEach` tries to insert the same unique username while the first test's rollback hasn't fully released the unique index lock.

**Fix — explicit `@AfterEach` cleanup:**

```java
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class UserRepositoryTest {

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();  // committed cleanup — no rollback timing issues
    }
}
```

`@DirtiesContext` ensures a fresh Spring context (and fresh H2 DB) for this test class, preventing data leakage from other test classes.

---

## 10. Spring Boot 4.x / Security 7.x Notes

| Topic | Note |
|-------|------|
| `DaoAuthenticationProvider` | Constructor requires `UserDetailsService` — no setter |
| `WebSecurityConfigurerAdapter` | Removed — use `SecurityFilterChain` bean |
| `@EnableWebSecurity` | Still required to activate Spring Security |
| `@EnableMethodSecurity` | Replaces `@EnableGlobalMethodSecurity` — enables `@PreAuthorize` |
| `HttpSecurity.csrf()` | Use `AbstractHttpConfigurer::disable` lambda style |
| `SessionCreationPolicy.STATELESS` | Required for JWT-based stateless APIs |

---

## 11. Test Results

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0

UserRepositoryTest (7 tests):
  ✓ findByEmail_existingEmail_returnsUser
  ✓ findByEmail_unknownEmail_returnsEmpty
  ✓ existsByEmail_returnsCorrectly
  ✓ findByIdWithAddresses_loadsAddressesInOneQuery   (@EntityGraph — no N+1)
  ✓ findByRole_returnsUsersWithThatRole
  ✓ findByRole_seller_returnsOnlySeller
  ✓ findByUsername_returnsCorrectUser

SecurityConfigTest (2 tests):
  ✓ passwordEncoder_encodesAndMatchesCorrectly
  ✓ passwordEncoder_samePasswordProducesDifferentHashes

UserServiceApplicationTests (1 test):
  ✓ contextLoads
```
