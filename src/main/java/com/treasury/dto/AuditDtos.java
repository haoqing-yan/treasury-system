package com.treasury.dto;

import java.time.LocalDateTime;

public final class AuditDtos {
    private AuditDtos() {
    }

    public record Response(
            Long id,
            String username,
            String action,
            String resourceType,
            String resourceId,
            String detail,
            String ipAddress,
            LocalDateTime operatedAt
    ) {
    }
}
