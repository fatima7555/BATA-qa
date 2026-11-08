package pk.bata.qa.tests;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import pk.bata.qa.core.Config;
import pk.bata.qa.core.DriverFactory;
import pk.bata.qa.flows.Flows;
import pk.bata.qa.stats.Decision;
import pk.bata.qa.stats.RunOutcome;
import pk.bata.qa.stats.SequentialRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * 10 test cases = 5 use cases x (1 valid + 1 invalid). Each test case is executed 29 / 46 / 61 times
 * (see SequentialRunner) and the use case is accepted or rejected from the observed failures.
 * Every failed run saves a screenshot in results/screenshots.
 */
public class BataUseCaseTests {

    private static IntFunction<RunOutcome> run(String id, Supplier<WebDriver> browser, BiFunction<Integer, WebDriver, RunOutcome> flow) {
        return n -> {
            WebDriver d = browser.get();
            try {
                RunOutcome o = flow.apply(n, d);
                if (!o.pass()) shot(d, id, n);
                return o;
            } catch (RuntimeException e) {
                shot(d, id, n);
                throw e;
            } finally { d.quit(); }
        };
    }

    private static void shot(WebDriver d, String id, int n) {
        try {
            Path dir = Paths.get(Config.RESULTS_DIR, "screenshots");
            Files.createDirectories(dir);
            Files.copy(((TakesScreenshot) d).getScreenshotAs(OutputType.FILE).toPath(),
                    dir.resolve(id + "_run" + n + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) { /* screenshots are best-effort */ }
    }

    private static void verify(Decision d) { Assert.assertTrue(d.accepted(), "Use case REJECTED -> " + d); }

    @Test(priority = 1)
    public void tc01_registration_valid() {
        verify(SequentialRunner.execute("TC01", "UC1 Registration", "valid", run("TC01", DriverFactory::create, Flows::registerValid)));
    }

    @Test(priority = 2)
    public void tc02_registration_invalid() {
        verify(SequentialRunner.execute("TC02", "UC1 Registration", "invalid", run("TC02", DriverFactory::create, Flows::registerInvalid)));
    }

    @Test(priority = 3)
    public void tc03_loginOrder_valid() {
        WebDriver shared = LoginBootstrap.loginAndKeepOpen();     // one manual login, same window for all runs
        try {
            verify(SequentialRunner.execute("TC03", "UC2 Login + order", "valid", n -> {
                try {
                    RunOutcome o = Flows.loginOrderValid(n, shared);
                    if (!o.pass()) shot(shared, "TC03", n);
                    return o;
                } catch (RuntimeException e) { shot(shared, "TC03", n); throw e; }
            }));
        } finally { shared.quit(); }
    }

    @Test(priority = 4)
    public void tc04_loginOrder_invalid() {
        verify(SequentialRunner.execute("TC04", "UC2 Login + order", "invalid", run("TC04", DriverFactory::create, Flows::loginInvalid)));
    }

    @Test(priority = 5)
    public void tc05_guestOrder_valid() {
        verify(SequentialRunner.execute("TC05", "UC3 Guest order", "valid", run("TC05", DriverFactory::create, Flows::guestOrderValid)));
    }

    @Test(priority = 6)
    public void tc06_guestOrder_invalid() {
        verify(SequentialRunner.execute("TC06", "UC3 Guest order", "invalid", run("TC06", DriverFactory::create, Flows::guestOrderInvalid)));
    }

    @Test(priority = 7)
    public void tc07_cartExtract_valid() {
        verify(SequentialRunner.execute("TC07", "UC4 Cart extraction", "valid", run("TC07", DriverFactory::create, Flows::cartExtractValid)));
    }

    @Test(priority = 8)
    public void tc08_cartExtract_invalid() {
        verify(SequentialRunner.execute("TC08", "UC4 Cart extraction", "invalid", run("TC08", DriverFactory::create, Flows::cartExtractEmpty)));
    }

    @Test(priority = 9)
    public void tc09_continueShopping_valid() {
        verify(SequentialRunner.execute("TC09", "UC5 Continue shopping", "valid", run("TC09", DriverFactory::create, Flows::continueShoppingValid)));
    }

    @Test(priority = 10)
    public void tc10_continueShopping_invalid() {
        verify(SequentialRunner.execute("TC10", "UC5 Continue shopping", "invalid", run("TC10", DriverFactory::create, Flows::continueShoppingInvalid)));
    }

    /** Builds the summary table and plots automatically (needs Python + matplotlib + scipy; skipped quietly otherwise). */
    @AfterClass(alwaysRun = true)
    public void buildReport() {
        try {
            Process p = new ProcessBuilder("python", "analysis/analyze.py", Config.RESULTS_DIR + "/results.csv")
                    .inheritIO().start();
            p.waitFor();
        } catch (Exception e) {
            System.out.println("[report] could not run analysis/analyze.py automatically: " + e.getMessage());
        }
    }
}
