package pk.bata.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class RegisterPage extends BasePage {
    private static final By FIRST = By.cssSelector("input[name='customer[first_name]'], #RegisterForm-FirstName");
    private static final By LAST = By.cssSelector("input[name='customer[last_name]'], #RegisterForm-LastName");
    private static final By EMAIL = By.cssSelector("input[name='customer[email]'], #RegisterForm-email");
    private static final By PASSWORD = By.cssSelector("input[name='customer[password]'], #RegisterForm-password");
    private static final By SUBMIT = By.cssSelector(
            "form[action$='/account'] button[type='submit'], form[action$='/account'] input[type='submit'], #create_customer [type='submit']");

    public RegisterPage(WebDriver d) { super(d); }

    public RegisterPage open() { go("/account/register"); return this; }

    public void fillAndSubmit(String first, String last, String email, String password) {
        WebElement f = findOrNull(2, FIRST);
        if (f != null) setValue(f, first);
        WebElement l = findOrNull(2, LAST);
        if (l != null) setValue(l, last);
        setValue(find(EMAIL), email);
        setValue(find(PASSWORD), password);
        click(find(SUBMIT));
        pause(2500);   // allow redirect / validation response
    }
}
