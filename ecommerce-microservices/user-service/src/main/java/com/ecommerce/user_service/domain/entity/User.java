package com.ecommerce.user_service.domain.entity;

import com.ecommerce.user_service.domain.enums.Role;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_email",    columnList = "email",    unique = true),
        @Index(name = "idx_user_username", columnList = "username", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;   // stored as BCrypt hash — never plain text

    @Column(nullable = false)
    private boolean enabled = true;

    // @ElementCollection — stores enum values in a separate join table (user_roles)
    // simpler than @ManyToMany when roles don't need their own entity fields
    @ElementCollection(fetch = FetchType.EAGER)  // EAGER: roles always needed for auth
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    // @OneToMany — one user has many addresses
    // mappedBy = "user" → Address.user field owns the FK (avoids duplicate FK column)
    // cascade = ALL → saving/deleting User also saves/deletes its Addresses
    // orphanRemoval → removing Address from list deletes it from DB
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Address> addresses = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    protected User() {}

    public User(String username, String email, String password, Set<Role> roles) {
        this.username = username;
        this.email    = email;
        this.password = password;
        this.roles    = roles;
    }

    // --- UserDetails contract — Spring Security reads these ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // convert Role enum → GrantedAuthority with "ROLE_" prefix (Spring Security convention)
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
    }

    @Override public String getPassword()             { return password; }
    @Override public String getUsername()             { return username; }
    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()              { return enabled; }

    // --- Getters ---
    public Long getId()              { return id; }
    public String getEmail()         { return email; }
    public Set<Role> getRoles()      { return Collections.unmodifiableSet(roles); }
    public List<Address> getAddresses() { return Collections.unmodifiableList(addresses); }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getUpdatedAt()    { return updatedAt; }

    // --- Mutators ---
    public void setPassword(String password) { this.password = password; }
    public void setEnabled(boolean enabled)  { this.enabled = enabled; }
    public void setEmail(String email)       { this.email = email; }

    public void addRole(Role role)           { roles.add(role); }
    public void removeRole(Role role)        { roles.remove(role); }

    // helper — keeps bidirectional relationship consistent
    public void addAddress(Address address) {
        addresses.add(address);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User u)) return false;
        return Objects.equals(id, u.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email + "', roles=" + roles + "}";
    }
}
