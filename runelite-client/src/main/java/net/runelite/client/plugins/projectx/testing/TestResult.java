package net.runelite.client.plugins.projectx.testing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TestResult {

    public String timestamp;
    public String script;
    public long durationMs;
    public String exitReason;
    public int exitCode;
    public List<Check> checks = new ArrayList<>();
    public List<String> screenshots = new ArrayList<>();
    public String logTail;
    public List<String> errors = new ArrayList<>();

    private transient long startTime;

    public TestResult(String script) {
        this.script = script;
        this.timestamp = Instant.now().toString();
        this.startTime = System.currentTimeMillis();
    }

    public void addCheck(String name, boolean passed, String error) {
        checks.add(new Check(name, passed, error));
    }

    public void addError(String error) {
        errors.add(error);
    }

    public void addScreenshot(String path) {
        screenshots.add(path);
    }

    public void complete(String exitReason) {
        this.exitReason = exitReason;
        this.durationMs = System.currentTimeMillis() - startTime;

        long failures = checks.stream().filter(c -> !c.passed).count();
        if ("completed".equals(exitReason)) {
            this.exitCode = failures > 0 ? 1 : 0;
        } else if ("timeout".equals(exitReason)) {
            this.exitCode = 2;
        } else if ("crash".equals(exitReason)) {
            this.exitCode = 3;
        } else if ("login_failure".equals(exitReason)) {
            this.exitCode = 4;
        } else {
            this.exitCode = 1;
        }
    }

    public long passedCount() {
        return checks.stream().filter(c -> c.passed).count();
    }

    public long failedCount() {
        return checks.stream().filter(c -> !c.passed).count();
    }

    public static class Check {
        public String name;
        public boolean passed;
        public String error;

        public Check(String name, boolean passed, String error) {
            this.name = name;
            this.passed = passed;
            this.error = error;
        }
    }
}
