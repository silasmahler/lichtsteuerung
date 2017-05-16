package de.silas.lichtsteuerung;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    Steckdose[] steckdosen;
    private Einstellungen einstellungen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //shared data vorbereiten
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        // Rufe die Funktion auf, die die Startseite anzeigt.
        zeigeStart();

    }

    // Zeigt die Startseite an.
    private void zeigeStart() {

        // Zeige die Startseite an.
        setContentView(R.layout.activity_main);

        // Verbindungsdaten laden.
        einstellungen.nutzername = pref.getString("nutzername", "");
        einstellungen.passwort = pref.getString("passwort", "");
        einstellungen.ip = pref.getString("ip", "");
        einstellungen.port = pref.getInt("port", 22);

        // Gib eine Benachrichtigung aus, falls noch keine Verbindungsdaten angegeben sind.
        benachrichtige();

        // Prüft, ob eines der Felder für die Verbindungsdaten unvollständig ist.
        if (pref.getInt("port", 0) == 0 || pref.getString("ip", "").equals("") || pref.getString("passwort", "").equals("") || pref.getString("nutzername", "").equals("")) {

            // Rufe die Einstellungsseite auf, um Verbindungsdaten angeben zu können.
            zeigeEinstellungen();

        } else {

            // Da bereits Verbindungsdaten angegeben sind kann die App nun per SSH auf den Pi zugreifen und die Datei "status.json" auslesen,
            // welche die Anzahl und Namen der Steckdosen enthält.
            starteSSHAbfrage(Einstellungen.nutzername, Einstellungen.passwort, Einstellungen.ip, Einstellungen.port, Einstellungen.befehlStatusabfrage);

        }
    }

    // Zeigt die Einstellungsseite an.
    public void zeigeEinstellungen() {
        setContentView(R.layout.activity_einstellungen);

        // Den Textfeldern Namen geben, um den Inhalt schreiben und auslesen zu können.
        final EditText edittext_nutzername = (EditText) findViewById(R.id.edittext_nutzername);
        final EditText edittext_ip = (EditText) findViewById(R.id.edittext_ip);
        final EditText edittext_passwort = (EditText) findViewById(R.id.edittext_passwort);
        final EditText edittext_port = (EditText) findViewById(R.id.edittext_port);
        Button speichern_button = (Button) findViewById(R.id.button_speichern);

        // Fehlerabfrage
        if (edittext_ip != null && edittext_nutzername != null && edittext_passwort != null && edittext_port != null && speichern_button != null) {

            // Textfelder mit bereits bestehenden Einstellungen befüllen.
            edittext_ip.setText(pref.getString("ip", ""));
            edittext_nutzername.setText(pref.getString("nutzername", ""));
            edittext_passwort.setText(pref.getString("passwort", ""));
            edittext_port.setText(pref.getInt("port", 22) + "");

        }

        // Auf den Klick, auf den Knopf "Speichern" reagieren.
        speichern_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Variablen vorbereiten, um den Inhalt der Textfelder unterspeichern zu können.
                String nutzername = "";
                String passwort = "";
                String ip = "";
                String port = "";
                // Inhalt der Textfelder in Variablen schreiben.
                nutzername = edittext_nutzername.getText().toString();
                ip = edittext_ip.getText().toString();
                passwort = edittext_passwort.getText().toString();
                port = edittext_port.getText().toString();
                if (!port.equals("") && !ip.equals("") && !nutzername.equals("") && !passwort.equals("")) {
                    // Verbindungsdaten laden
                    editor.putString("nutzername", nutzername);
                    editor.putString("passwort", passwort);
                    editor.putString("ip", ip);
                    editor.putInt("port", Integer.parseInt(port));
                    // Einstellungen speichern
                    editor.commit();
                    // Gehe bei erfolgreicher Speicherung zur Startseite zurück
                    zeigeStart();
                } else {
                    // Fehlermeldung ausgeben, falls eines der Felder frei war.
                    Toast.makeText(MainActivity.this, "Es scheinen noch Felder frei zu sein.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    // Definiert, dass es ein OptionMenü gibt und wie dieses aussehen soll. (Nach Vorlage der Datei "actionbar_manu.xml")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_menu, menu);
        return true;
    }

    // Definiert, wie auf Klicks im OptionMenü reagiert werden soll.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_start:
                // Dieser Teil (bis zum "return") wird ausgeführt, wenn wir auf den Menüpunkt mit der ID "menu_start" klicken.
                zeigeStart();
                return true;
            case R.id.menu_ueber:
                // Dieser Teil (bis zum "return") wird ausgeführt, wenn wir auf den Menüpunkt mit der ID "menu_ueber" klicken.
                DialogFragment newFragment = new UeberDialog();
                newFragment.show(this.getFragmentManager(), "Über");
                return true;
            case R.id.menu_einstellungen:
                // Dieser Teil (bis zum "return") wird ausgeführt, wenn wir auf den Menüpunkt mit der ID "menu_einstellungen" klicken.
                zeigeEinstellungen();
                return true;
            default:
                // Standardfunktion. Sollte bestehen bleiben.
                return super.onOptionsItemSelected(item);
        }
    }

    // Kleine Klasse zum Erstellen eines Dialogs.
    public static class UeberDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //Dialog erstellen
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            //Befülle Dialog mit dem Text "ueber_text" aus der Datei res -> strings.xml
            builder.setMessage(R.string.ueber_text)
                    //Erstelle einen Button mit dem Text "ok"  aus der Datei res -> strings.xml
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //Schließe das Dialogfeld, beim Klick auf den erstellten Button
                            dismiss();
                        }
                    });
            //Gib das erstellte Dialogfeld an die aufrufende Klasse zurück, um es anzeigen zu können.
            return builder.create();
        }
    }

    // Zeigt eine Benachrichtigung an, falls zu dem Zeitpunkt des Starts noch keine Verbindungsdaten angegeben wurde.
    public void benachrichtige() {
        // Prüft, ob eines der Felder für die Verbindungsdaten unvollständig ist.
        if (pref.getInt("port", 0) == 0 || pref.getString("ip", "").equals("") || pref.getString("passwort", "").equals("") || pref.getString("nutzername", "").equals("")) {
            Toast.makeText(MainActivity.this, "Verbindungsdaten unvollständig. Menü -> Einstellungen", Toast.LENGTH_LONG).show();
        }

    }

    // Die Datei "status.json" wurde über SSH in die App eingelesen.
// Anhand der enthaltenen Informationen werden nun die entsprechenden Button auf der Startseite erstellt.
    public void createButton(String status) {

        // Wähle das LinearLayout aus, um in dieses dynamisch Button für jede Steckdose erstellen zu können.
        LinearLayout hauptfeld = (LinearLayout) findViewById(R.id.hauptfeld);

        try {

            // Lese die "status.json"-Datei ordnungsgemäß ein.
            JSONObject statusObjekt = new JSONObject(status);
            JSONArray steckdosenArray = new JSONArray(statusObjekt.getString("outlets"));

            // Mache die Liste der Steckdosen so lang, wie es auch Steckdosen gibt.
            steckdosen = new Steckdose[steckdosenArray.length()];

            // Gib jedem Objekt "Steckdose" in der Liste der Steckdosen ihren Status, ihre ID und ihren Namen.
            for (int j = 0; j < steckdosenArray.length(); ++j) {
                JSONObject jsonProdukt = steckdosenArray.getJSONObject(j);
                steckdosen[j] = new Steckdose(this);
                steckdosen[j].id = j;
                steckdosen[j].name = jsonProdukt.getString("name");
                steckdosen[j].status = jsonProdukt.getInt("status");
            }

            // Gehe ein zweites mal durch alle Einträge der Steckdosenliste.
            // Dieses mal sind alle Steckdosen richtig definiert, deshalb geben wir ihnen ihren Text und
            // fügen sie in das LinearLayout "hauptfeld" hinzu.
            for (int i = 0; i < steckdosen.length; i++) {

                // Gib der Steckdose einen Namen
                steckdosen[i].setText(steckdosen[i].name + " - " + steckdosen[i].status);

                // Für die Steckdose zum LinearLayout hinzu.
                hauptfeld.addView(steckdosen[i]);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    // Kleine Zusatzfunktion, die den eigentlichen SSH Befehl asynchron im Hintergrund startet.
    public void starteSSHAbfrage(final String nutzername, final String passwort, final String ip, final int port, final String befehl) {

        // Hintergrundaufgabe erstellen
        new AsyncTask<Integer, Void, String>() {
            @Override
            protected String doInBackground(Integer... params) {
                // String, in den die ganzen Zeilen Ausgabe geschrieben werden
                String ausgabe = "";

                try {

                    // Funktion ausführen und Konsolenausgabe in "lines" speichern.
                    ArrayList<String> lines = sshBefehl(nutzername, passwort, ip, port, befehl);

                    // Alle Zeilen der Konsolenausgabe in den Android Logs ausgeben.
                    while (!lines.isEmpty()) {
                        //Ausgabe in der Konsole
                        Log.e("Rückgabe", lines.get(0));

                        //Hänge momentane Zeile an den String "ausgabe" an
                        ausgabe += lines.get(0);

                        //Zeile entfernen
                        lines.remove(0);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return ausgabe;
            }

            protected void onPostExecute(String ausgabe) {
                // Sende die Ausgabe an die Funktion "createButton", um nach dem empfangenen Status
                // die Button zum Schalten der einzelnen Steckdosen erstellen zu können.
                createButton(ausgabe);
            }
        }.execute(1);

    }

    // Dies ist die eigentliche Funktion, die den SSH Befehl ausführt.
    public static ArrayList<String> sshBefehl(String username, String passwort, String hostname, int port, String befehl) throws Exception {

        Log.e("SSH Befehl", "Started - Befehl: " + befehl);

        // Erstelle das benötigte Objekt, um eine SSH-Verbindung aufbauen zu können.
        JSch jsch = new JSch();

        // Bereite ein paar Variablen vor, um Ausgaben der Konsole auslesen zu können.
        byte[] buffer = new byte[1024];
        ArrayList<String> lines = new ArrayList<>();

        // Füttere das Objekt mit allen nötigen Informationen, um eine Verbindung aufbauen zu können.
        Session session = jsch.getSession(username, hostname, port);
        session.setPassword(passwort);

        // Umgehe das Abgleichen nach dem richtigen Key. (Es wird nun eine Man-In-Middle Attacke nicht mehr abgefangen)
        Properties prop = new Properties();
        prop.put("StrictHostKeyChecking", "no");
        session.setConfig(prop);

        // Stelle eine Verbindung her.
        session.connect();

        // Erstelle ein neues Objekt, für einen neuen SSH Channel.
        ChannelExec channelssh = (ChannelExec) session.openChannel("exec");

        // Füttere den Channel mit dem Befehl und schicke den Befehl ab.
        channelssh.setCommand(befehl);
        channelssh.connect();

        // Fange an die Ausgaben der Konsole auszulesen.
        try {
            InputStream in = channelssh.getInputStream();
            String line = "";

            // Lese alle Ausgaben aus, bis der Befehl beendet wurde oder die Verbindung abbricht.
            while (true) {

                // Schreibe jede Zeile der Konsolenausgabe in unser Array.
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);

                    // Brich die Protokollierung der Ausgabe, für diese Zeile, ab, wenn die Ausgabe leer sein sollte.
                    if (i < 0) {
                        break;
                    }
                    line = new String(buffer, 0, i);
                    lines.add(line);
                }

                // Wir wurden ausgeloggt.
                if (line.contains("logout")) {
                    break;
                }

                // Befehl beendet oder Verbindung abgebrochen.
                if (channelssh.isClosed()) {
                    break;
                }

                // Warte einen kleinen Augenblick mit der nächsten Zeile.
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
        } catch (Exception e) {
        }

        // Beende alle Verbindungen.
        channelssh.disconnect();
        session.disconnect();

        // Gib die Ausgabe zurück
        return lines;
    }

}
