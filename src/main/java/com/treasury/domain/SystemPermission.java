package com.treasury.domain;

/**
 * 系统中的原子权限。角色只负责聚合权限，业务代码只依赖权限，避免岗位调整时修改接口。
 */
public enum SystemPermission {
    ACCOUNT_MANAGE("account:manage"),
    PAYMENT_CREATE("payment:create"),
    PAYMENT_SUBMIT("payment:submit"),
    PAYMENT_APPROVE("payment:approve"),
    PAYMENT_EXECUTE("payment:execute"),
    RECONCILIATION_HANDLE("reconciliation:handle"),
    EXCEPTION_HANDLE("exception:handle"),
    CASH_PLAN_CREATE("cash-plan:create"),
    AUDIT_READ("audit:read");

    private final String authority;

    SystemPermission(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
