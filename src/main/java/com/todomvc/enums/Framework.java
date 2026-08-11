package com.todomvc.enums;

/**
 * Represents each TodoMVC implementation under test.
 * Adding a new framework target requires only a new enum constant + URL,
 * nothing else in the suite changes.
 */
public enum Framework {

    REACT("react", "https://todomvc.com/examples/react/dist/"),
    VUE("vue", "https://todomvc.com/examples/vue/dist/"),
    ANGULAR("angular", "https://todomvc.com/examples/angular/dist/browser/");

    private final String id;
    private final String defaultUrl;

    Framework(String id, String defaultUrl) {
        this.id = id;
        this.defaultUrl = defaultUrl;
    }

    public String getId() {
        return id;
    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public static Framework fromId(String id) {
        for (Framework f : values()) {
            if (f.id.equalsIgnoreCase(id)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown framework id: " + id
                + ". Valid values: react, vue, angular");
    }
}
