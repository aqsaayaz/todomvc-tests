package com.todomvc.tests;

import com.todomvc.config.TestConfig;
import com.todomvc.generators.ActionGenerator;
import com.todomvc.utils.Action;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import java.util.List;

/**
 * The one test suite referenced in the requirements: it contains ZERO
 * framework-specific code or conditionals. Which app it points at is
 * controlled entirely by the "framework" system property (see testng.xml
 * / pom.xml), resolved once in BaseTodoTest.setUp() via TestConfig.
 *
 * Run identically against all three targets:
 *   mvn test -Dframework=react -Dseed=42
 *   mvn test -Dframework=vue   -Dseed=42
 *   mvn test -Dframework=angular -Dseed=42
 *
 * Or all three in one go via testng.xml's three <test> blocks (see file).
 */
@Epic("TodoMVC")
@Feature("Cross-framework seeded action replay")
public class CrossFrameworkTest extends BaseTodoTest {

    @Test(description = "30 seeded actions reproduce identically and app state matches the independent model after every step")
    @Description("Generates a deterministic action sequence from the configured seed, executes each action "
            + "against the live app, and asserts the app's visible todos, completion states, filter, and "
            + "remaining count exactly match an independent expected-state model after every single action.")
    public void seededActionSequenceMatchesModel() {
        ActionGenerator generator = new ActionGenerator(seed);
        List<Action> actions = generator.generate(TestConfig.getActionCount());

        for (Action action : actions) {
            executeAction(action);
        }
    }
}
