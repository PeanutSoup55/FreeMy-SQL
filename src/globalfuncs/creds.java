package globalfuncs;

public class creds {
    public static String user = "";
    public static String pass = "";
    public static String url = "";

    public static String getUser(){
        return user;
    }
    public static String getPass(){
        return pass;
    }
    public static String getUrl(){
        return url;
    }

    public static void Display(){
        System.out.println(user + "\n" + pass + "\n" + url);
    }

}
