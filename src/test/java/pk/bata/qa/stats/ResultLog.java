package pk.bata.qa.stats;

import pk.bata.qa.core.Config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.Instant;

public final class ResultLog {
    private ResultLog() {}

    private static String q(String s) { return "\"" + (s == null ? "" : s.replace("\"", "\"\"").replace("\n", " ")) + "\""; }

    private static synchronized void append(String file, String header, String line) {
        try {
            Path p = Paths.get(Config.RESULTS_DIR, file);
            Files.createDirectories(p.getParent());
            if (!Files.exists(p)) Files.writeString(p, header + System.lineSeparator());
            Files.writeString(p, line + System.lineSeparator(), StandardOpenOption.APPEND);
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static void run(String testId, String useCase, String kind, int run, RunOutcome o) {
        append("results.csv", "test_id,use_case,kind,run,result,value_used,detail,timestamp",
                String.join(",", testId, q(useCase), kind, String.valueOf(run), o.pass() ? "PASS" : "FAIL",
                        q(o.valueUsed()), q(o.detail()), Instant.now().toString()));
    }

    /** {passes, failures} already recorded for this test (lets an interrupted test resume). */
    public static synchronized int[] counts(String testId) {
        int pass = 0, fail = 0;
        Path p = Paths.get(Config.RESULTS_DIR, "results.csv");
        try {
            if (Files.exists(p)) {
                for (String line : Files.readAllLines(p)) {
                    if (!line.startsWith(testId + ",")) continue;
                    if (line.contains(",PASS,")) pass++;
                    else if (line.contains(",FAIL,")) fail++;
                }
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return new int[]{pass, fail};
    }

    public static void decision(Decision d) {
        append("decisions.csv", "test_id,runs,passes,failures,decision",
                String.join(",", d.testId(), String.valueOf(d.runs()), String.valueOf(d.passes()),
                        String.valueOf(d.failures()), d.accepted() ? "ACCEPT" : "REJECT"));
    }
}