package com.treasury.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "app_users", uniqueConstraints =
        @UniqueConstraint(name = "uk_app_user_username", columnNames = "username"))
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean locked;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Set<SystemRole> roles = EnumSet.noneOf(SystemRole.class);

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, String displayName, Set<SystemRole> roles) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.enabled = true;
        this.locked = false;
        this.roles = EnumSet.copyOf(roles);
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public boolean isEnabled() { return enabled; }
    public boolean isLocked() { return locked; }
    public Set<SystemRole> getRoles() { return Collections.unmodifiableSet(roles); }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
