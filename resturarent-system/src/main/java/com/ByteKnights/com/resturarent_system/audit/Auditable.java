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

    //module is the module that is being audited
    AuditModule module();

    //event type is the type of event that is being audited
    AuditEventType eventType();

    //target type is the type of target that is being audited
    AuditTargetType targetType();

    //success severity is the severity of the event
    AuditSeverity successSeverity() default AuditSeverity.INFO;

    //description is the description of the event
    String description() default "";

    //capture result as new value is a boolean value that is used to capture the result of the event
    boolean captureResultAsNewValue() default false;
}