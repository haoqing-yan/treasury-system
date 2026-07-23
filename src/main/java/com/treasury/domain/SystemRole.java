package com.treasury.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum SystemRole {
    OPERATOR(EnumSet.of(
            SystemPermission.PAYMENT_CREATE,
            SystemPermission.PAYMENT_SUBMIT,
            SystemPermission.CASH_PLAN_CREATE
    )),
    APPROVER(EnumSet.of(
            SystemPermission.PAYMENT_APPROVE,
            SystemPermission.RECONCILIATION_HANDLE,
            SystemPermission.EXCEPTION_HANDLE
    )),
    ADMIN(EnumSet.allOf(SystemPermission.class));

    private final Set<SystemPermission> permissions;

    SystemRole(Set<SystemPermission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public Set<SystemPermission> permissions() {
        return permissions;
    }
}
