package com.sevatyres.viewmodel;

/** Simple singleton for login session state. */
public final class SessionState {
    private SessionState() {}

    private static String currentUserName = "Admin";
    private static String currentUserRole = "Admin";

    public static String getCurrentUserName() { return currentUserName; }
    public static void setCurrentUserName(String name) { currentUserName = name; }

    public static String getCurrentUserRole() { return currentUserRole; }
    public static void setCurrentUserRole(String role) { currentUserRole = role; }

    public static boolean isAdmin() { return "Admin".equalsIgnoreCase(currentUserRole); }
    public static boolean isManager() { return "Manager".equalsIgnoreCase(currentUserRole); }
}
