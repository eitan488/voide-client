package voidclient.events;

public abstract class Cancellable {
    private boolean cancelled;

    public void cancel() { cancelled = true; }
    public boolean isCancelled() { return cancelled; }
}
