package pk.bata.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Bata's sign-up / login page: ONE email box, then a 6-digit code is emailed. */
public class AccountEntryPage extends BasePage {
    private static final By EMAIL = By.cssSelector(
            "input[type='email'], input[name='email'], input[autocomplete='email'], input[autocomplete='username']");

    public AccountEntryPage(WebDriver d) { super(d); }

    public AccountEntryPage open() { go("/account/login"); return this; }

    public void submitEmail(String email) {
        WebElement e = find(EMAIL);
        setValue(e, email);
        e.sendKeys(Keys.ENTER);
    }

    /** True if the "Enter code" screen appears within the given seconds. */
    public boolean codeScreenShown(int seconds) {
        return exists(seconds, By.xpath("//*[self::h1 or self::h2 or self::h3 or self::div or self::p or self::span]"
                + "[contains(translate(normalize-space(string(.)),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'enter code')]"));
    }
}
