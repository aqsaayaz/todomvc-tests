package com.todomvc.generators;

import com.todomvc.enums.FilterType;
import com.todomvc.models.TodoModel;
import com.todomvc.utils.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates a deterministic, seeded sequence of actions.
 *
 * Determinism contract: given the same seed, generate() ALWAYS produces the
 * exact same list of Action objects, in the exact same order, with the same
 * payload text. This is achieved by:
 *   - using a single java.util.Random seeded once from the constructor seed
 *   - never consulting wall-clock time, UI state, or any other entropy
 *   - tracking a lightweight shadow of "how many todos exist / are visible"
 *     purely to keep generated indices in-bounds (this shadow is NOT the
 *     expected-state model used for assertions - that is TodoModel, built
 *     fresh and mutated independently by the test as actions execute)
 *
 * The generator is also framework-agnostic: it knows nothing about React,
 * Vue, or Angular. The identical seed produces the identical action list
 * regardless of which framework the suite is pointed at, which is what
 * makes "one test suite, unchanged, against three apps" possible.
 */
public class ActionGenerator {

    private static final String[] WORDS = {
            "buy milk", "walk dog", "write report", "read book", "clean house",
            "call mom", "fix bug", "plan trip", "pay bills", "water plants",
            "book flight", "renew license", "review PR", "update resume", "learn Rust"
    };

    private final Random random;
    private final long seed;

    public ActionGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public long getSeed() {
        return seed;
    }

    /**
     * Generates {@code count} seeded actions. A shadow todo count/visibility
     * tracker ensures indices stay valid (e.g. never TOGGLE index 3 when only
     * 2 todos are visible).
     */
    public List<Action> generate(int count) {
        List<Action> actions = new ArrayList<>();
        // Shadow bookkeeping only - to keep generated action indices legal.
        List<Boolean> shadowCompleted = new ArrayList<>();
        FilterType shadowFilter = FilterType.ALL;

        int guardAttempts = 0;
        while (actions.size() < count && guardAttempts < count * 20) {
            guardAttempts++;
            int visibleCount = visibleCount(shadowCompleted, shadowFilter);
            // weighted choice of action kind
            int roll = random.nextInt(100);
            Action action;

            if (roll < 30 || shadowCompleted.isEmpty()) {
                // ADD (30%) - always legal, also used as bootstrap when list is empty
                String text = WORDS[random.nextInt(WORDS.length)] + " #" + (shadowCompleted.size() + 1);
                action = Action.add(text);
                shadowCompleted.add(false);
            } else if (roll < 45 && visibleCount > 0) {
                // TOGGLE (15%)
                int idx = random.nextInt(visibleCount);
                action = Action.toggle(idx);
                int realIdx = translateVisibleToReal(shadowCompleted, shadowFilter, idx);
                shadowCompleted.set(realIdx, !shadowCompleted.get(realIdx));
            } else if (roll < 55 && visibleCount > 0) {
                // EDIT (10%)
                int idx = random.nextInt(visibleCount);
                String text = WORDS[random.nextInt(WORDS.length)] + " (edited)";
                action = Action.edit(idx, text);
            } else if (roll < 65 && visibleCount > 0) {
                // DELETE (10%)
                int idx = random.nextInt(visibleCount);
                action = Action.delete(idx);
                int realIdx = translateVisibleToReal(shadowCompleted, shadowFilter, idx);
                shadowCompleted.remove(realIdx);
            } else if (roll < 80) {
                // FILTER (15%)
                FilterType[] options = FilterType.values();
                FilterType f = options[random.nextInt(options.length)];
                action = Action.filter(f);
                shadowFilter = f;
            } else if (roll < 90) {
                // CLEAR_COMPLETED (10%)
                action = Action.clearCompleted();
                shadowCompleted.removeIf(Boolean::booleanValue);
            } else {
                // RELOAD (10%)
                action = Action.reload();
            }

            actions.add(action);
        }
        return actions;
    }

    private int visibleCount(List<Boolean> shadowCompleted, FilterType filter) {
        int c = 0;
        for (boolean completed : shadowCompleted) {
            if (matches(completed, filter)) c++;
        }
        return c;
    }

    private int translateVisibleToReal(List<Boolean> shadowCompleted, FilterType filter, int visibleIdx) {
        int seen = -1;
        for (int i = 0; i < shadowCompleted.size(); i++) {
            if (matches(shadowCompleted.get(i), filter)) {
                seen++;
                if (seen == visibleIdx) return i;
            }
        }
        throw new IllegalStateException("translateVisibleToReal out of range");
    }

    private boolean matches(boolean completed, FilterType filter) {
        switch (filter) {
            case ACTIVE: return !completed;
            case COMPLETED: return completed;
            default: return true;
        }
    }
}
