package com.treasury.service;

import com.treasury.domain.AuditLog;
import com.treasury.dto.AuditDtos;
import com.treasury.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String username, String action, String resourceType, String resourceId, String detail) {
        repository.save(new AuditLog(username, action, resourceType, resourceId, detail, currentIp()));
    }

    @Transactional(readOnly = true)
    public List<AuditDtos.Response> recent() {
        return repository.findTop100ByOrderByOperatedAtDesc().stream()
                .map(log -> new AuditDtos.Response(
                        log.getId(), log.getUsername(), log.getAction(), log.getResourceType(),
                        log.getResourceId(), log.getDetail(), log.getIpAddress(), log.getOperatedAt()
                ))
                .toList();
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "system";
    }
}
