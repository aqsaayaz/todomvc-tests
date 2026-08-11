package com.todomvc.models;

import com.todomvc.enums.FilterType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Independent expected-state model for a TodoMVC session.
 *
 * CRITICAL INVARIANT: this class never reads from the browser/DOM. It is
 * mutated ONLY by applying actions (add/edit/toggle/delete/filter/clear/
 * toggleAll/reload) and is the single source of truth the page's actual
 * state is diffed against after every step. This is what prevents the
 * "tests read app state as the expected result" anti-pattern.
 */
public class TodoModel {

    private final List<Todo> todos = new ArrayList<>();
    private FilterType activeFilter = FilterType.ALL;
    private long nextId = 1;

    // ---- mutators (one per action type) ----

    public long add(String text) {
        long id = nextId++;
        todos.add(new Todo(id, text, false));
        return id;
    }

    public void edit(int visibleIndex, String newText) {
        Todo t = getVisible(visibleIndex);
        t.setText(newText);
    }

    public void toggle(int visibleIndex) {
        Todo t = getVisible(visibleIndex);
        t.setCompleted(!t.isCompleted());
    }

    public void toggleAll() {
        // TodoMVC semantics: if not all are completed, complete all;
        // if all are completed, mark all active.
        boolean allCompleted = !todos.isEmpty() && todos.stream().allMatch(Todo::isCompleted);
        boolean target = !allCompleted;
        for (Todo t : todos) {
            t.setCompleted(target);
        }
    }

    public void delete(int visibleIndex) {
        Todo t = getVisible(visibleIndex);
        todos.remove(t);
    }

    public void setFilter(FilterType filter) {
        this.activeFilter = filter;
    }

    public void clearCompleted() {
        todos.removeIf(Todo::isCompleted);
    }

    /** Reload is a no-op on the model: persisted state (localStorage) must survive. */
    public void reload() {
        // intentionally empty - state is expected to persist across reload
    }

    // ---- read-only projections used for assertions ----

    public List<Todo> getAllTodos() {
        return todos.stream().map(Todo::copy).collect(Collectors.toList());
    }

    public List<Todo> getVisibleTodos() {
        return todos.stream()
                .filter(this::matchesFilter)
                .map(Todo::copy)
                .collect(Collectors.toList());
    }

    public int getRemainingCount() {
        return (int) todos.stream().filter(t -> !t.isCompleted()).count();
    }

    public int getCompletedCount() {
        return (int) todos.stream().filter(Todo::isCompleted).count();
    }

    public FilterType getActiveFilter() {
        return activeFilter;
    }

    public boolean isEmpty() {
        return todos.isEmpty();
    }

    private boolean matchesFilter(Todo t) {
        switch (activeFilter) {
            case ACTIVE: return !t.isCompleted();
            case COMPLETED: return t.isCompleted();
            default: return true;
        }
    }

    private Todo getVisible(int visibleIndex) {
        List<Todo> visible = todos.stream().filter(this::matchesFilter).collect(Collectors.toList());
        if (visibleIndex < 0 || visibleIndex >= visible.size()) {
            throw new IndexOutOfBoundsException(
                    "No visible todo at index " + visibleIndex + " (visible count=" + visible.size() + ")");
        }
        Todo visibleTodo = visible.get(visibleIndex);
        // return the actual mutable instance held in `todos`, not the copy
        return todos.stream().filter(t -> t.getId() == visibleTodo.getId()).findFirst().orElseThrow();
    }
}
