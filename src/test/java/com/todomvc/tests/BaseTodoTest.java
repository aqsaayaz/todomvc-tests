package com.todomvc.tests;

import com.todomvc.config.TestConfig;
import com.todomvc.enums.Framework;
import com.todomvc.models.Todo;
import com.todomvc.models.TodoModel;
import com.todomvc.pages.TodoPage;
import com.todomvc.utils.Action;
import com.todomvc.utils.TestReporter;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;

import java.util.logging.Level;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared harness for cross-framework TodoMVC tests.
 *
 * This class is intentionally framework-agnostic and action-set-agnostic:
 * it does not enumerate ADD/EDIT/TOGGLE/... anywhere. The dispatch table
 * lives in {@link #executeAction(Action)} as a single switch, so extending
 * the action vocabulary (e.g. TOGGLE_ALL) means adding one case there and
 * one mutator in TodoModel - this file's structure does not change.
 */
public abstract class BaseTodoTest {

    protected WebDriver driver;
    protected TodoPage page;
    protected TodoModel model;
    protected Framework framework;
    protected long seed;

    protected final List<Action> executedHistory = new ArrayList<>();

    @BeforeMethod
    public void setUp(ITestContext context) {
        // Prefer the per-<test> XML parameter (thread-safe under
        // parallel="tests") and fall back to the -Dframework CLI override.
        String xmlFramework = context.getCurrentXmlTest().getParameter("framework");
        framework = (xmlFramework != null && !xmlFramework.isBlank())
                ? Framework.fromId(xmlFramework)
                : TestConfig.getFramework();
        seed = TestConfig.getSeed();

        ChromeOptions options = new ChromeOptions();

        LoggingPreferences loggingPreferences = new LoggingPreferences();
        loggingPreferences.enable(LogType.PERFORMANCE, Level.ALL);
        options.setCapability("goog:loggingPrefs", loggingPreferences);

        if (TestConfig.isHeadless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1280,900", "--disable-gpu", "--no-sandbox");
        driver = new ChromeDriver(options);

        page = new TodoPage(driver, TestConfig.getBaseUrl(framework), TestConfig.getImplicitWaitSeconds());
        model = new TodoModel();
        executedHistory.clear();

        page.open();
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Executes one action against BOTH the real app (TodoPage) and the
     * independent expected-state model (TodoModel), then asserts they
     * agree. This is the single dispatch point for the action vocabulary.
     */
    protected void executeAction(Action action) {
        executedHistory.add(action);
        try {
            switch (action.getType()) {
                case ADD:
                    page.addTodo(action.getText());
                    model.add(action.getText());
                    break;
                case EDIT:
                    page.editTodo(action.getIndex(), action.getText());
                    model.edit(action.getIndex(), action.getText());
                    break;
                case TOGGLE:
                    page.toggleTodo(action.getIndex());
                    model.toggle(action.getIndex());
                    break;
                case DELETE:
                    page.deleteTodo(action.getIndex());
                    model.delete(action.getIndex());
                    break;
                case FILTER:
                    page.applyFilter(action.getFilter());
                    model.setFilter(action.getFilter());
                    break;
                case CLEAR_COMPLETED:
                    page.clearCompleted();
                    model.clearCompleted();
                    break;
                case RELOAD:
                    page.reload();
                    model.reload();
                    break;
                case TOGGLE_ALL:
                    page.toggleAll();
                    model.toggleAll();
                    break;
                default:
                    throw new IllegalStateException("Unhandled action type: " + action.getType());
            }
        } catch (RuntimeException e) {
            failWithReport(executedHistory.size() - 1, e);
            throw e;
        }

        assertStateMatches(executedHistory.size() - 1);
    }

    /**
     * Verifies exact todo list, completion states, filter, and remaining
     * count of the live app against the independent model. Called after
     * every single action.
     */
    protected void assertStateMatches(int actionIndex) {
        List<Todo> expectedVisible = model.getVisibleTodos();
        List<String> actualTexts;
        List<Boolean> actualCompleted;
        int actualRemaining;

        try {
            actualTexts = page.getVisibleTodoTexts();
            actualCompleted = page.getVisibleTodoCompletionStates();
            actualRemaining = page.getRemainingCountDisplayed();
        } catch (RuntimeException e) {
            failWithReport(actionIndex, e);
            throw e;
        }

        List<String> expectedTexts = expectedVisible.stream().map(Todo::getText).toList();
        List<Boolean> expectedCompleted = expectedVisible.stream().map(Todo::isCompleted).toList();
        int expectedRemaining = model.getRemainingCount();

        boolean mismatch = !expectedTexts.equals(actualTexts)
                || !expectedCompleted.equals(actualCompleted)
                || expectedRemaining != actualRemaining
                || model.getActiveFilter() != page.getActiveFilterDisplayed();

        if (mismatch) {
            failWithReport(actionIndex, null);
        }

        Assert.assertEquals(actualTexts, expectedTexts, "Visible todo list text mismatch after action #" + actionIndex);
        Assert.assertEquals(actualCompleted, expectedCompleted, "Completion states mismatch after action #" + actionIndex);
        Assert.assertEquals(actualRemaining, expectedRemaining, "Remaining count mismatch after action #" + actionIndex);
        Assert.assertEquals(page.getActiveFilterDisplayed(), model.getActiveFilter(), "Active filter mismatch after action #" + actionIndex);
    }

    private void failWithReport(int actionIndex, Throwable cause) {
        byte[] screenshot = null;
        try {
            screenshot = page.takeScreenshot();
        } catch (RuntimeException ignored) {
            // best-effort
        }

        String expectedDesc = TestReporter.describeTodos(model.getVisibleTodos())
                + "remaining=" + model.getRemainingCount()
                + ", filter=" + model.getActiveFilter();

        String actualDesc;
        try {
            actualDesc = "texts=" + page.getVisibleTodoTexts()
                    + ", completed=" + page.getVisibleTodoCompletionStates()
                    + ", remaining=" + page.getRemainingCountDisplayed()
                    + ", filter=" + page.getActiveFilterDisplayed();
        } catch (RuntimeException e) {
            actualDesc = "(unable to read actual state: " + e.getMessage() + ")";
        }

        // Trace file path is a placeholder hook: wire up Selenium 4 BiDi /
        // CDP tracing or a proxy-based recorder here if enabled for the run.
        Path traceFile = Path.of("target", "traces", framework.getId() + "-" + seed + ".trace.zip");

        TestReporter.reportFailure(
                framework.getId(),
                seed,
                executedHistory,
                actionIndex,
                expectedDesc,
                actualDesc,
                screenshot,
                traceFile,
                cause
        );
    }
}
