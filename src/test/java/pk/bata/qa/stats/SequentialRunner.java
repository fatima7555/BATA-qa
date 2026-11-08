package pk.bata.qa.stats;

import pk.bata.qa.core.Config;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntFunction;

/**
 * Binomial acceptance sampling for quality goal p0 = 90 % at 95 % confidence.
 *
 *   checkpoint n = 29 -> accept if failures <= 0
 *   checkpoint n = 46 -> accept if failures <= 1
 *   checkpoint n = 61 -> accept if failures <= 2, otherwise reject
 *
 * Start with 29 runs. If the checkpoint is not met, keep running up to the next checkpoint.
 * More than 2 failures at any time can no longer be accepted, so the use case is rejected immediately.
 * If the test was interrupted earlier (for example by a site block), it resumes from the runs already recorded.
 */
public final class SequentialRunner {
    private static final int[] N = {29, 46, 61};
    private static final int[] ALLOWED = {0, 1, 2};

    private SequentialRunner() {}

    public static Decision execute(String testId, String useCase, String kind, IntFunction<RunOutcome> oneRun) {
        int[] prior = ResultLog.counts(testId);
        int passes = prior[0], failures = prior[1];
        int done = passes + failures;

        for (int s = 0; s < N.length; s++) {                       // already finished earlier?
            if (done == N[s] && failures <= ALLOWED[s]) return finish(new Decision(testId, done, passes, failures, true));
        }
        if (failures > ALLOWED[ALLOWED.length - 1]) return finish(new Decision(testId, done, passes, failures, false));
        int stage = 0;
        while (stage < N.length - 1 && done >= N[stage]) stage++;
        if (done > 0) System.out.println("[" + testId + "] resuming after " + done + " earlier runs");

        for (int run = done + 1; ; run++) {
            RunOutcome o;
            try {
                o = oneRun.apply(run);
            } catch (SiteBlockedException b) {                      // a site block is NOT a test failure: do not record it
                System.out.println("[" + testId + "] STOPPED: " + b.getMessage()
                        + "  (run " + run + " not counted; run this test again later to continue)");
                throw b;
            } catch (Throwable t) {                                 // other errors count as failures
                o = RunOutcome.fail("", t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()).split("\n")[0]);
            }
            if (o.pass()) passes++; else failures++;
            ResultLog.run(testId, useCase, kind, run, o);
            System.out.println("[" + testId + "] run " + run + ": " + (o.pass() ? "PASS" : "FAIL") + "  " + o.detail());

            Decision d = null;
            if (failures > ALLOWED[ALLOWED.length - 1]) {
                d = new Decision(testId, run, passes, failures, false);
            } else if (run == N[stage]) {
                if (failures <= ALLOWED[stage]) d = new Decision(testId, run, passes, failures, true);
                else if (stage == N.length - 1) d = new Decision(testId, run, passes, failures, false);
                else stage++;
            }
            if (d != null) return finish(d);
            politeDelay();
        }
    }

    private static Decision finish(Decision d) { ResultLog.decision(d); return d; }

    /** Ethics rule: wait between runs so traffic never looks like an attack. */
    private static void politeDelay() {
        long ms = ThreadLocalRandom.current().nextLong(Config.MIN_DELAY_S * 1000L, Config.MAX_DELAY_S * 1000L + 1);
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}