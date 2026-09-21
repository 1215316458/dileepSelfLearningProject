package com.ecommerce.user_service.service;

import com.ecommerce.user_service.domain.entity.User;
import com.ecommerce.user_service.domain.enums.Role;
import com.ecommerce.user_service.dto.*;
import com.ecommerce.user_service.repository.UserRepository;
import com.ecommerce.user_service.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authManager;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authManager) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService      = jwtService;
        this.authManager     = authManager;
    }

    // --- Auth ---

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email()))
            throw new IllegalArgumentException("Email already registered: " + req.email());
        if (userRepository.existsByUsername(req.username()))
            throw new IllegalArgumentException("Username already taken: " + req.username());

        User user = new User(
            req.username(),
            req.email(),
            passwordEncoder.encode(req.password()),
            Set.of(Role.CUSTOMER)
        );
        userRepository.save(user);

        return AuthResponse.of(
            jwtService.generateAccessToken(user),
            jwtService.generateRefreshToken(user),
            jwtService.getAccessTokenExpiration()
        );
    }

    public AuthResponse login(AuthRequest req) {
        // authManager.authenticate triggers DaoAuthenticationProvider:
        //   1. loadUserByUsername(req.username())
        //   2. BCrypt.matches(req.password(), storedHash)
        // throws BadCredentialsException if either step fails
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        User user = userRepository.findByUsername(req.username())
                .or(() -> userRepository.findByEmail(req.username()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return AuthResponse.of(
            jwtService.generateAccessToken(user),
            jwtService.generateRefreshToken(user),
            jwtService.getAccessTokenExpiration()
        );
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken))
            throw new IllegalArgumentException("Not a refresh token");

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!jwtService.isTokenValid(refreshToken, user))
            throw new IllegalArgumentException("Refresh token expired or invalid");

        return AuthResponse.of(
            jwtService.generateAccessToken(user),
            jwtService.generateRefreshToken(user),
            jwtService.getAccessTokenExpiration()
        );
    }

    // --- Profile ---

    @Transactional(readOnly = true)
    public UserResponse getProfile(String username) {
        return UserResponse.from(findByUsername(username));
    }

    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest req) {
        User user = findByUsername(username);

        if (req.email() != null && !req.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(req.email()))
                throw new IllegalArgumentException("Email already in use: " + req.email());
            user.setEmail(req.email());
        }
        if (req.password() != null && !req.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    // --- Admin ---

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id))
            throw new IllegalArgumentException("User not found: " + id);
        userRepository.deleteById(id);
    }

    // --- Helper ---

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
