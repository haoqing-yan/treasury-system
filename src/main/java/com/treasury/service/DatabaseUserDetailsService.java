package com.treasury.service;

import com.treasury.domain.AppUser;
import com.treasury.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
        String[] roles = user.getRoles().stream().map(Enum::name).sorted().toArray(String[]::new);
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(roles)
                .disabled(!user.isEnabled())
                .accountLocked(user.isLocked())
                .build();
    }
}
