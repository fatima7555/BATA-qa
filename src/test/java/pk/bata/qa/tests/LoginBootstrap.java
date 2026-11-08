package pk.bata.qa.tests;

import org.openqa.selenium.WebDriver;
import pk.bata.qa.core.Config;
import pk.bata.qa.core.DriverFactory;

/**
 * Bata logs in with an emailed 6-digit code, which a script cannot read. So TC03 needs ONE manual login:
 * a Chrome window opens, you log in (type your email, copy the code from the inbox), and TC03 then keeps
 * that SAME window open and reuses the logged-in session for every run.
 */
public final class LoginBootstrap {
    private LoginBootstrap() {}

    public static WebDriver loginAndKeepOpen() {
        WebDriver d = DriverFactory.createVisible();
        d.get(Config.BASE_URL + "/account/login");
        System.out.println("\n=====================================================================");
        System.out.println(" ACTION NEEDED: a Chrome window opened. Log in to Bata there:");
        System.out.println("   1. type your test email   2. open the email inbox   3. type the 6-digit code");
        System.out.println(" You have 10 minutes. DO NOT close this window - the test keeps using it.");
        System.out.println("=====================================================================\n");
        long end = System.currentTimeMillis() + 10 * 60 * 1000L;
        while (System.currentTimeMillis() < end) {
            String url = d.getCurrentUrl();
            if (url.contains("account.bata.com.pk") && (url.contains("/profile") || url.contains("/orders"))) {
                try { Thread.sleep(3000); } catch (InterruptedException ignored) { }
                System.out.println("[setup] login detected - starting the runs.");
                return d;
            }
            try { Thread.sleep(2000); } catch (InterruptedException ignored) { }
        }
        d.quit();
        throw new IllegalStateException("Manual login was not completed within 10 minutes.");
    }
}
