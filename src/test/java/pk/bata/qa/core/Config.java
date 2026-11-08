package pk.bata.qa.core;

import java.util.Arrays;
import java.util.List;

/** All settings come from -D system properties or env vars (login.email -> LOGIN_EMAIL). */
public final class Config {
    private Config() {}

    private static String get(String key, String def) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) v = System.getenv(key.toUpperCase().replace('.', '_'));
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    public static final String BASE_URL = get("base.url", "https://www.bata.com.pk");
    public static final boolean HEADLESS = Boolean.parseBoolean(get("headless", "false"));
    public static final int MIN_DELAY_S = Integer.parseInt(get("min.delay", "30"));   // ethics rule: 3-10 s between runs
    public static final int MAX_DELAY_S = Integer.parseInt(get("max.delay", "45"));
    public static final int TIMEOUT_S = Integer.parseInt(get("timeout", "15"));
    public static final String RESULTS_DIR = get("results.dir", "results");

    /** Disposable-mailbox domain for generated registration emails. */
    public static final String EMAIL_DOMAIN = get("email.domain", "sharklasers.com");

    /** One disposable account you register once (needed for the login use case). */
    public static final String LOGIN_EMAIL = get("login.email", "");
    public static final String LOGIN_PASSWORD = get("login.password", "");

    /** Folder of a Chrome profile you logged into once by hand (used by TC03). */
    public static final String CHROME_PROFILE = get("chrome.profile", java.nio.file.Paths.get("chrome-profile").toAbsolutePath().toString());

    /** Product handles (the part after /products/). Refresh if items sell out. */
    public static final List<String> PRODUCTS = Arrays.asList(get("products",
            "851-4029,877-6016,874-6078,867-2081,677-4027,671-6142,677-6027,571-8030,677-7029,571-5030").split(","));
}