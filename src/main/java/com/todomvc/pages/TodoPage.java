package com.todomvc.pages;

import com.todomvc.enums.FilterType;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object for the TodoMVC app.
 *
 * All three reference implementations (React, Vue, Angular) render to the
 * SAME markup contract (this is TodoMVC's whole point - a shared CSS/DOM
 * spec: .new-todo, .todo-list li, .toggle, .destroy, .filters a, etc). This
 * class relies only on that shared contract, which is exactly why one page
 * object (and one test suite) works unchanged across all three frameworks.
 *
 * No fixed sleeps anywhere: every wait is an explicit, condition-based
 * WebDriverWait keyed off DOM state.
 */
public class TodoPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    private static final By NEW_TODO_INPUT = By.cssSelector(".new-todo");
    private static final By TODO_ITEMS = By.cssSelector(".todo-list li");
    private static final By TOGGLE_ALL = By.cssSelector(".toggle-all");
    private static final By CLEAR_COMPLETED = By.cssSelector(".clear-completed");
    private static final By TODO_COUNT = By.cssSelector(".todo-count");
    private static final By FILTER_ALL = By.cssSelector("[href='#/']");
    private static final By FILTER_ACTIVE = By.cssSelector("[href='#/active']");
    private static final By FILTER_COMPLETED = By.cssSelector("[href='#/completed']");

    public TodoPage(WebDriver driver, String baseUrl, int implicitWaitSeconds) {
        this.driver = driver;
        this.baseUrl = baseUrl;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(implicitWaitSeconds));
    }

    public void open() {
        driver.get(baseUrl);
        wait.until(ExpectedConditions.presenceOfElementLocated(NEW_TODO_INPUT));
    }

    public void reload() {
        driver.navigate().refresh();
        wait.until(ExpectedConditions.presenceOfElementLocated(NEW_TODO_INPUT));
    }

    // ---- actions ----

    public void addTodo(String text) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(NEW_TODO_INPUT));
        int before = getItemElements().size();
        input.sendKeys(text);
        input.sendKeys(Keys.ENTER);
        wait.until(d -> getItemElements().size() == before + 1);
    }

    public void editTodo(int visibleIndex, String newText) {
        WebElement item = getItemElements().get(visibleIndex);
        Actions builder = new Actions(driver);
        builder.doubleClick(item.findElement(By.cssSelector("label"))).perform();
        WebElement editInput = wait.until(d -> item.findElement(By.cssSelector(".edit")));
        editInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        editInput.sendKeys(Keys.DELETE);
        editInput.sendKeys(newText);
        editInput.sendKeys(Keys.ENTER);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".todo-list li.editing")));
    }

    public void toggleTodo(int visibleIndex) {
        WebElement item = getItemElements().get(visibleIndex);
        boolean wasCompleted = isItemCompleted(item);
        item.findElement(By.cssSelector(".toggle")).click();
        wait.until(d -> isItemCompleted(getItemElements().get(visibleIndex)) != wasCompleted);
    }

    public void toggleAll() {
        int before = getItemElements().size();
        WebElement toggleAll = wait.until(ExpectedConditions.elementToBeClickable(TOGGLE_ALL));
        toggleAll.click();
        // wait for all visible items to reach a consistent completed state
        wait.until(d -> {
            List<WebElement> items = getItemElements();
            if (items.isEmpty()) return true;
            boolean first = isItemCompleted(items.get(0));
            return items.stream().allMatch(i -> isItemCompleted(i) == first) && items.size() == before;
        });
    }

    public void deleteTodo(int visibleIndex) {
        List<WebElement> items = getItemElements();
        WebElement item = items.get(visibleIndex);
        int before = items.size();
        Actions builder = new Actions(driver);
        builder.moveToElement(item).perform();
        item.findElement(By.cssSelector(".destroy")).click();
        wait.until(d -> getItemElements().size() == before - 1);
    }

    public void clearCompleted() {
        List<WebElement> completedBefore = getItemElements().stream()
                .filter(this::isItemCompleted).collect(Collectors.toList());
        if (completedBefore.isEmpty()) {
            return; // nothing to clear, avoid clicking a non-existent/disabled control
        }
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(CLEAR_COMPLETED));
        btn.click();
        wait.until(d -> getItemElements().stream().noneMatch(this::isItemCompleted));
    }

    public void applyFilter(FilterType filter) {
        By locator;
        switch (filter) {
            case ACTIVE: locator = FILTER_ACTIVE; break;
            case COMPLETED: locator = FILTER_COMPLETED; break;
            default: locator = FILTER_ALL; break;
        }
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        wait.until(d -> d.findElement(locator).getAttribute("class") != null
                && d.findElement(locator).getAttribute("class").contains("selected"));
    }

    // ---- reads (actual state - compared against TodoModel, never used to build expectations) ----

    public List<String> getVisibleTodoTexts() {
        return getItemElements().stream()
                .map(e -> e.findElement(By.cssSelector("label")).getText())
                .collect(Collectors.toList());
    }

    public List<Boolean> getVisibleTodoCompletionStates() {
        return getItemElements().stream()
                .map(this::isItemCompleted)
                .collect(Collectors.toList());
    }

    public int getRemainingCountDisplayed() {
        try {
            String text = driver.findElement(TODO_COUNT).getText();
            String digits = text.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NoSuchElementException e) {
            return 0;
        }
    }

    public FilterType getActiveFilterDisplayed() {
        if (isSelected(FILTER_COMPLETED)) return FilterType.COMPLETED;
        if (isSelected(FILTER_ACTIVE)) return FilterType.ACTIVE;
        return FilterType.ALL;
    }

    public byte[] takeScreenshot() {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    // ---- helpers ----

    private boolean isSelected(By locator) {
        try {
            String cls = driver.findElement(locator).getAttribute("class");
            return cls != null && cls.contains("selected");
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private List<WebElement> getItemElements() {
        return driver.findElements(TODO_ITEMS);
    }

    private boolean isItemCompleted(WebElement item) {
        String cls = item.getAttribute("class");
        return cls != null && cls.contains("completed");
    }
}
