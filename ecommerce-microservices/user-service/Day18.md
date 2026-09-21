# Day 18 — JWT + Auth Endpoints

## What was built

| File | Purpose |
|---|---|
| `security/JwtService.java` | Generate + validate access and refresh tokens |
| `security/JwtAuthenticationFilter.java` | `OncePerRequestFilter` — reads JWT from every request |
| `config/SecurityConfig.java` | Updated — wires JWT filter + `AuthenticationEntryPoint` |
| `config/WebConfig.java` | Registers `LoginRateLimitInterceptor` |
| `interceptor/LoginRateLimitInterceptor.java` | Brute-force protection on `/api/auth/login` |
| `exception/GlobalExceptionHandler.java` | Maps exceptions → HTTP status codes |
| `dto/` (5 records) | `RegisterRequest`, `AuthRequest`, `AuthResponse`, `UpdateProfileRequest`, `UserResponse` |
| `service/UserService.java` | register, login, refresh, profile CRUD |
| `controller/AuthController.java` | POST `/register`, `/login`, `/refresh` |
| `controller/UserController.java` | GET/PUT `/profile`, admin list + delete |
| `test/security/JwtServiceTest.java` | 5 unit tests |
| `test/controller/AuthControllerTest.java` | 7 integration tests |

**Test results: 22/22 PASS**

---

## 1. Why JWT? (Stateless Authentication)

### The session-based problem
Traditional web apps store a session on the server:
```
Client logs in → Server creates session (sessionId=abc123) → Stores in memory/Redis
Client sends Cookie: JSESSIONID=abc123 on every request
Server looks up abc123 → finds user → authorizes
```

Problems at scale:
- Every server instance needs access to the session store (sticky sessions or shared Redis)
- Horizontal scaling is harder — server must be stateful
- CSRF attacks exploit the browser auto-sending cookies

### JWT solves this
```
Client logs in → Server creates a signed token containing the user's identity + roles
Client sends Authorization: Bearer <token> on every request
Server verifies the signature — no DB lookup needed for auth
```

The server is now **stateless** — any instance can verify any token because they all share the same secret key. This is why `SessionCreationPolicy.STATELESS` is set in `SecurityConfig`.

---

## 2. JWT Structure

A JWT is three Base64URL-encoded JSON objects joined by dots:

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIiwicm9sZXMiOlsiUk9MRV9DVVNUT01FUiJdfQ.xyz
      HEADER                              PAYLOAD                                  SIGNATURE
```

### Header
```json
{ "alg": "HS256", "typ": "JWT" }
```
`HS256` = HMAC-SHA256. The algorithm used to sign the token.

### Payload (Claims)
```json
{
  "sub": "john",
  "roles": ["ROLE_CUSTOMER"],
  "type": "access",
  "iat": 1700000000,
  "exp": 1700000900
}
```

Standard claims:
- `sub` — subject (who the token is about)
- `iat` — issued at (Unix timestamp)
- `exp` — expiration (Unix timestamp)

Custom claims we added:
- `roles` — list of `ROLE_X` strings (avoids a DB lookup on every request)
- `type` — `"access"` or `"refresh"` (prevents using a refresh token as an access token)

### Signature
```
HMAC-SHA256(
  base64url(header) + "." + base64url(payload),
  secret_key
)
```

The signature is what makes JWT tamper-proof. If an attacker changes `"roles":["ROLE_CUSTOMER"]` to `"roles":["ROLE_ADMIN"]`, the signature won't match and `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` throws `SignatureException`.

**Important**: JWT payload is Base64-encoded, NOT encrypted. Anyone can decode it. Never put passwords or sensitive data in JWT claims.

---

## 3. Access Token vs Refresh Token

| | Access Token | Refresh Token |
|---|---|---|
| TTL | 15 minutes | 7 days |
| Claims | username + roles + `type=access` | username + `type=refresh` |
| Sent in | `Authorization: Bearer <token>` | `Refresh-Token: <token>` header |
| Purpose | Authorize API calls | Get a new access token |

### Why two tokens?

**Problem with a single long-lived token**: If stolen, the attacker has access for days/weeks.

**Problem with a single short-lived token**: User has to log in every 15 minutes.

**Solution — two tokens**:
- Access token is short-lived (15 min). If stolen, damage window is small.
- Refresh token is long-lived (7 days). Used only to get a new access token, not to call APIs.
- Refresh token can be revoked server-side (Day 27: store valid refresh tokens in Redis; on logout, delete from Redis).

### The refresh flow
```
1. Client: POST /api/auth/login → receives accessToken + refreshToken
2. Client uses accessToken for API calls
3. accessToken expires after 15 min
4. Client: POST /api/auth/refresh (Refresh-Token: <refreshToken>) → receives new accessToken
5. If refreshToken also expires → user must log in again
```

### Preventing refresh token misuse
In `JwtService.isRefreshToken()`:
```java
return "refresh".equals(extractClaim(token, c -> c.get("type", String.class)));
```
In `UserService.refresh()`:
```java
if (!jwtService.isRefreshToken(refreshToken))
    throw new IllegalArgumentException("Not a refresh token");
```
This prevents someone from using an access token as a refresh token.

---

## 4. jjwt 0.12.x API

jjwt had a major breaking API change between 0.11.x and 0.12.x.

### Building a token (0.12.x)
```java
Jwts.builder()
    .subject("john")                          // sets "sub" claim
    .claim("roles", List.of("ROLE_CUSTOMER")) // custom claim
    .claim("type", "access")
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + 900_000))
    .signWith(signingKey)                     // signs with HS256 (inferred from key type)
    .compact();                               // produces the "header.payload.signature" string
```

### Parsing a token (0.12.x)
```java
Jwts.parser()
    .verifyWith(signingKey)   // sets the key used to verify the signature
    .build()
    .parseSignedClaims(token) // throws if signature invalid, token expired, or malformed
    .getPayload();            // returns Claims object
```

Exceptions thrown by `parseSignedClaims`:
- `ExpiredJwtException` — token past its `exp` claim
- `SignatureException` — signature doesn't match (tampered token)
- `MalformedJwtException` — not a valid JWT format
- `UnsupportedJwtException` — wrong token type

### Key generation
```java
// Keys.hmacShaKeyFor requires at least 256 bits (32 bytes) for HS256
SecretKey key = Keys.hmacShaKeyFor(hexToBytes(secret));
```
The secret in `application.yml` is a 64-character hex string = 32 bytes = 256 bits. This is the minimum for HS256.

---

## 5. JwtAuthenticationFilter — OncePerRequestFilter

### What it does
Every HTTP request to a protected endpoint goes through this filter:

```
Request → JwtAuthenticationFilter → SecurityFilterChain authorization check → Controller
```

```java
protected void doFilterInternal(request, response, chain) {
    // 1. Read "Authorization: Bearer <token>" header
    String token = authHeader.substring(7);

    // 2. Extract username from token claims (verifies signature + expiry)
    String username = jwtService.extractUsername(token);

    // 3. Only authenticate if SecurityContext is empty (not already authenticated)
    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails user = userDetailsService.loadUserByUsername(username);

        if (jwtService.isTokenValid(token, user)) {
            // 4. Create authentication object — null credentials (JWT is the credential)
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            // 5. Store in SecurityContext — downstream code can read this
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }

    chain.doFilter(request, response); // always continue the chain
}
```

### Why OncePerRequestFilter?
Servlet filters can be invoked multiple times per request in forward/include dispatch scenarios (e.g., error pages, `RequestDispatcher.forward()`). `OncePerRequestFilter` tracks whether it has already run for this request using a request attribute, and skips if so.

### Filter placement in the chain
```java
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
```

Spring Security's filter chain order (simplified):
```
... → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter → FilterSecurityInterceptor
```

JWT filter runs first, populates `SecurityContextHolder`. When `UsernamePasswordAuthenticationFilter` runs next, it sees the context is already populated and skips. `FilterSecurityInterceptor` then checks the populated context against the authorization rules.

### Why not check the DB on every request?
We do call `userDetailsService.loadUserByUsername(username)` — this hits the DB. This is intentional: it lets us check if the user is still enabled/not-locked. In high-traffic systems, this can be cached (Day 27: Redis cache for UserDetails with short TTL).

---

## 6. SecurityFilterChain — Updated for JWT

```java
http
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
    )
    .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
    .authenticationProvider(authenticationProvider())
    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

### AuthenticationEntryPoint
Spring Security 7.x returns **403 Forbidden** by default when a request has no credentials. The correct HTTP status for "you need to authenticate" is **401 Unauthorized**.

- **401** — you are not authenticated (no credentials or invalid credentials)
- **403** — you are authenticated but not authorized (wrong role)

`AuthenticationEntryPoint` fires when an `AuthenticationException` is thrown (unauthenticated request hits a protected resource). `AccessDeniedHandler` fires when an `AccessDeniedException` is thrown (authenticated but wrong role → 403).

---

## 7. @PreAuthorize — Method-Level Security

```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<List<UserResponse>> getAllUsers() { ... }
```

### How it works
`@EnableMethodSecurity` on `SecurityConfig` activates Spring AOP proxying for security annotations. Every bean with `@PreAuthorize` is wrapped in a proxy:

```
Caller → AOP Proxy (checks @PreAuthorize expression) → Real method
```

The expression `hasRole('ADMIN')` is evaluated against the `Authentication` object in `SecurityContextHolder`. Spring automatically prepends `ROLE_` — so `hasRole('ADMIN')` checks for `ROLE_ADMIN` in the authorities.

### SpEL expressions available
```java
@PreAuthorize("hasRole('ADMIN')")                    // single role
@PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")       // any of these roles
@PreAuthorize("isAuthenticated()")                   // just needs to be logged in
@PreAuthorize("#username == authentication.name")    // method param matches logged-in user
@PostAuthorize("returnObject.username == authentication.name") // check return value
```

### Defense in depth — two layers
Admin endpoints are protected at two levels:
1. **URL level** (`SecurityFilterChain`): `.requestMatchers("/api/admin/**").hasRole("ADMIN")` — blocks at the filter chain before the request reaches the controller
2. **Method level** (`@PreAuthorize`): blocks even if someone misconfigures the URL rules

---

## 8. HandlerInterceptor vs Filter

Both intercept requests, but at different layers of the stack:

```
HTTP Request
    ↓
[Servlet Container]
    ↓
Filter (e.g., JwtAuthenticationFilter)   ← runs here
    ↓
[DispatcherServlet]
    ↓
HandlerInterceptor (e.g., LoginRateLimitInterceptor)   ← runs here
    ↓
Controller method
```

| Aspect | Filter | HandlerInterceptor |
|---|---|---|
| Interface | `javax.servlet.Filter` | `org.springframework.web.servlet.HandlerInterceptor` |
| Layer | Servlet container | Spring MVC |
| Access to handler | No | Yes — knows which controller method will be called |
| Access to model/view | No | Yes (in `postHandle`) |
| Registration | `FilterRegistrationBean` or `@Component` | `WebMvcConfigurer.addInterceptors()` |
| Runs for | All requests (including static resources) | Only requests dispatched by `DispatcherServlet` |
| Use for | Auth, CORS, request/response modification | Rate limiting, audit logging, locale, access control |

### LoginRateLimitInterceptor
```java
public boolean preHandle(request, response, handler) {
    String ip = request.getRemoteAddr();
    // ConcurrentHashMap.compute() is atomic — thread-safe without explicit locking
    attempts.compute(ip, (key, val) -> {
        if (val == null || now - val[1] > WINDOW_MS) return new long[]{1, now};
        val[0]++;
        return val;
    });

    if (attempts.get(ip)[0] > MAX_ATTEMPTS) {
        response.setStatus(429);  // 429 Too Many Requests
        return false;             // stops processing — controller is never called
    }
    return true;
}
```

`preHandle` returning `false` short-circuits the entire handler chain — the controller method is never invoked.

**Limitation of this implementation**: state is in-memory, so it resets on restart and doesn't work across multiple instances. Production fix: store attempt counts in Redis with TTL (Day 27).

---

## 9. Authentication Flow — End to End

### Register
```
POST /api/auth/register { username, email, password }
    → GlobalExceptionHandler validates @Valid constraints
    → UserService.register()
        → check existsByEmail / existsByUsername
        → BCrypt.encode(password) — never store plain text
        → save User with Role.CUSTOMER
        → generate accessToken + refreshToken
    → 201 Created { accessToken, refreshToken, tokenType, expiresIn }
```

### Login
```
POST /api/auth/login { username, password }
    → LoginRateLimitInterceptor checks IP rate limit
    → UserService.login()
        → authManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
            → DaoAuthenticationProvider.loadUserByUsername(username)
            → BCrypt.matches(password, storedHash)
            → throws BadCredentialsException if either fails
        → generate accessToken + refreshToken
    → 200 OK { accessToken, refreshToken, tokenType, expiresIn }
```

### Authenticated request
```
GET /api/users/profile
    Authorization: Bearer eyJhbGci...
    → JwtAuthenticationFilter
        → extract token from header
        → jwtService.extractUsername(token)  — verifies signature + expiry
        → userDetailsService.loadUserByUsername(username)
        → jwtService.isTokenValid(token, user)
        → SecurityContextHolder.setAuthentication(...)
    → SecurityFilterChain: anyRequest().authenticated() — passes (context is populated)
    → UserController.getProfile(@AuthenticationPrincipal UserDetails principal)
        → principal.getUsername() — extracted from SecurityContext
    → 200 OK { id, username, email, roles, enabled, createdAt }
```

### @AuthenticationPrincipal
```java
public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserDetails principal)
```
`@AuthenticationPrincipal` injects the `UserDetails` object that was set in `SecurityContextHolder` by `JwtAuthenticationFilter`. No need to call `SecurityContextHolder.getContext().getAuthentication()` manually.

---

## 10. GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(...)  // 400

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(...)  // 400

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleAuthFailure(...) // 401

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(...)     // 500
}
```

`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`. It intercepts exceptions thrown from any `@RestController` and converts them to `ResponseEntity` responses.

**Security note**: `BadCredentialsException` and `UsernameNotFoundException` both return the same message `"Invalid credentials"`. This prevents username enumeration — an attacker can't tell whether the username doesn't exist or the password is wrong.

---

## 11. Testing

### JwtServiceTest — pure unit test
No Spring context. Instantiates `JwtService` directly with the same hex key as `application.yml`:
```java
jwtService = new JwtService("404E635266...", 900_000L, 604_800_000L);
```
Fast — runs in milliseconds. Tests: valid token, refresh type flag, wrong user, expired token.

### AuthControllerTest — full integration test
```java
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
@DirtiesContext(classMode = BEFORE_CLASS)
```

- `WebEnvironment.MOCK` — loads full Spring context, real security filter chain, real DB (H2), but no actual HTTP server
- `@ActiveProfiles("test")` — loads `application-test.yml`, activates `test` profile so `DataSeeder` (`@Profile("dev")`) doesn't run
- `@DirtiesContext(BEFORE_CLASS)` — fresh application context for this test class, preventing state bleed from other test classes
- `MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())` — builds `MockMvc` with the full security filter chain applied (required since `@AutoConfigureMockMvc` was removed in Spring Boot 4.x)

---

## 12. Spring Boot 4.x / Security 7.x Breaking Changes

| Change | Old | New |
|---|---|---|
| `@AutoConfigureMockMvc` | `org.springframework.boot.test.autoconfigure.web.servlet` | **Removed** — use `MockMvcBuilders.webAppContextSetup()` |
| Unauthenticated response | 401 | **403** by default — add `AuthenticationEntryPoint` for 401 |
| `DaoAuthenticationProvider` | `new DaoAuthenticationProvider()` + `setUserDetailsService()` | `new DaoAuthenticationProvider(userDetailsService)` |
| `@EnableGlobalMethodSecurity` | Used for `@PreAuthorize` | **Removed** — use `@EnableMethodSecurity` |
| `WebSecurityConfigurerAdapter` | Extended for security config | **Removed** — use `SecurityFilterChain` bean |

---

## 13. OAuth2 Resource Server Concept

Day 18 implements JWT manually. Spring Security also has a built-in OAuth2 Resource Server:

```java
http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));
```

This auto-configures:
- JWT parsing and signature verification
- Populating `SecurityContext` from JWT claims
- Standard claim mapping (`sub` → principal name, `scope`/`roles` → authorities)

**When to use which:**
- Manual `JwtService` + `JwtAuthenticationFilter` — full control, custom claims, learning purposes
- `oauth2ResourceServer` — production apps, standard OAuth2 flows, integrating with external identity providers (Keycloak, Auth0, Cognito)

---

## Key Concepts Summary

| Concept | One-line explanation |
|---|---|
| JWT | Signed token carrying identity + claims — server verifies signature, no session needed |
| Access token | Short-lived (15 min) — used to call APIs |
| Refresh token | Long-lived (7 days) — used only to get a new access token |
| `OncePerRequestFilter` | Filter guaranteed to run exactly once per HTTP request |
| `SecurityContextHolder` | Thread-local store for the current request's `Authentication` object |
| `@PreAuthorize` | AOP-based method-level security — evaluated before the method runs |
| `AuthenticationEntryPoint` | Fires on unauthenticated requests — should return 401 |
| `AccessDeniedHandler` | Fires on authenticated but unauthorized requests — returns 403 |
| `HandlerInterceptor` | Spring MVC hook — runs after DispatcherServlet, before controller |
| Filter | Servlet container hook — runs before DispatcherServlet |
| `@AuthenticationPrincipal` | Injects `UserDetails` from `SecurityContextHolder` into controller params |
