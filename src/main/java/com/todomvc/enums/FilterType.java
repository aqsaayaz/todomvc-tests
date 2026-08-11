package com.todomvc.enums;

public enum FilterType {
    ALL(""),
    ACTIVE("active"),
    COMPLETED("completed");

    private final String path;

    FilterType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
