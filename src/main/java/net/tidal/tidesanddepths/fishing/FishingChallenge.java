package net.tidal.tidesanddepths.fishing;

final class FishingChallenge {
    private final int bobberEntityId;
    private boolean resolved;
    private boolean successful;

    FishingChallenge(int bobberEntityId) {
        this.bobberEntityId = bobberEntityId;
    }

    int bobberEntityId() {
        return bobberEntityId;
    }

    boolean resolved() {
        return resolved;
    }

    void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    boolean successful() {
        return successful;
    }

    void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
