package SSH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ConnectionProfileStore {

    private static final Preferences PROFILES = Preferences.userRoot().node("freemyquery/ssh_profiles");
    private static final String SEP = "\u0001";

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
}