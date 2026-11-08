package pk.bata.qa.pages;

import org.openqa.selenium.*;

import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;

public class ProductPage extends BasePage {
    private static final By ADD = By.cssSelector(
            "button[name='add'], form[action*='/cart/add'] button[type='submit'], button.product-form__submit");
    private static final By SIZE_OPTIONS = By.cssSelector(
            "fieldset input[type='radio']:not([disabled]), [class*='swatch' i] input[type='radio']:not([disabled]), "
                    + "[class*='size' i] input[type='radio']:not([disabled])");
    private static final By QTY = By.cssSelector("input[name='quantity']");

    public ProductPage(WebDriver d) { super(d); }

    public ProductPage open(String handle) { go("/products/" + handle); return this; }

    public boolean isSoldOut() {
        WebElement add = findOrNull(6, ADD);
        return add == null || !add.isEnabled() || add.getText().toLowerCase().contains("sold out");
    }

    /** Radios that belong to the main product (not to "You may also like" cards). */
    private List<WebElement> mainRadios() {
        List<WebElement> out = new ArrayList<>();
        for (WebElement r : driver.findElements(By.cssSelector("input[type='radio']"))) {
            try {
                if (!r.isEnabled()) continue;
                Object inCard = ((JavascriptExecutor) driver).executeScript(
                        "return arguments[0].closest('[class*=\"card\"],[class*=\"recommend\"],[class*=\"related\"],[class*=\"recently\"]') !== null;", r);
                if (!Boolean.TRUE.equals(inCard)) out.add(r);
            } catch (WebDriverException ignored) { }
        }
        return out;
    }

    private void choose(WebElement radio) {
        try {
            String id = radio.getAttribute("id");
            if (id != null && !id.isEmpty()) {
                for (WebElement l : driver.findElements(By.cssSelector("label[for='" + id + "']"))) {
                    if (l.isDisplayed()) { click(l); pause(300); if (radio.isSelected()) return; }
                }
            }
        } catch (WebDriverException ignored) { }
        jsClick(radio);
    }

    /**
     * Picks a different available SIZE on each run (index rotates) and returns it, or "n/a" if the product has no size.
     * Size options are recognised by value like "41/7" or by a name containing "size"; colours are never picked as a size.
     */
    public String selectSize(int run) {
        List<WebElement> sizes = new ArrayList<>();
        List<WebElement> others = new ArrayList<>();
        for (WebElement r : mainRadios()) {
            String v = r.getAttribute("value");
            String n = String.valueOf(r.getAttribute("name")).toLowerCase();
            boolean looksSize = (v != null && v.trim().matches("\\d{1,2}(\\.\\d)?(/\\d{1,2}(\\.\\d)?)?")) || n.contains("size");
            (looksSize ? sizes : others).add(r);
        }
        List<WebElement> pool = !sizes.isEmpty() ? sizes : others;
        if (!pool.isEmpty()) {
            WebElement o = pool.get(Math.floorMod(run, pool.size()));
            choose(o);
            pause(400);
            String v = o.getAttribute("value");
            return v == null ? "?" : v;
        }
        // fallback: size shown as a dropdown
        for (WebElement sel : driver.findElements(By.cssSelector("form[action*='/cart/add'] select"))) {
            try {
                if (!sel.isDisplayed()) continue;
                Select s = new Select(sel);
                List<WebElement> choices = new ArrayList<>();
                for (WebElement o : s.getOptions()) {
                    String v = o.getAttribute("value");
                    if (o.isEnabled() && v != null && !v.isEmpty()) choices.add(o);
                }
                if (choices.isEmpty()) continue;
                WebElement o = choices.get(Math.floorMod(run, choices.size()));
                s.selectByValue(o.getAttribute("value"));
                pause(400);
                return o.getText().trim();
            } catch (WebDriverException ignored) { }
        }
        return "n/a";
    }

    /** Sets the quantity field directly (used for invalid-quantity representatives). */
    public void setQuantity(String q) {
        List<WebElement> cands = driver.findElements(By.cssSelector(
                "form[action*='/cart/add'] input[name='quantity'], input[name='quantity'], input[name='qty'], "
                        + "input[id*='quantity' i], input[class*='quantity' i], input[class*='qty' i], input[type='number']"));
        WebElement target = null;
        for (WebElement e : cands) {
            try { if (e.isDisplayed()) { target = e; break; } } catch (WebDriverException ignored) { }
        }
        if (target == null && !cands.isEmpty()) target = cands.get(0);
        if (target == null) throw new NoSuchElementException("quantity input not found on the product page");
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                        + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", target, q);
        pause(400);
    }

    public void clickAddToCart() {
        click(find(ADD));
        pause(1500);
    }
}