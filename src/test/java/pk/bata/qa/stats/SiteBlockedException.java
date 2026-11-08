package pk.bata.qa.stats;

/** Thrown when the site seems to be throttling or blocking automated traffic: the test STOPS (assignment ethics rule). */
public class SiteBlockedException extends RuntimeException {
    public SiteBlockedException(String message) { super(message); }
}
