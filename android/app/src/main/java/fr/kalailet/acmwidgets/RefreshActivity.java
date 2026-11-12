package fr.kalailet.acmwidgets;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Bouton Actualiser des widgets : la lecture se fait au premier plan (une activité invisible),
 * car Samsung coupe souvent internet aux applis en arrière-plan (« UnknownHostException »).
 */
public class RefreshActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final android.content.Context app = getApplicationContext();
        Widgets.refreshing(app, true);
        new Thread(() -> {
            String err = Store.fetch(app);
            Widgets.refreshing(app, false);
            final String msg = err != null ? "ACM Widgets : " + err : "ACM Widgets : à jour (" + Snap.ago(snapAt(app), System.currentTimeMillis()) + ")";
            runOnUiThread(() -> {
                Toast.makeText(app, msg, Toast.LENGTH_SHORT).show();
                finish();
                overridePendingTransition(0, 0);
            });
        }).start();
    }

    private static long snapAt(android.content.Context c) {
        Snap s = Store.snap(c);
        return s == null ? 0 : s.at();
    }
}
