package com.khlif.app.paris;

/**
 * Created by anis on 07/05/16.
 */
public class Commons {

    static public String port = ":80";
    //public String server = "http://www.khlif.com"+port;
    static public String server = "http://10.21.12.162"+port;
    static public String SLASH = "/";
    static public String server_api = server + SLASH +"Paris/" ;

    static public String monuments = server + SLASH + "monuments/";

    static public String all_monuments = server_api + "monument/";

    static public String format = "format=json";

    static public String version = monuments + "version";

    static public BitmapCache bitmapCache = new BitmapCache();


    static public int CONNEXION_ERROR=-1;

    static public int Debug=2;

}
