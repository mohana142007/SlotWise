package com.slotwise.entity;

public enum ResourceType {
    ROOM("Room / Facility"),
    EQUIPMENT("Equipment / Device");

    private final String displayName;

    ResourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
