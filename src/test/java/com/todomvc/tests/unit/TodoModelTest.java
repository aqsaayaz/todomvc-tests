package com.todomvc.tests.unit;

import com.todomvc.enums.FilterType;
import com.todomvc.models.Todo;
import com.todomvc.models.TodoModel;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Pure unit tests for {@link TodoModel}.
 *
 * Unlike {@code CrossFrameworkTest}, these do NOT touch a browser or
 * WebDriver at all — they only exercise the in-memory expected-state
 * model. That makes them fast and 100% deterministic, which is exactly
 * what CI needs for a quick "did I break the core logic" signal, as
 * opposed to the slower Selenium suite in testng.xml.
 */
public class TodoModelTest {

    private TodoModel model;

    @BeforeMethod
    public void setUp() {
        model = new TodoModel();
    }

    @Test
    public void testAddToggleAndCounts() {
        model.add("Buy milk");
        model.add("Walk the dog");

        Assert.assertEquals(model.getAllTodos().size(), 2, "should have 2 todos after adding 2");
        Assert.assertEquals(model.getRemainingCount(), 2, "nothing completed yet");
        Assert.assertEquals(model.getCompletedCount(), 0, "nothing completed yet");

        // toggle the first visible todo ("Buy milk")
        model.toggle(0);

        List<Todo> all = model.getAllTodos();
        Assert.assertTrue(all.get(0).isCompleted(), "first todo should now be completed");
        Assert.assertFalse(all.get(1).isCompleted(), "second todo should be untouched");
        Assert.assertEquals(model.getCompletedCount(), 1);
        Assert.assertEquals(model.getRemainingCount(), 1);

        // toggling twice returns it to the original state
        model.toggle(0);
        Assert.assertEquals(model.getCompletedCount(), 0);
        Assert.assertEquals(model.getRemainingCount(), 2);
    }

    @Test
    public void testFilterAndClearCompleted() {
        model.add("Buy milk");      // index 0
        model.add("Walk the dog");  // index 1
        model.add("Pay bills");     // index 2

        model.toggle(0); // complete "Buy milk"
        model.toggle(2); // complete "Pay bills"

        model.setFilter(FilterType.COMPLETED);
        List<String> completedTexts = model.getVisibleTodos().stream().map(Todo::getText).toList();
        Assert.assertEquals(completedTexts, List.of("Buy milk", "Pay bills"));

        model.setFilter(FilterType.ACTIVE);
        List<String> activeTexts = model.getVisibleTodos().stream().map(Todo::getText).toList();
        Assert.assertEquals(activeTexts, List.of("Walk the dog"));

        model.clearCompleted();
        model.setFilter(FilterType.ALL);

        Assert.assertEquals(model.getAllTodos().size(), 1, "only the active todo should remain");
        Assert.assertEquals(model.getAllTodos().get(0).getText(), "Walk the dog");
        Assert.assertEquals(model.getRemainingCount(), 1);
        Assert.assertEquals(model.getCompletedCount(), 0);
    }
}
