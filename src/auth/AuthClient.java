package auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Talks to Supabase Auth + the `subscriptions` table over plain HTTP.
 * No Stripe calls happen here — the app only ever asks "is this user active?"
 *
 * Requires the Gson dependency, e.g. in Maven:
 *   <dependency>
 *     <groupId>com.google.code.gson</groupId>
 *     <artifactId>gson</artifactId>
 *     <version>2.11.0</version>
 *   </dependency>
 */
public class AuthClient {

    private static final String SUPABASE_URL = "https://gksjnbicrycggdwaasxc.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imdrc2puYmljcnljZ2dkd2Fhc3hjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI5OTg1MTgsImV4cCI6MjA5ODU3NDUxOH0.LCWxrffBJSj_gOzNy8AN6UY7o7807l5DVw7-yXOGGUM";

    public record AuthResult(boolean success, String message, String accessToken,
                             String userId, boolean subscriptionActive, Instant expiresAt) {
    }

    /**
     * Attempts sign-in; on failure (when signUp=true) falls back to creating
     * the account. Mirrors the toggle already in Login.java's account pane.
     */
    public static CompletableFuture<AuthResult> authenticate(String email, String password, boolean signUp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = signUp
                        ? "/auth/v1/signup"
                        : "/auth/v1/token?grant_type=password";

                JsonObject body = new JsonObject();
                body.addProperty("email", email);
                body.addProperty("password", password);

                JsonObject authResponse = postJson(endpoint, body.toString());

                if (authResponse.has("error_description") || authResponse.has("msg")) {
                    String msg = authResponse.has("error_description")
                            ? authResponse.get("error_description").getAsString()
                            : authResponse.get("msg").getAsString();
                    return new AuthResult(false, msg, null, null, false, null);
                }

                if (!authResponse.has("access_token")) {
                    // Sign-up succeeded but email confirmation is required —
                    // there's no session yet.
                    return new AuthResult(false, "Check your email to confirm your account.",
                            null, null, false, null);
                }

                String accessToken = authResponse.get("access_token").getAsString();
                String userId = authResponse.getAsJsonObject("user").get("id").getAsString();

                JsonObject subRow = getSubscriptionRow(userId, accessToken);
                boolean active = false;
                Instant expiresAt = null;

                if (subRow != null) {
                    String status = subRow.has("status") ? subRow.get("status").getAsString() : "inactive";
                    active = "active".equals(status) || "trialing".equals(status);
                    if (subRow.has("current_period_end") && !subRow.get("current_period_end").isJsonNull()) {
                        expiresAt = Instant.parse(subRow.get("current_period_end").getAsString());
                    }
                }

                LicenseStore.save(accessToken, userId, expiresAt);
                if (active) LicenseStore.markVerifiedNow();

                return new AuthResult(true, "OK", accessToken, userId, active, expiresAt);

            } catch (Exception e) {
                return new AuthResult(false, "Network error: " + e.getMessage(), null, null, false, null);
            }
        });
    }

    /**
     * Re-checks subscription status for an already-authenticated user.
     * Returns null if the check couldn't be completed (network unreachable) —
     * callers should treat null as "unknown," not "inactive," so the grace
     * period logic in LicenseStore can decide what to do.
     */
    public static CompletableFuture<Boolean> verifySubscription(String userId, String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject subRow = getSubscriptionRow(userId, accessToken);
                if (subRow == null) return false;
                String status = subRow.has("status") ? subRow.get("status").getAsString() : "inactive";
                return "active".equals(status) || "trialing".equals(status);
            } catch (IOException e) {
                return null; // network unreachable — let the caller decide
            }
        });
    }

    private static JsonObject getSubscriptionRow(String userId, String accessToken) throws IOException {
        URL url = new URL(SUPABASE_URL + "/rest/v1/subscriptions?user_id=eq." + userId
                + "&select=status,current_period_end");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);
        conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        String responseText = readResponse(conn);
        var arr = JsonParser.parseString(responseText).getAsJsonArray();
        if (arr.isEmpty()) return null;
        return arr.get(0).getAsJsonObject();
    }

    private static JsonObject postJson(String endpointPath, String jsonBody) throws IOException {
        URL url = new URL(SUPABASE_URL + endpointPath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        String responseText = readResponse(conn);
        return JsonParser.parseString(responseText).getAsJsonObject();
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        var stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }


    public record SubscriptionDetails(String status, Instant currentPeriodEnd) {}

    public static CompletableFuture<String> getUserEmail(String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/auth/v1/user");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                JsonObject obj = JsonParser.parseString(readResponse(conn)).getAsJsonObject();
                return obj.has("email") ? obj.get("email").getAsString() : null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    public static CompletableFuture<SubscriptionDetails> getSubscriptionDetails(String userId, String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject row = getSubscriptionRow(userId, accessToken);
                if (row == null) return null;
                String status = row.has("status") ? row.get("status").getAsString() : "inactive";
                Instant expires = (row.has("current_period_end") && !row.get("current_period_end").isJsonNull())
                        ? Instant.parse(row.get("current_period_end").getAsString()) : null;
                return new SubscriptionDetails(status, expires);
            } catch (IOException e) {
                return null;
            }
        });
    }

    public static CompletableFuture<String> createBillingPortalSession(String accessToken, String returnUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/functions/v1/create-portal-session");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setDoOutput(true);

                JsonObject body = new JsonObject();
                body.addProperty("return_url", returnUrl);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                JsonObject response = JsonParser.parseString(readResponse(conn)).getAsJsonObject();
                return response.has("url") ? response.get("url").getAsString() : null;
            } catch (Exception e) {
                return null;
            }
        });
    }
}
