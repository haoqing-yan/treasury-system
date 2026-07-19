package com.treasury.dto;

import com.treasury.domain.ExceptionCategory;
import com.treasury.domain.ExceptionCaseType;
import com.treasury.domain.ExceptionSeverity;
import com.treasury.domain.ExceptionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class ExceptionCaseDtos {
    private ExceptionCaseDtos() {
    }

    public record Response(
            Long id,
            String caseNo,
            ExceptionCategory category,
            ExceptionCaseType type,
            ExceptionSeverity severity,
            ExceptionStatus status,
            String title,
            String description,
            String sourceType,
            String sourceId,
            String sourceReference,
            String assignee,
            String resolution,
            LocalDateTime detectedAt,
            LocalDateTime claimedAt,
            LocalDateTime resolvedAt
    ) {
    }

    public record Summary(long totalCount, long businessCount, long systemCount,
                          long openCount, long processingCount,
                          long resolvedCount, long highPriorityCount) {
    }

    public record ResolveRequest(
            @NotBlank(message = "处理结论不能为空")
            @Size(min = 5, max = 500, message = "处理结论需为 5 至 500 个字符")
            String note
    ) {
    }

    public record ReopenRequest(
            @NotBlank(message = "重新打开原因不能为空")
            @Size(min = 5, max = 500, message = "重新打开原因需为 5 至 500 个字符")
            String reason
    ) {
    }
}
