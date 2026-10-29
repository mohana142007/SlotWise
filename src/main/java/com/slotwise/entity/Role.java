package com.slotwise.entity;

public enum Role {
    ROLE_STUDENT("Student"),
    ROLE_FACULTY("Faculty"),
    ROLE_ADMIN("Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
