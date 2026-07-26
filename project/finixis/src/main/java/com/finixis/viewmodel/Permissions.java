package com.finixis.viewmodel;

import com.finixis.model.Role;

/**
 * Mirrors the permission matrix from the spec. UI-only simulation — not access control.
 */
public final class Permissions {
    private Permissions() {}

    public static boolean canAddCustomer(Role r) { return r != Role.EMPLOYEE; }
    public static boolean canEditCustomer(Role r) { return r != Role.EMPLOYEE; }
    public static boolean canDeleteCustomer(Role r) { return r == Role.ADMIN; }

    public static boolean canAddCredit(Role r) { return true; }
    public static boolean canEditCredit(Role r) { return r != Role.EMPLOYEE; }
    public static boolean canDeleteCredit(Role r) { return r != Role.EMPLOYEE; }
    public static boolean canMarkCreditSettled(Role r) { return r != Role.EMPLOYEE; }

    public static boolean canRecordPayment(Role r) { return true; }
    public static boolean canEditPayment(Role r) { return r != Role.EMPLOYEE; }
    public static boolean canDeletePayment(Role r) { return r == Role.ADMIN; }

    public static boolean canAddInventoryItem(Role r) { return r != Role.EMPLOYEE; }
    public static boolean canEditInventoryItem(Role r) { return r != Role.EMPLOYEE; }
    public static boolean canDeleteInventoryItem(Role r) { return r == Role.ADMIN; }
    public static boolean canStockInOut(Role r) { return true; }

    public static boolean canAddTransaction(Role r) { return true; }
    public static boolean canEditTransaction(Role r) { return r != Role.EMPLOYEE; }
    public static boolean canDeleteTransaction(Role r) { return r != Role.EMPLOYEE; }

    public static boolean canExportOrReport(Role r) { return r != Role.EMPLOYEE; }
    public static boolean canAccessReports(Role r) { return r != Role.EMPLOYEE; }
    public static boolean canManageUsers(Role r) { return r == Role.ADMIN; }
    public static boolean canToggleTheme(Role r) { return true; }
}
