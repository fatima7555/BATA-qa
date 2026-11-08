package pk.bata.qa.stats;

public record Decision(String testId, int runs, int passes, int failures, boolean accepted) {
    @Override public String toString() {
        return testId + ": " + (accepted ? "ACCEPT" : "REJECT") + " (" + passes + "/" + runs + " passed, " + failures + " failed)";
    }
}
