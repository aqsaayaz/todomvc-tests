package com.todomvc.generators;

import com.todomvc.enums.FilterType;
import com.todomvc.utils.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates a deterministic, seeded sequence of actions.
 *
 * The same seed always produces the same action sequence.
 * The generator is independent of React, Vue, and Angular.
 */
public class ActionGenerator {

    private static final String[] WORDS = {
            "buy milk",
            "walk dog",
            "write report",
            "read book",
            "clean house",
            "call mom",
            "fix bug",
            "plan trip",
            "pay bills",
            "water plants",
            "book flight",
            "renew license",
            "review PR",
            "update resume",
            "learn Rust"
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
     * Generates a deterministic list of actions.
     */
    public List<Action> generate(int count) {

        List<Action> actions = new ArrayList<>();

        // Shadow state is used only to generate valid indexes.
        List<Boolean> shadowCompleted = new ArrayList<>();
        FilterType shadowFilter = FilterType.ALL;

        int guardAttempts = 0;

        while (actions.size() < count && guardAttempts < count * 20) {

            guardAttempts++;

            int visibleCount =
                    visibleCount(shadowCompleted, shadowFilter);

            int roll = random.nextInt(100);

            Action action;

            // ADD - 30%
            if (roll < 30 || shadowCompleted.isEmpty()) {

                String text =
                        WORDS[random.nextInt(WORDS.length)]
                                + " #"
                                + (shadowCompleted.size() + 1);

                action = Action.add(text);

                shadowCompleted.add(false);

                // TOGGLE - 15%
            } else if (roll < 45 && visibleCount > 0) {

                int index = random.nextInt(visibleCount);

                action = Action.toggle(index);

                int realIndex =
                        translateVisibleToReal(
                                shadowCompleted,
                                shadowFilter,
                                index
                        );

                shadowCompleted.set(
                        realIndex,
                        !shadowCompleted.get(realIndex)
                );

                // EDIT - 10%
            } else if (roll < 55 && visibleCount > 0) {

                int index = random.nextInt(visibleCount);

                String text =
                        WORDS[random.nextInt(WORDS.length)]
                                + " (edited)";

                action = Action.edit(index, text);

                // DELETE - 10%
            } else if (roll < 65 && visibleCount > 0) {

                int index = random.nextInt(visibleCount);

                action = Action.delete(index);

                int realIndex =
                        translateVisibleToReal(
                                shadowCompleted,
                                shadowFilter,
                                index
                        );

                shadowCompleted.remove(realIndex);

                // FILTER - 15%
            } else if (roll < 80) {

                FilterType[] filters = FilterType.values();

                FilterType filter =
                        filters[random.nextInt(filters.length)];

                action = Action.filter(filter);

                shadowFilter = filter;

                // CLEAR COMPLETED - 5%
            } else if (roll < 85) {

                action = Action.clearCompleted();

                shadowCompleted.removeIf(Boolean::booleanValue);

                // TOGGLE ALL - 7%
            } else if (roll < 92) {

                action = Action.toggleAll();

                boolean allCompleted =
                        !shadowCompleted.isEmpty()
                                && shadowCompleted.stream()
                                .allMatch(Boolean::booleanValue);

                for (int i = 0; i < shadowCompleted.size(); i++) {
                    shadowCompleted.set(i, !allCompleted);
                }

                // RELOAD - 8%
            } else {

                action = Action.reload();
            }

            actions.add(action);
        }

        return actions;
    }

    private int visibleCount(
            List<Boolean> shadowCompleted,
            FilterType filter) {

        int count = 0;

        for (boolean completed : shadowCompleted) {

            if (matches(completed, filter)) {
                count++;
            }
        }

        return count;
    }

    private int translateVisibleToReal(
            List<Boolean> shadowCompleted,
            FilterType filter,
            int visibleIndex) {

        int seen = -1;

        for (int i = 0; i < shadowCompleted.size(); i++) {

            if (matches(shadowCompleted.get(i), filter)) {

                seen++;

                if (seen == visibleIndex) {
                    return i;
                }
            }
        }

        throw new IllegalStateException(
                "Invalid visible todo index: " + visibleIndex
        );
    }

    private boolean matches(
            boolean completed,
            FilterType filter) {

        switch (filter) {

            case ACTIVE:
                return !completed;

            case COMPLETED:
                return completed;

            case ALL:
            default:
                return true;
        }
    }
}