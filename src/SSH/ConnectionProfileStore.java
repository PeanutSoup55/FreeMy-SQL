package SSH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Persists saved SSH/DB connection profiles and a short "recent connections"
 * history to the OS-level Preferences store — same mechanism already used
 * for schema card drag positions. Plaintext storage, by design choice:
 * convenience over security for a local desktop tool.
 */
public class ConnectionProfileStore {

    private static final Preferences PROFILES = Preferences.userRoot().node("freemyquery/ssh_profiles");
    private static final Preferences HISTORY  = Preferences.userRoot().node("freemyquery/ssh_history");
    private static final String SEP = "\u0001";
    private static final int MAX_HISTORY = 5;

    private ConnectionProfileStore() {}

    public static class Profile {
        public String name, sshHost, sshPort, sshUser, sshPass, dbHost, dbPort, dbUser, dbPass, dbName;
    }

    public static List<String> listProfileNames() {
        try {
            List<String> names = new ArrayList<>(Arrays.asList(PROFILES.keys()));
            Collections.sort(names);
            return names;
        } catch (BackingStoreException e) {
            return new ArrayList<>();
        }
    }

    public static void save(Profile p) {
        String packed = String.join(SEP,
                p.sshHost, p.sshPort, p.sshUser, p.sshPass,
                p.dbHost, p.dbPort, p.dbUser, p.dbPass, p.dbName);
        PROFILES.put(p.name, packed);
    }

    public static Profile load(String name) {
        String packed = PROFILES.get(name, null);
        if (packed == null) return null;

        String[] parts = packed.split(SEP, -1);
        if (parts.length < 9) return null;

        Profile p = new Profile();
        p.name = name;
        p.sshHost = parts[0]; p.sshPort = parts[1]; p.sshUser = parts[2]; p.sshPass = parts[3];
        p.dbHost  = parts[4]; p.dbPort  = parts[5]; p.dbUser  = parts[6]; p.dbPass  = parts[7];
        p.dbName  = parts[8];
        return p;
    }

    public static void delete(String name) {
        PROFILES.remove(name);
    }

    /** Records a successful connection. Keeps only the most recent MAX_HISTORY entries. */
    public static void addHistory(String host, String dbName) {
        try {
            List<String> entries = new ArrayList<>();
            for (String k : HISTORY.keys()) entries.add(HISTORY.get(k, ""));

            entries.add(System.currentTimeMillis() + SEP + host + SEP + dbName);
            entries.sort((a, b) -> Long.compare(timestampOf(b), timestampOf(a)));

            if (entries.size() > MAX_HISTORY) {
                entries = entries.subList(0, MAX_HISTORY);
            }

            for (String k : HISTORY.keys()) HISTORY.remove(k);
            for (int i = 0; i < entries.size(); i++) HISTORY.put("h" + i, entries.get(i));
        } catch (BackingStoreException ignored) {}
    }

    /** Each entry: {host, dbName}, newest first. */
    public static List<String[]> getHistory() {
        List<String[]> result = new ArrayList<>();
        try {
            List<String> entries = new ArrayList<>();
            for (String k : HISTORY.keys()) entries.add(HISTORY.get(k, ""));
            entries.sort((a, b) -> Long.compare(timestampOf(b), timestampOf(a)));

            for (String e : entries) {
                String[] parts = e.split(SEP, -1);
                if (parts.length == 3) result.add(new String[]{parts[1], parts[2]});
            }
        } catch (BackingStoreException ignored) {}
        return result;
    }

    private static long timestampOf(String entry) {
        try {
            return Long.parseLong(entry.split(SEP, -1)[0]);
        } catch (Exception e) {
            return 0L;
        }
    }
}