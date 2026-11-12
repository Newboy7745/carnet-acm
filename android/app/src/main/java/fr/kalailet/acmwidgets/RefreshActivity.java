package fr.kalailet.acmwidgets;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Bouton Actualiser des widgets : la lecture se fait au premier plan (petite fenêtre visible « Actualisation des widgets… »),
 * car Samsung coupe souvent internet aux applis en arrière-plan (« UnknownHostException »).
 */
public class RefreshActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        android.widget.TextView t = UI.text(this, "Actualisation des widgets…", 17);
        int p = UI.dp(this, 24);
        t.setPadding(p, p, p, p);
        setContentView(t);
        final android.content.Context app = getApplicationContext();
        Widgets.refreshing(app, true);
        new Thread(() -> {
            String err = Store.fetch(app);
            Widgets.refreshing(app, false);
            final String msg = err != null ? "ACM Widgets : " + err : "ACM Widgets : à jour (" + Snap.ago(snapAt(app), System.currentTimeMillis()) + ")";
            runOnUiThread(() -> {
                Toast.makeText(app, msg, Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    private static long snapAt(android.content.Context c) {
        Snap s = Store.snap(c);
        return s == null ? 0 : s.at();
    }
}
