package globalfuncs;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.Arrays;

public class creds {
    public static String initials = "";
    public static String user = "";
    public static String pass = "";
    public static String url = "";

    public static String getUser(){
        return user;
    }
    public static String getPass(){return pass;}
    public static String getUrl(){
        return url;
    }
    public static String getInitials(){return initials;}

    public static void setInitials(String initials) {
        creds.initials = initials;
    }

    public static void setUser(String user) {
        creds.user = user;
    }

    public static void setPass(String pass) {
        creds.pass = pass;
    }

    public static void setUrl(String url) {
        creds.url = url;
    }

    public static void Display(){
        System.out.println(user + "\n" + pass + "\n" + url);
    }

    private static final Preferences ROOT_PREFS = Preferences.userRoot().node("Free_My_SQL/saved_login");
    private static final Preferences PROFILES_NODE = ROOT_PREFS.node("profiles");

    private static final String KEY_URL = "server_url";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_INITIALS = "user_initials";
    private static final String KEY_LAST_PROFILE = "last_used_profile";
    private static final String KEY_REMEMBER_ME = "remember_me";

    public static void saveProfile(String profileName, String url, String username, String initials) {
        Preferences profileNode = PROFILES_NODE.node(profileName);
        profileNode.put(KEY_URL, url);
        profileNode.put(KEY_USERNAME, username);
        profileNode.put(KEY_INITIALS, initials);
        ROOT_PREFS.put(KEY_LAST_PROFILE, profileName);
        flushPrefs();
    }

    public static String[] loadProfile(String profileName) {
        if (profileName == null || profileName.isEmpty()) {
            return new String[]{"", "", ""};
        }
        Preferences profileNode = PROFILES_NODE.node(profileName);
        return new String[]{
                profileNode.get(KEY_URL, ""),
                profileNode.get(KEY_USERNAME, ""),
                profileNode.get(KEY_INITIALS, "")
        };
    }

    public static String[] getAllProfileNames() {
        try {
            String[] children = PROFILES_NODE.childrenNames();
            if (children.length == 0) {
                return new String[]{"Default Profile"};
            }
            Arrays.sort(children);
            return children;
        } catch (BackingStoreException e) {
            return new String[]{"Default Profile"};
        }
    }

    public static String getLastUsedProfile() {
        return ROOT_PREFS.get(KEY_LAST_PROFILE, "Default Profile");
    }

    public static void setRememberMe(boolean enabled) {
        ROOT_PREFS.putBoolean(KEY_REMEMBER_ME, enabled);
        flushPrefs();
    }

    public static boolean isRememberMeEnabled() {
        return ROOT_PREFS.getBoolean(KEY_REMEMBER_ME, false);
    }

    public static void removeProfile(String profileName) {
        try {
            if (PROFILES_NODE.nodeExists(profileName)) {
                PROFILES_NODE.node(profileName).removeNode();
                flushPrefs();
            }
        } catch (BackingStoreException ignored) {}
    }

    private static void flushPrefs() {
        try {
            ROOT_PREFS.flush();
        } catch (BackingStoreException e) {
            System.err.println("Preferences flush failed: " + e.getMessage());
        }
    }

}
