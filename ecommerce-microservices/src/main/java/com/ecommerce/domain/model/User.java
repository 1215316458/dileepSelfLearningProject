package com.ecommerce.domain.model;

import com.ecommerce.domain.enums.Role;

public class User extends BaseEntity<Long> {

    private static final long serialVersionUID = 1L;

    private String username;
    private String email;
    // transient — skipped during serialization. Password will be null after deserialization.
    // Never persist raw passwords to disk.
    private transient String password;
    private Role role;

    public User(Long id, String username, String email, String password, Role role) {
        setIdentity(id);
        setCreatedAt(java.time.LocalDateTime.now());
        setUpdatedAt(java.time.LocalDateTime.now());
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    @Override
    public String toString() {
        return new StringBuilder("User{")
                .append("id=").append(getIdentity())
                .append(", username=").append(username)
                .append(", email=").append(email)
                // password intentionally excluded from toString — never log sensitive data
                .append(", role=").append(role)
                .append('}')
                .toString();
    }
}
