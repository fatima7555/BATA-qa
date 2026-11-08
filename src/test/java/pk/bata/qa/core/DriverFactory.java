package pk.bata.qa.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

public final class DriverFactory {
    private DriverFactory() {}

    /** A fresh, clean browser (no login, empty cart) - used for guest/cart/registration tests. */
    public static WebDriver create() { return build(false, false); }

    /** Visible browser, no saved profile - used for TC03 (one manual login, then reused for every run). */
    public static WebDriver createVisible() { return build(false, true); }

    /** Browser that keeps the saved login (Chrome profile folder). Always visible. */
    public static WebDriver createLoggedIn() { return build(true, false); }

    /** Visible browser with the saved profile, used for the one-time manual login. */
    public static WebDriver createForManualLogin() { return build(true, true); }

    private static WebDriver build(boolean withProfile, boolean forceVisible) {
        ChromeOptions o = new ChromeOptions();
        if (Config.HEADLESS && !withProfile && !forceVisible) o.addArguments("--headless=new");
        o.addArguments("--window-size=1366,900", "--lang=en-US");
        if (withProfile) o.addArguments("--user-data-dir=" + Config.CHROME_PROFILE);
        WebDriver d = new ChromeDriver(o);   // Selenium Manager downloads the matching driver
        d.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        d.manage().timeouts().scriptTimeout(Duration.ofSeconds(20));
        return d;
    }
}
