package pk.bata.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {
    private static final By EMAIL = By.cssSelector("input[name='customer[email]'], #CustomerEmail");
    private static final By PASSWORD = By.cssSelector("input[name='customer[password]'], #CustomerPassword");
    private static final By SUBMIT = By.cssSelector(
            "form[action$='/account/login'] button[type='submit'], form[action$='/account/login'] input[type='submit']");
    private static final By ERROR = By.cssSelector(".errors, .form__message, [class*='error' i], [role='alert']");
    private static final By LOGOUT = By.cssSelector("a[href*='/account/logout']");

    public LoginPage(WebDriver d) { super(d); }

    public LoginPage open() { go("/account/login"); return this; }

    public void login(String email, String password) {
        setValue(find(EMAIL), email);
        setValue(find(PASSWORD), password);
        click(find(SUBMIT));
        pause(2500);
    }

    public boolean hasError() { return exists(4, ERROR); }

    /** Logged in = /account lands on the customer profile/orders page, or the page offers "Sign out". */
    public boolean isLoggedIn() {
        go("/account");
        long end = System.currentTimeMillis() + 12000;
        while (System.currentTimeMillis() < end) {
            String url = driver.getCurrentUrl();
            if (url.contains("account.bata.com.pk") && (url.contains("/profile") || url.contains("/orders"))) return true;
            try {
                String t = String.valueOf(((JavascriptExecutor) driver).executeScript(
                        "return document.body ? document.body.innerText : ''")).toLowerCase();
                if (t.contains("sign out")) return true;
            } catch (WebDriverException ignored) { }
            pause(1000);
        }
        return false;
    }
}
