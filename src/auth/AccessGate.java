package auth;

import java.util.concurrent.CompletableFuture;

/**
 * Single entry point for "should this user get into the app right now?"
 * Call this at startup (after Login, or instead of showing Login at all if
 * a session is already cached).
 */
public class AccessGate {

    public enum Result {
        ALLOW_ONLINE,        // verified against Supabase just now
        ALLOW_GRACE_PERIOD,  // couldn't reach Supabase, but within 7 days of last success
        DENY_EXPIRED,        // reachable and subscription is not active, or grace period ran out
        DENY_NO_SESSION       // no cached session at all — show Login
    }

    public static CompletableFuture<Result> checkAccess() {
        if (!LicenseStore.hasStoredSession()) {
            return CompletableFuture.completedFuture(Result.DENY_NO_SESSION);
        }

        String userId = LicenseStore.getUserId();
        String token = LicenseStore.getToken();

        return AuthClient.verifySubscription(userId, token)
                .thenApply(active -> {
                    if (active == null) {
                        // Network unreachable — fall back to grace period.
                        return LicenseStore.isWithinGracePeriod()
                                ? Result.ALLOW_GRACE_PERIOD
                                : Result.DENY_EXPIRED;
                    }
                    if (active) {
                        LicenseStore.markVerifiedNow();
                        return Result.ALLOW_ONLINE;
                    }
                    return Result.DENY_EXPIRED;
                });
    }

    // Example usage in your main app startup:
    //
    //   AccessGate.checkAccess().thenAccept(result -> Platform.runLater(() -> {
    //       switch (result) {
    //           case ALLOW_ONLINE, ALLOW_GRACE_PERIOD -> showMainApp();
    //           case DENY_EXPIRED, DENY_NO_SESSION -> showLoginScreen();
    //       }
    //       if (result == AccessGate.Result.ALLOW_GRACE_PERIOD) {
    //           long daysLeft = LicenseStore.daysLeftInGracePeriod();
    //           // optionally show a small banner: "Offline — verify within X days"
    //       }
    //   }));
}