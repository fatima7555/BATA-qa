package pk.bata.qa.flows;

import org.openqa.selenium.WebDriver;
import pk.bata.qa.core.Config;
import pk.bata.qa.data.DataFactory;
import pk.bata.qa.data.DataFactory.*;
import pk.bata.qa.pages.*;
import pk.bata.qa.stats.RunOutcome;
import pk.bata.qa.stats.SiteBlockedException;

import java.util.ArrayList;
import java.util.List;

/**
 * One method = one execution of one test case. Each returns PASS when the observed behaviour matches the
 * expected outcome. For INVALID test cases, PASS means the site correctly rejected the bad input.
 */
public final class Flows {
    private Flows() {}

    // ===== helpers =====

    private static boolean looksBlocked(WebDriver d) {
        try {
            Object o = ((org.openqa.selenium.JavascriptExecutor) d)
                    .executeScript("return document.body ? document.body.innerText : ''");
            String t = String.valueOf(o).toLowerCase();       // only text a visitor can actually see
            for (String k : new String[]{"too many requests", "verify you are human", "are you a robot",
                    "access denied", "unusual traffic", "temporarily blocked", "checking your browser"}) {
                if (t.contains(k)) return true;
            }
        } catch (RuntimeException ignored) { }
        return false;
    }

    /**
     * Adds a product to the cart. Tries up to 4 different sizes per product and at most 3 products per run.
     * If the site looks like it is throttling/blocking us, throws SiteBlockedException (the test stops).
     */
    static String addProduct(WebDriver d, int run, int offset) {
        List<String> products = Config.PRODUCTS;
        int triedProducts = 0;
        for (int i = 0; i < products.size() && triedProducts < 3; i++) {
            String handle = products.get((run + offset + i) % products.size());
            ProductPage pp = new ProductPage(d).open(handle);
            if (looksBlocked(d)) throw new SiteBlockedException("blocking/CAPTCHA page detected on " + handle + " - stop and tell your instructor");
            if (pp.isSoldOut()) {
                System.out.println("[add] " + handle + " is sold out - trying next product");
                continue;
            }
            triedProducts++;
            int before = pp.cartItemCount();
            if (before < 0) throw new SiteBlockedException("cart endpoint unreadable (site may be throttling requests) - wait 30+ minutes");
            for (int attempt = 0; attempt < 4; attempt++) {
                String size = pp.selectSize(run + attempt);
                pp.clickAddToCart();
                if (pp.waitForCartCountAtLeast(before + 1, 6)) return handle + "/size=" + size;
                if (looksBlocked(d)) throw new SiteBlockedException("blocking/CAPTCHA page appeared after add-to-cart on " + handle);
                if (size.equals("n/a")) break;
            }
            System.out.println("[add] could not add " + handle + " (all sizes tried) - trying next product");
        }
        throw new IllegalStateException("could not add any of 3 products to the cart - check results/screenshots");
    }

    private static RunOutcome checkoutValid(WebDriver d, Customer c, boolean includeEmail, String value) {
        CartPage cart = new CartPage(d).open();
        cart.clickCheckout();
        CheckoutPage co = new CheckoutPage(d);
        if (!co.isLoaded()) return RunOutcome.fail(value, "checkout page did not load; url=" + d.getCurrentUrl());
        co.fill(c, includeEmail);
        boolean noErrors = !co.hasVisibleFieldError();
        boolean payment = co.paymentSectionVisible();
        boolean ok = noErrors && payment;                      // STOP here: never press Pay / Complete order
        return new RunOutcome(ok, value, ok ? "reached payment stage, fields accepted"
                : "noFieldErrors=" + noErrors + ", paymentVisible=" + payment);
    }

    // ===== UC1 registration (Bata uses email-code sign-up: we stop at the "Enter code" screen) =====

    public static RunOutcome registerValid(int run, WebDriver d) {
        Customer c = DataFactory.customer(run);
        AccountEntryPage p = new AccountEntryPage(d).open();
        p.submitEmail(c.email());
        boolean ok = p.codeScreenShown(12);
        return new RunOutcome(ok, c.email(), ok ? "valid email accepted, verification-code screen reached (stopped before the code)"
                : "no code screen; url=" + d.getCurrentUrl());
    }

    public static RunOutcome registerInvalid(int run, WebDriver d) {
        BadEmail b = DataFactory.badEmail(run);
        AccountEntryPage p = new AccountEntryPage(d).open();
        p.submitEmail(b.email());
        boolean codeSent = p.codeScreenShown(5);
        return new RunOutcome(!codeSent, b.label() + " | '" + b.email() + "'",
                codeSent ? "BUG: invalid email accepted" : "rejected as expected");
    }

    // ===== UC2 login + order (stop before payment); uses a Chrome profile that is already logged in =====

    public static RunOutcome loginOrderValid(int run, WebDriver d) {
        LoginPage lp = new LoginPage(d);
        lp.go("/");
        lp.clearCartViaApi();
        if (!lp.isLoggedIn())
            return RunOutcome.fail("profile=" + Config.CHROME_PROFILE, "not logged in - log in once inside the saved Chrome profile");
        String item = addProduct(d, run, 0);
        return checkoutValid(d, DataFactory.customer(run), false, item);
    }

    public static RunOutcome loginInvalid(int run, WebDriver d) {
        BadEmail b = DataFactory.badEmail(run);
        AccountEntryPage p = new AccountEntryPage(d).open();
        p.submitEmail(b.email());
        boolean codeSent = p.codeScreenShown(5);
        return new RunOutcome(!codeSent, b.label() + " | '" + b.email() + "'",
                codeSent ? "BUG: invalid email accepted at login" : "rejected as expected");
    }

    // ===== UC3 guest order (stop before payment) =====

    public static RunOutcome guestOrderValid(int run, WebDriver d) {
        String item = addProduct(d, run, 2);
        return checkoutValid(d, DataFactory.customer(run), true, item);
    }

    public static RunOutcome guestOrderInvalid(int run, WebDriver d) {
        BadGuest b = DataFactory.badGuest(run);
        String item = addProduct(d, run, 2);
        new CartPage(d).open().clickCheckout();
        CheckoutPage co = new CheckoutPage(d);
        if (!co.isLoaded()) return RunOutcome.fail(b.label(), "checkout page did not load");
        co.fill(DataFactory.customer(run), true);
        co.corrupt(b.field(), b.value());
        boolean error = co.hasVisibleFieldError();
        return new RunOutcome(error, b.label() + " | value='" + b.value() + "' | " + item,
                error ? "inline validation error shown" : "BUG: invalid value accepted silently");
    }

    // ===== UC4 shopping cart extraction =====
    // Items are put in the cart through Bata's own cart API (fewer page loads = gentler on the site);
    // the use case under test - reading the cart page and all its items - is done through the page itself.

    private static final String ADD_VIA_API = """
            const cb = arguments[arguments.length - 1];
            const handle = arguments[0], pick = arguments[1];
            fetch('/products/' + handle + '.js', {credentials: 'same-origin'})
              .then(r => { if (!r.ok) { cb('ERR' + r.status); return null; } return r.json(); })
              .then(p => {
                if (!p) return;
                const avail = (p.variants || []).filter(v => v.available);
                if (!avail.length) { cb('SOLDOUT'); return; }
                const v = avail[pick % avail.length];
                return fetch('/cart/add.js', {method: 'POST', credentials: 'same-origin',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({items: [{id: v.id, quantity: 1}]})})
                  .then(r => cb(r.ok ? 'OK:' + (v.title || v.id) : 'ERR' + r.status));
              })
              .catch(e => cb('ERR'));
            """;

    /** Adds one in-stock variant of a product through the cart API. Returns "handle/variant". */
    static String addViaApi(WebDriver d, int run, int offset) {
        List<String> products = Config.PRODUCTS;
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) d;
        for (int i = 0; i < Math.min(4, products.size()); i++) {
            String handle = products.get((run + offset + i) % products.size());
            String r = String.valueOf(js.executeAsyncScript(ADD_VIA_API, handle, run + i));
            if (r.startsWith("OK")) return handle + "/" + r.substring(3);
            if (r.equals("ERR403") || r.equals("ERR429"))
                throw new SiteBlockedException("site refused the cart request (HTTP " + r.substring(3) + ") - STOP");
            System.out.println("[add] " + handle + ": " + r + " - trying next product");
        }
        throw new IllegalStateException("could not add any product through the cart API");
    }

    public static RunOutcome cartExtractValid(int run, WebDriver d) {
        int k = 1 + (run % 3);                                  // 1, 2 or 3 distinct products
        List<String> items = new ArrayList<>();
        CartPage cart = new CartPage(d).open();
        for (int i = 0; i < k; i++) items.add(addViaApi(d, run, i * 3));
        cart.open();
        List<CartPage.CartLine> lines = cart.extractLines();
        int api = cart.cartLineCount();
        boolean ok = lines.size() == api && api >= 1 && api <= k && lines.stream().noneMatch(l -> l.title().isBlank());
        return new RunOutcome(ok, "k=" + k + " " + items, "extracted=" + lines.size() + ", cart.js lines=" + api + " -> " + lines);
    }

    /**
     * Invalid class = EMPTY cart. Three different ways of reaching an empty cart are used in turn
     * (new session / cookies cleared / direct link with a tracking parameter). Only page loads, no cart requests.
     */
    public static RunOutcome cartExtractEmpty(int run, WebDriver d) {
        CartPage cart = new CartPage(d);
        String how;
        if (run % 3 == 0) {
            how = "fresh_session";
            cart.open();
        } else if (run % 3 == 1) {
            how = "cookies_cleared";
            cart.open();
            d.manage().deleteAllCookies();
            cart.open();
        } else {
            how = "link_with_tracking_param";
            cart.go("/cart?ref=qa" + run);
        }
        List<CartPage.CartLine> lines = cart.extractLines();
        boolean ok = lines.isEmpty() && cart.emptyMessageShown();
        return new RunOutcome(ok, how, "extracted=" + lines.size() + ", emptyMessage=" + cart.emptyMessageShown());
    }

    // ===== UC5 continue shopping =====

    public static RunOutcome continueShoppingValid(int run, WebDriver d) {
        String item = addProduct(d, run, 4);
        CartPage cart = new CartPage(d).open();
        boolean moved = cart.clickContinueShopping();
        int count = cart.cartItemCount();
        boolean ok = moved && count >= 1;
        return new RunOutcome(ok, item, "movedToCollection=" + moved + ", cartItemCount=" + count + ", url=" + d.getCurrentUrl());
    }

    public static RunOutcome continueShoppingInvalid(int run, WebDriver d) {
        String q = DataFactory.badQuantity(run);
        List<String> products = Config.PRODUCTS;
        ProductPage pp = new ProductPage(d).open(products.get((run + 4) % products.size()));
        pp.selectSize(run);
        pp.setQuantity(q);
        pp.clickAddToCart();
        pp.waitForCartCountAtLeast(1, 4);
        int count = pp.cartItemCount();
        CartPage cart = new CartPage(d).open();
        boolean moved = cart.clickContinueShopping();
        // Invalid quantity must be rejected (cart stays empty) or normalised to a valid quantity of 1 - never stored as entered.
        boolean ok = (count == 0 || count == 1) && moved;
        String how = count == 0 ? "rejected" : (count == 1 ? "corrected to 1" : "unexpected quantity");
        return new RunOutcome(ok, "quantity='" + q + "'", how + ", cartItemCount=" + count + ", continueShoppingWorked=" + moved);
    }
}