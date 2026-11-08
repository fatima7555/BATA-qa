package pk.bata.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pk.bata.qa.core.Config;

import java.util.ArrayList;
import java.util.List;

public class CartPage extends BasePage {
    public record CartLine(String title, String quantity, String price) {}

    private static final By[] ROW_LOCATORS = {
            By.cssSelector("tr.cart-item"), By.cssSelector("[id^='CartItem-']"),
            By.cssSelector(".cart-item"), By.cssSelector("[data-cart-item]")};
    private static final By TITLE = By.cssSelector("a[href*='/products/']");
    private static final By QTY = By.cssSelector("input[name^='updates'], input.quantity__input, input[type='number']");
    private static final By PRICE = By.cssSelector("[class*='price' i]");
    private static final By CHECKOUT = By.cssSelector(
            "button[name='checkout'], input[name='checkout'], #checkout, a[href$='/checkout']");

    public CartPage(WebDriver d) { super(d); }

    public CartPage open() { go("/cart"); return this; }

    /** Use Case 4: read every visible line item from the cart page. */
    public List<CartLine> extractLines() {
        List<CartLine> out = new ArrayList<>();
        for (By rowBy : ROW_LOCATORS) {
            List<WebElement> rows = new ArrayList<>();
            for (WebElement r : driver.findElements(rowBy)) if (r.isDisplayed()) rows.add(r);
            if (rows.isEmpty()) continue;
            for (WebElement r : rows) {
                String title = textOf(r, TITLE);
                if (title.isBlank()) title = r.getText().split("\n")[0].trim();
                WebElement q = r.findElements(QTY).stream().findFirst().orElse(null);
                String qty = q == null ? "1" : q.getAttribute("value");
                out.add(new CartLine(title, qty, textOf(r, PRICE)));
            }
            break;
        }
        return out;
    }

    private String textOf(WebElement root, By by) {
        for (WebElement e : root.findElements(by)) {
            String t = e.getText();
            if (t != null && !t.isBlank()) return t.trim();
        }
        return "";
    }

    public boolean emptyMessageShown() { return exists(4, headingWithText("your cart is empty")); }

    public void clickCheckout() {
        WebElement c = findOrNull(Config.TIMEOUT_S, CHECKOUT);
        if (c == null) c = find(clickableWithText("check out"));
        click(c);
    }

    /** Use Case 5: click "Continue shopping" and confirm we land on a collection page. */
    public boolean clickContinueShopping() {
        WebElement c = findOrNull(6, clickableWithText("continue shopping"));
        if (c == null) return false;
        click(c);
        long end = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < end) {
            if (driver.getCurrentUrl().contains("/collections")) return true;
            pause(300);
        }
        return false;
    }
}
