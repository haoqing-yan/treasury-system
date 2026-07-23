package com.treasury.service;

import com.treasury.domain.AppUser;
import com.treasury.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    public DatabaseUserDetailsService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("账号或密码错误"));
        var authorities = user.getRoles().stream()
                .flatMap(role -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(new SimpleGrantedAuthority("ROLE_" + role.name())),
                        role.permissions().stream()
                                .map(permission -> new SimpleGrantedAuthority(permission.authority()))
                ))
                .distinct()
                .toList();
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!user.isEnabled())
                .accountLocked(user.isLocked())
                .build();
    }
}
