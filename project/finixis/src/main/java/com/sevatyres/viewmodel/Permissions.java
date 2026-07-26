package com.sevatyres.viewmodel;

/** Role-based permission checks for UI. */
public final class Permissions {
    private Permissions() {}

    public static boolean canEditInventory()   { return SessionState.isAdmin() || SessionState.isManager(); }
    public static boolean canDeleteCustomers() { return SessionState.isAdmin(); }
    public static boolean canViewReports()     { return true; }
    public static boolean canExportData()      { return SessionState.isAdmin() || SessionState.isManager(); }
}
