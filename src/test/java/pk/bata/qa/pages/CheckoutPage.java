package pk.bata.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pk.bata.qa.data.DataFactory.Customer;
import pk.bata.qa.data.DataFactory.GuestField;

/**
 * SAFETY: this page object deliberately has NO method that locates or clicks the final
 * "Pay now" / "Complete order" button. Tests stop before payment, as the assignment requires.
 */
public class CheckoutPage extends BasePage {
    private static final By EMAIL = By.cssSelector("input[name='email'], input#email, input[autocomplete='email']");
    private static final By FIRST = By.cssSelector("input[name='firstName'], input[autocomplete='shipping given-name']");
    private static final By LAST = By.cssSelector("input[name='lastName'], input[autocomplete='shipping family-name']");
    private static final By ADDRESS = By.cssSelector("input[name='address1'], input[autocomplete='shipping address-line1']");
    private static final By CITY = By.cssSelector("input[name='city'], input[autocomplete='shipping address-level2']");
    private static final By POSTAL = By.cssSelector("input[name='postalCode'], input[autocomplete='shipping postal-code']");
    private static final By PHONE = By.cssSelector("input[name='phone'], input[autocomplete='shipping tel'], input[type='tel']");
    private static final By FIELD_ERROR = By.cssSelector("[id$='-error'], [role='alert'], [class*='field__message' i], [class*='error' i]");

    public CheckoutPage(WebDriver d) { super(d); }

    public boolean isLoaded() {
        long end = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < end) {
            if (driver.getCurrentUrl().contains("/checkout")) return exists(15, FIRST, EMAIL);
            pause(300);
        }
        return false;
    }

    private void fillIfPresent(By by, String v) {
        WebElement e = findOrNull(3, by);
        if (e != null) { setValue(e, v); pause(300); }
    }

    /** Phone formats tried in order; the first one the site accepts is remembered for all later runs. */
    private static int phoneStyle = 0;

    private static String phoneAs(String p, int style) {
        String ten = p.startsWith("0") ? p.substring(1) : p;       // 3xxxxxxxxx
        return switch (style) {
            case 0 -> p;                  // 03xxxxxxxxx
            case 1 -> ten;                // 3xxxxxxxxx
            case 2 -> "+92" + ten;        // +923xxxxxxxxx
            default -> "92" + ten;        // 923xxxxxxxxx
        };
    }

    /** Fills contact + shipping fields. Email is skipped for logged-in users when it is pre-filled. */
    public void fill(Customer c, boolean includeEmail) {
        if (includeEmail) fillIfPresent(EMAIL, c.email());
        fillIfPresent(FIRST, c.first());
        fillIfPresent(LAST, c.last());
        fillIfPresent(ADDRESS, c.address());
        fillIfPresent(CITY, c.city());
        fillIfPresent(POSTAL, c.postal());
        fillPhone(c.phone());
        pause(1000);
    }

    private void fillPhone(String phone) {
        WebElement e = findOrNull(3, PHONE);
        if (e == null) return;
        for (int style = phoneStyle; style < 4; style++) {
            setValue(e, phoneAs(phone, style));
            e.sendKeys(Keys.TAB);
            pause(900);
            if (!phoneErrorShownNow()) {
                if (style != phoneStyle) System.out.println("[checkout] phone format #" + style + " accepted: " + phoneAs(phone, style));
                phoneStyle = style;
                return;
            }
        }
        System.out.println("[checkout] no phone format was accepted; last tried: " + phoneAs(phone, 3));
    }

    private boolean phoneErrorShownNow() {
        String t = visibleErrorTextNow().toLowerCase();
        return t.contains("phone") || t.contains("mobile");
    }

    /** Overwrites one field with an invalid representative and blurs it so inline validation fires. */
    public void corrupt(GuestField field, String value) {
        By by = switch (field) {
            case EMAIL -> EMAIL;
            case FIRST_NAME -> FIRST;
            case ADDRESS -> ADDRESS;
            case PHONE -> PHONE;
        };
        WebElement e = find(by);
        setValue(e, value);
        e.sendKeys(Keys.TAB);
        pause(1200);
    }

    /** Text of all currently visible, non-empty error elements (no waiting). */
    private String visibleErrorTextNow() {
        StringBuilder sb = new StringBuilder();
        for (WebElement e : driver.findElements(FIELD_ERROR)) {
            try {
                if (e.isDisplayed()) {
                    String t = e.getText();
                    if (t != null && !t.isBlank()) sb.append(t.trim().replace("\n", " ")).append(" | ");
                }
            } catch (StaleElementReferenceException ignored) { }
        }
        return sb.toString();
    }

    /** True only if a visible element with NON-EMPTY text matches the error locators; prints that text. */
    public boolean hasVisibleFieldError() {
        long end = System.currentTimeMillis() + 4000;
        do {
            String t = visibleErrorTextNow();
            if (!t.isEmpty()) {
                System.out.println("[checkout] visible error text: " + t);
                return true;
            }
            pause(250);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    /** The payment section is displayed (we never interact with it). */
    public boolean paymentSectionVisible() { return exists(10, headingWithText("payment")); }
}
