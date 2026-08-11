package com.todomvc.utils;

import com.todomvc.models.Todo;
import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;


/**
 * On any assertion failure, captures everything needed to reproduce and
 * diagnose it without re-running: seed, full action history up to and
 * including the failing action, expected state, actual state, a
 * screenshot, and a browser trace (via Selenium's logs / a saved trace
 * file if trace recording is enabled on the driver).
 *
 * Artifacts are written under target/failure-reports/<framework>/<seed>-<timestamp>/
 * and attached to the Allure report when Allure is active.
 */
public class TestReporter {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    public static void reportFailure(String framework,
                                      long seed,
                                      List<Action> actionHistory,
                                      int failingActionIndex,
                                      String expectedStateDescription,
                                      String actualStateDescription,
                                      byte[] screenshot,
                                      Path traceFile,
                                      Throwable cause) {
        try {
            String dirName = seed + "-" + LocalDateTime.now().format(TS);
            Path dir = Paths.get("target", "failure-reports", framework, dirName);
            Files.createDirectories(dir);

            StringBuilder report = new StringBuilder();
            report.append("=== TodoMVC Cross-Framework Test Failure Report ===\n");
            report.append("Framework: ").append(framework).append("\n");
            report.append("Seed: ").append(seed).append("\n");
            report.append("Failing action index: ").append(failingActionIndex).append("\n\n");

            report.append("--- Action history (in order) ---\n");
            for (int i = 0; i < actionHistory.size(); i++) {
                String marker = (i == failingActionIndex) ? " <-- FAILED HERE" : "";
                report.append(i).append(": ").append(actionHistory.get(i)).append(marker).append("\n");
            }

            report.append("\n--- Expected state ---\n").append(expectedStateDescription).append("\n");
            report.append("\n--- Actual state ---\n").append(actualStateDescription).append("\n");

            if (cause != null) {
                report.append("\n--- Exception ---\n").append(cause).append("\n");
            }

            Path reportFile = dir.resolve("report.txt");
            Files.writeString(reportFile, report.toString(), StandardCharsets.UTF_8);
            Allure.addAttachment("Failure report", "text/plain", report.toString(), ".txt");

            if (screenshot != null) {
                Path screenshotFile = dir.resolve("screenshot.png");
                Files.write(screenshotFile, screenshot);
                Allure.addAttachment("Screenshot", new ByteArrayInputStream(screenshot));
            }

            if (traceFile != null && Files.exists(traceFile)) {
                Path traceDest = dir.resolve("trace.zip");
                Files.copy(traceFile, traceDest);
                Allure.addAttachment("Trace", Files.newInputStream(traceDest));
            }

            System.err.println("Failure artifacts written to: " + dir.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("TestReporter failed to write failure artifacts: " + e.getMessage());
        }
    }

    public static String describeTodos(List<Todo> todos) {
        if (todos.isEmpty()) return "(empty)";
        StringBuilder sb = new StringBuilder();
        for (Todo t : todos) {
            sb.append(String.format("  [%s] %s%n", t.isCompleted() ? "x" : " ", t.getText()));
        }
        return sb.toString();
    }

    public static Path saveBrowserTrace(WebDriver driver,
                                        String framework,
                                        long seed) {
        try {
            Path traceDir = Paths.get("target", "traces");
            Files.createDirectories(traceDir);

            String timestamp = LocalDateTime.now().format(TS);

            Path traceJson = traceDir.resolve(
                    framework + "-" + seed + "-" + timestamp + "-performance.json"
            );

            StringBuilder output = new StringBuilder();
            output.append("{\n");
            output.append("  \"framework\": \"")
                    .append(framework)
                    .append("\",\n");
            output.append("  \"seed\": ")
                    .append(seed)
                    .append(",\n");
            output.append("  \"events\": [\n");

            var logs = driver.manage().logs().get(LogType.PERFORMANCE).getAll();

            for (int i = 0; i < logs.size(); i++) {
                output.append("    ")
                        .append(logs.get(i).getMessage());

                if (i < logs.size() - 1) {
                    output.append(",");
                }

                output.append("\n");
            }

            output.append("  ]\n");
            output.append("}\n");

            Files.writeString(
                    traceJson,
                    output.toString(),
                    StandardCharsets.UTF_8
            );

            Path traceZip = traceDir.resolve(
                    framework + "-" + seed + "-" + timestamp + ".trace.zip"
            );

            try (java.util.zip.ZipOutputStream zip =
                         new java.util.zip.ZipOutputStream(
                                 Files.newOutputStream(traceZip))) {

                zip.putNextEntry(
                        new java.util.zip.ZipEntry("performance-trace.json")
                );

                zip.write(
                        Files.readAllBytes(traceJson)
                );

                zip.closeEntry();
            }

            return traceZip;

        } catch (Exception e) {
            System.err.println(
                    "Unable to save browser trace: " + e.getMessage()
            );
            return null;
        }
    }
}
