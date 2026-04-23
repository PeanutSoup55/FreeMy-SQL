package globalfuncs;

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

}
