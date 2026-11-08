package pk.bata.qa.stats;

/** Result of ONE execution of a test case. */
public record RunOutcome(boolean pass, String valueUsed, String detail) {
    public static RunOutcome fail(String valueUsed, String detail) { return new RunOutcome(false, valueUsed, detail); }
}
