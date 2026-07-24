package com.treasury.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    /**
     * 同时兼容标准 ROLE_* 身份与细粒度权限。业务方法统一校验细粒度权限。
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        RoleHierarchyImpl hierarchy = RoleHierarchyImpl.fromHierarchy("""
                ROLE_OPERATOR > payment:create
                ROLE_OPERATOR > payment:submit
                ROLE_OPERATOR > cash-plan:create
                ROLE_APPROVER > payment:approve
                ROLE_APPROVER > reconciliation:handle
                ROLE_APPROVER > exception:handle
                ROLE_ADMIN > account:manage
                ROLE_ADMIN > payment:create
                ROLE_ADMIN > payment:submit
                ROLE_ADMIN > payment:approve
                ROLE_ADMIN > payment:execute
                ROLE_ADMIN > payment:batch
                ROLE_ADMIN > reconciliation:handle
                ROLE_ADMIN > exception:handle
                ROLE_ADMIN > cash-plan:create
                ROLE_ADMIN > audit:read
                """);
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(hierarchy);
        return handler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login.html",
                                "/css/**",
                                "/js/login.js",
                                "/api/auth/csrf",
                                "/api/auth/login",
                                "/actuator/health",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login.html?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login.html?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                )
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

}
