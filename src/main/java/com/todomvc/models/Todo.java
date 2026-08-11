package com.todomvc.models;

import java.util.Objects;

/**
 * Immutable-ish value object representing a single todo item in the
 * expected-state model. Identity is by internal id (creation order),
 * not by text, so duplicate-text todos are handled correctly.
 */
public class Todo {

    private final long id;
    private String text;
    private boolean completed;

    public Todo(long id, String text, boolean completed) {
        this.id = id;
        this.text = text;
        this.completed = completed;
    }

    public long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Todo copy() {
        return new Todo(id, text, completed);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Todo)) return false;
        Todo todo = (Todo) o;
        return completed == todo.completed && Objects.equals(text, todo.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, completed);
    }

    @Override
    public String toString() {
        return "Todo{id=" + id + ", text='" + text + "', completed=" + completed + '}';
    }
}
