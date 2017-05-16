package de.silas.lichtsteuerung;

public class Einstellungen {

    public static String nutzername;
    public static String passwort;
    public static String ip;
    public static int port;
    public static final String befehlPre = "echo ";
    public static final String befehlPost = " | sudo -S ./Funksteckdosen-RaspberryPi/funk";
    public static final String befehlStatusabfrage = "cat ./Funksteckdosen-RaspberryPi/status.json";

}
