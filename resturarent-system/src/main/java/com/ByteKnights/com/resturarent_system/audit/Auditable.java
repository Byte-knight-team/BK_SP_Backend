package com.ByteKnights.com.resturarent_system.audit;

import com.ByteKnights.com.resturarent_system.entity.AuditEventType;
import com.ByteKnights.com.resturarent_system.entity.AuditModule;
import com.ByteKnights.com.resturarent_system.entity.AuditSeverity;
import com.ByteKnights.com.resturarent_system.entity.AuditTargetType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    AuditModule module();

    AuditEventType eventType();

    AuditTargetType targetType();

    AuditSeverity successSeverity() default AuditSeverity.INFO;

    String description() default "";

    boolean captureResultAsNewValue() default false;
}