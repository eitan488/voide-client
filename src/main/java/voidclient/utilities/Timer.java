package voidclient.utilities;

/** Millisecond stopwatch. */
public final class Timer {
    private long start = System.currentTimeMillis();

    public void reset() { start = System.currentTimeMillis(); }
    public long elapsed() { return System.currentTimeMillis() - start; }
    public boolean passed(long ms) { return elapsed() >= ms; }

    public boolean passedAndReset(long ms) {
        if (!passed(ms)) return false;
        reset();
        return true;
    }
}
