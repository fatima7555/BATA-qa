package pk.bata.qa.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import pk.bata.qa.core.Config;

import java.time.Duration;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared helpers. Locators are written as ordered fallbacks because themes change class names;
 * verify them once with DevTools on the live site (see README).
 */
public abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.TIMEOUT_S));
    }

    // ---------- navigation ----------
    public void go(String path) {
        driver.get(Config.BASE_URL + path);
        failIfBlocked();
        dismissOverlays();
    }

    /** Waits up to 10 s for a "verifying" page to finish by itself; stops the test if it does not. */
    protected void failIfBlocked() {
        long end = System.currentTimeMillis() + 10000;
        while (true) {
            String t;
            try {
                t = String.valueOf(((JavascriptExecutor) driver).executeScript(
                        "return document.body ? document.body.innerText : ''")).toLowerCase();
            } catch (WebDriverException e) { return; }
            boolean blocked = false;
            for (String k : new String[]{"needs to be verified", "just a moment", "verify you are human",
                    "are you a robot", "too many requests", "access denied", "unusual traffic", "checking your browser"}) {
                if (t.contains(k)) { blocked = true; break; }
            }
            if (!blocked) return;
            if (System.currentTimeMillis() > end)
                throw new pk.bata.qa.stats.SiteBlockedException(
                        "Verification page shown by the site - STOP, wait a few hours and tell your instructor");
            pause(1000);
        }
    }

    protected void dismissOverlays() {
        try {
            for (WebElement b : driver.findElements(By.cssSelector(
                    "#onetrust-accept-btn-handler, .popup-close, button[aria-label='Close popup'], [class*='newsletter'] [class*='close' i]"))) {
                if (b.isDisplayed()) jsClick(b);
            }
        } catch (WebDriverException ignored) { /* overlays are optional */ }
    }

    // ---------- element helpers ----------
    protected static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** First displayed element matching any locator (tried in order); polls until timeout. */
    protected WebElement find(By... locators) {
        WebElement e = findOrNull(Config.TIMEOUT_S, locators);
        if (e == null) throw new NoSuchElementException("No visible element for any of: " + Arrays.toString(locators));
        return e;
    }

    protected WebElement findOrNull(int seconds, By... locators) {
        long end = System.currentTimeMillis() + seconds * 1000L;
        do {
            for (By by : locators) {
                for (WebElement e : driver.findElements(by)) {
                    try { if (e.isDisplayed()) return e; } catch (StaleElementReferenceException ignored) { }
                }
            }
            pause(250);
        } while (System.currentTimeMillis() < end);
        return null;
    }

    protected boolean exists(int seconds, By... locators) { return findOrNull(seconds, locators) != null; }

    protected void jsClick(WebElement e) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", e); }

    protected void click(WebElement e) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", e);
        try { e.click(); } catch (WebDriverException ex) { jsClick(e); }
    }

    /** Select-all + delete works better than clear() on React-style inputs. */
    protected void setValue(WebElement e, String value) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", e);
        e.click();
        e.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        if (value != null && !value.isEmpty()) e.sendKeys(value);
    }

    /** Case-insensitive "contains text" XPath for links/buttons. */
    protected static By clickableWithText(String text) {
        return By.xpath("//*[self::a or self::button or self::input[@type='submit']]"
                + "[contains(translate(normalize-space(string(.)),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                + text.toLowerCase() + "')]");
    }

    protected static By headingWithText(String text) {
        return By.xpath("//*[self::h1 or self::h2 or self::h3 or self::legend or self::p]"
                + "[contains(translate(normalize-space(string(.)),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                + text.toLowerCase() + "')]");
    }

    // ---------- Shopify cart endpoints (same-origin, read from inside the page) ----------
    protected String cartJson() {
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
                "const cb = arguments[arguments.length-1];"
              + "fetch('/cart.js',{credentials:'same-origin'}).then(r=>r.text()).then(cb).catch(e=>cb('ERR'));");
        return String.valueOf(r);
    }

    /** Total quantity in cart (item_count), or -1 if it could not be read. */
    public int cartItemCount() {
        Matcher m = Pattern.compile("\"item_count\":(\\d+)").matcher(cartJson());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** Number of distinct line items in cart. */
    public int cartLineCount() {
        Matcher m = Pattern.compile("\"product_title\":").matcher(cartJson());
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    public boolean waitForCartCountAtLeast(int expected) { return waitForCartCountAtLeast(expected, Config.TIMEOUT_S); }

    public boolean waitForCartCountAtLeast(int expected, int seconds) {
        long end = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < end) {
            if (cartItemCount() >= expected) return true;
            pause(1500);
        }
        return false;
    }

    public void clearCartViaApi() {
        ((JavascriptExecutor) driver).executeAsyncScript(
                "const cb = arguments[arguments.length-1];"
              + "fetch('/cart/clear.js',{method:'POST',credentials:'same-origin'}).then(()=>cb('ok')).catch(()=>cb('ERR'));");
    }
}
