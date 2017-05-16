package de.silas.lichtsteuerung;

/**
 * Created by Silas-M on 14.05.2017.
 */

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class Steckdose extends Button {

    public int id;
    public String name;
    public int status;
    public final Steckdose dieseKlasse;

    public Steckdose(Context context) {
        super(context);

        dieseKlasse = this;

        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("Button", "Klicked");
                if (!Einstellungen.nutzername.equals("") && !Einstellungen.passwort.equals("") && !Einstellungen.ip.equals("")) {
                    Log.e("Button", "If Passed");
                    starteSSHBefehl(Einstellungen.nutzername, Einstellungen.passwort, Einstellungen.ip, Einstellungen.port, getSteckdosenbefehl(), false);
                    dieseKlasse.setText(dieseKlasse.name + " - " + neuerStatus());
                    aenderStatus();
                }
            }
        });
    }

    public String getSteckdosenbefehl(){

        int neuerStatus = neuerStatus();

        return Einstellungen.befehlPre + Einstellungen.passwort + Einstellungen.befehlPost + " " + id + " " + neuerStatus;
    }

    private void aenderStatus(){
        status = neuerStatus();
    }

    private int neuerStatus(){
        // Erstelle einen neuen Status, abhängig von dem aktuellen Status.
        // -1 = Unbekannter Status => Schalte die Steckdose ein
        // 0 = Steckdose aus =>  Schalte die Steckdose ein
        // 1 = Steckdose ein =>  Schalte die Steckdose aus
        int neuerStatus = 0;

        if(status == -1)
            neuerStatus = 1;
        else if (status == 0)
            neuerStatus = 1;
        else if (status == 1)
            neuerStatus = 0;

        return neuerStatus;
    }

    public static void starteSSHBefehl(final String nutzername, final String passwort, final String ip, final int port, final String befehl, final boolean statusAbfrage){

        // Hintergrundaufgabe erstellen
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    // String, in den die ganzen Zeilen Ausgabe geschrieben werden
                    String ausgabe = "";

                    Log.e("Button", "SSH Started");

                    // Funktion ausführen
                    MainActivity.sshBefehl(nutzername, passwort, ip, port, befehl);


                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);

    }

}