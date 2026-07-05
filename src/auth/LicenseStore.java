package auth;

import java.time.Instant;
import java.util.prefs.Preferences;

/**
 * Local cache of auth/subscription state, using the same Preferences-based
 * persistence pattern already used elsewhere in the app (e.g. creds).
 *
 * This is what makes the 7-day offline grace period possible: we remember
 * the last time we successfully verified the subscription online, and allow
 * continued access for up to GRACE_PERIOD_MILLIS past that if we can't
 * reach Supabase.
 */
public class LicenseStore {

    private static final Preferences prefs = Preferences.userNodeForPackage(LicenseStore.class);

    private static final String KEY_TOKEN = "license_token";
    private static final String KEY_USER_ID = "license_user_id";
    private static final String KEY_EXPIRES_AT = "license_expires_at";
    private static final String KEY_LAST_VERIFIED = "license_last_verified";

    private static final long GRACE_PERIOD_MILLIS = 7L * 24 * 60 * 60 * 1000;

    /** Call after a successful login or a successful re-verification. */
    public static void save(String token, String userId, Instant expiresAt) {
        prefs.put(KEY_TOKEN, token);
        prefs.put(KEY_USER_ID, userId);
        prefs.put(KEY_EXPIRES_AT, expiresAt != null ? expiresAt.toString() : "");
        prefs.putLong(KEY_LAST_VERIFIED, System.currentTimeMillis());
    }

    public static void clear() {
        prefs.remove(KEY_TOKEN);
        prefs.remove(KEY_USER_ID);
        prefs.remove(KEY_EXPIRES_AT);
        prefs.remove(KEY_LAST_VERIFIED);
    }

    public static String getToken() {
        return prefs.get(KEY_TOKEN, null);
    }

    public static String getUserId() {
        return prefs.get(KEY_USER_ID, null);
    }

    public static boolean hasStoredSession() {
        return getToken() != null;
    }

    /** Call this whenever an online check succeeds, even if status is unchanged. */
    public static void markVerifiedNow() {
        prefs.putLong(KEY_LAST_VERIFIED, System.currentTimeMillis());
    }

    public static boolean isWithinGracePeriod() {
        long lastVerified = prefs.getLong(KEY_LAST_VERIFIED, 0);
        if (lastVerified == 0) return false;
        return (System.currentTimeMillis() - lastVerified) < GRACE_PERIOD_MILLIS;
    }

    /** For UI messaging — how many days of grace period are left. */
    public static long daysLeftInGracePeriod() {
        long lastVerified = prefs.getLong(KEY_LAST_VERIFIED, 0);
        if (lastVerified == 0) return 0;
        long elapsed = System.currentTimeMillis() - lastVerified;
        long remaining = GRACE_PERIOD_MILLIS - elapsed;
        return Math.max(0, remaining / (24 * 60 * 60 * 1000));
    }
}