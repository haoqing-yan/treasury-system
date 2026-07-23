package com.treasury.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "parameterName", token.getParameterName(),
                "token", token.getToken());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        var authorities = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();
        return Map.of(
                "username", authentication.getName(),
                "roles", authorities.stream()
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .map(authority -> authority.substring("ROLE_".length()))
                        .sorted()
                        .toList(),
                "permissions", authorities.stream()
                        .filter(authority -> !authority.startsWith("ROLE_"))
                        .sorted()
                        .toList()
        );
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (servletRequest.getSession(false) != null) {
            servletRequest.changeSessionId();
        }
        servletRequest.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context
        );
        return me(authentication);
    }

    public record LoginRequest(
            @jakarta.validation.constraints.NotBlank String username,
            @jakarta.validation.constraints.NotBlank String password
    ) {
    }
}
