package com.ByteKnights.com.resturarent_system.audit;

import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditStatus;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logSuccess(Auditable auditable, Object result) {
        Long targetId = extractId(result);

        auditLogService.logCurrentUserAction(
                auditable.module(),
                auditable.eventType(),
                AuditStatus.SUCCESS,
                auditable.successSeverity(),
                auditable.targetType(),
                targetId,
                null,
                resolveDescription(auditable.description(), "Action completed successfully"),
                null,
                auditable.captureResultAsNewValue() ? result : null
        );
    }

    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "ex")
    public void logFailure(Auditable auditable, Exception ex) {
        auditLogService.logCurrentUserAction(
                auditable.module(),
                auditable.eventType(),
                AuditStatus.FAILURE,
                AuditSeverity.ERROR,
                auditable.targetType(),
                null,
                null,
                resolveDescription(auditable.description(), "Action failed") + ": " + ex.getMessage(),
                null,
                null
        );
    }

    private String resolveDescription(String customDescription, String fallback) {
        return customDescription != null && !customDescription.isBlank()
                ? customDescription
                : fallback;
    }

    private Long extractId(Object result) {
        if (result == null) {
            return null;
        }

        try {
            Method getIdMethod = result.getClass().getMethod("getId");
            Object idValue = getIdMethod.invoke(result);

            if (idValue instanceof Number number) {
                return number.longValue();
            }
        } catch (Exception ignored) {
            // Some DTOs may not have getId(). That is okay.
        }

        return null;
    }
}