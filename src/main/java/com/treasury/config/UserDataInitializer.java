package com.treasury.config;

import com.treasury.domain.AppUser;
import com.treasury.domain.SystemRole;
import com.treasury.repository.AppUserRepository;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
public class UserDataInitializer implements ApplicationRunner {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserDataInitializer(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing("operator", "operator123", "资金经办", Set.of(SystemRole.OPERATOR));
        createIfMissing("approver", "approver123", "资金审批", Set.of(SystemRole.APPROVER));
        createIfMissing("admin", "admin123", "系统管理员",
                Set.of(SystemRole.ADMIN, SystemRole.OPERATOR, SystemRole.APPROVER));
    }

    private void createIfMissing(String username, String password, String displayName, Set<SystemRole> roles) {
        if (repository.findByUsernameIgnoreCase(username).isEmpty()) {
            repository.save(new AppUser(username, passwordEncoder.encode(password), displayName, roles));
        }
    }
}
