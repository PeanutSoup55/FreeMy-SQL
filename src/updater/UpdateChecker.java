package updater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Checks a version.json file hosted in your GitHub repo against the
 * current build's version string. Fails silently on any network issue —
 * an update prompt should never block someone from using the app offline.
 */
public class UpdateChecker {

    // Use the raw.githubusercontent.com URL for your repo/branch.
    private static final String VERSION_URL =
            "https://raw.githubusercontent.com/PeanutSoup55/FreeMy-SQL/main/version.json";

    // TODO: bump this constant with every release, or better, inject it at
    // build time (e.g. via a Maven resource filter) so it's not hand-edited.
    public static final String CURRENT_VERSION = "2.0.0";

    public record UpdateInfo(boolean updateAvailable, String latestVersion, String downloadUrl, String notes) {
    }

    public static UpdateInfo checkForUpdate() {
        try {
            URL url = new URL(VERSION_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
                String latest = json.get("latestVersion").getAsString();
                String downloadUrl = json.get("downloadUrl").getAsString();
                String notes = json.has("notes") ? json.get("notes").getAsString() : "";

                boolean isNewer = compareVersions(latest, CURRENT_VERSION) > 0;
                return new UpdateInfo(isNewer, latest, downloadUrl, notes);
            }
        } catch (Exception e) {
            return new UpdateInfo(false, CURRENT_VERSION, null, null);
        }
    }

    /** Simple dotted-version comparison: "2.10.0" > "2.9.0" correctly. */
    private static int compareVersions(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int va = i < pa.length ? Integer.parseInt(pa[i]) : 0;
            int vb = i < pb.length ? Integer.parseInt(pb[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    // Example usage at startup, on a background thread:
    //
    //   new Thread(() -> {
    //       UpdateInfo info = UpdateChecker.checkForUpdate();
    //       if (info.updateAvailable()) {
    //           Platform.runLater(() -> showUpdateBanner(info));
    //       }
    //   }).start();
}
