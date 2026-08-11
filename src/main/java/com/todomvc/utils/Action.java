package com.todomvc.utils;

import com.todomvc.enums.FilterType;

/**
 * A single seeded action in a test scenario.
 *
 * Design note: ActionType is an enum here for exhaustiveness in the
 * executor's switch, but adding a new action (e.g. TOGGLE_ALL) requires
 * touching exactly three places, all additive, none structural:
 *   1. ActionType enum          -> add constant
 *   2. TodoModel                -> add a mutator method
 *   3. TodoPage + executor      -> add UI action + one switch case
 * No existing test, generator logic, or reporting code needs to change.
 */
public class Action {

    public enum ActionType {
        ADD,
        EDIT,
        TOGGLE,
        DELETE,
        FILTER,
        CLEAR_COMPLETED,
        RELOAD,
        TOGGLE_ALL // added during review - see README "Extending the suite"
    }

    private final ActionType type;
    private final Integer index;      // target index for EDIT/TOGGLE/DELETE
    private final String text;        // payload for ADD/EDIT
    private final FilterType filter;  // payload for FILTER

    private Action(ActionType type, Integer index, String text, FilterType filter) {
        this.type = type;
        this.index = index;
        this.text = text;
        this.filter = filter;
    }

    public static Action add(String text) {
        return new Action(ActionType.ADD, null, text, null);
    }

    public static Action edit(int index, String newText) {
        return new Action(ActionType.EDIT, index, newText, null);
    }

    public static Action toggle(int index) {
        return new Action(ActionType.TOGGLE, index, null, null);
    }

    public static Action delete(int index) {
        return new Action(ActionType.DELETE, index, null, null);
    }

    public static Action filter(FilterType filter) {
        return new Action(ActionType.FILTER, null, null, filter);
    }

    public static Action clearCompleted() {
        return new Action(ActionType.CLEAR_COMPLETED, null, null, null);
    }

    public static Action reload() {
        return new Action(ActionType.RELOAD, null, null, null);
    }

    public static Action toggleAll() {
        return new Action(ActionType.TOGGLE_ALL, null, null, null);
    }

    public ActionType getType() { return type; }
    public Integer getIndex() { return index; }
    public String getText() { return text; }
    public FilterType getFilter() { return filter; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(type.name());
        if (index != null) sb.append("(index=").append(index).append(")");
        if (text != null) sb.append("(text=\"").append(text).append("\")");
        if (filter != null) sb.append("(filter=").append(filter).append(")");
        return sb.toString();
    }
}
