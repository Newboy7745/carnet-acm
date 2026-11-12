package fr.kalailet.acmwidgets;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/** Écran de l'app : connexion à Suivi ACM, état, aide pour ajouter les widgets. */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        handle(getIntent());
        build();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handle(intent);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Store.topic(this) != null && Store.stale(this)) refresh(false);
    }

    private void handle(Intent i) {
        Uri u = i == null ? null : i.getData();
        if (u != null && "acmwidgets".equals(u.getScheme())) connect(u.toString());
    }

    private void connect(String code) {
        String err = Store.connect(this, code);
        if (err != null) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
        Toast.makeText(this, "Connecté à Suivi ACM : lecture des données…", Toast.LENGTH_SHORT).show();
        refresh(true);
    }

    private void refresh(final boolean toast) {
        new Thread(() -> {
            final String err = Store.fetch(getApplicationContext());
            Widgets.updateAll(getApplicationContext());
            runOnUiThread(() -> {
                if (toast) Toast.makeText(this, err == null ? "Widgets à jour" : err, Toast.LENGTH_LONG).show();
                build();
            });
        }).start();
    }

    private void build() {
        ScrollView sv = new ScrollView(this);
        sv.setFitsSystemWindows(true); /* Android 15 : affichage bord à bord, le contenu reste sous la barre d'état */
        sv.setFillViewport(true);
        LinearLayout root = UI.column(this);
        sv.addView(root);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        head.addView(logo, new LinearLayout.LayoutParams(UI.dp(this, 64), UI.dp(this, 64)));
        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setPadding(UI.dp(this, 14), 0, 0, 0);
        ht.addView(UI.title(this, "ACM Widgets"));
        ht.addView(UI.muted(this, "Les widgets de Suivi ACM pour votre écran d'accueil"));
        head.addView(ht);
        root.addView(head);

        Snap f = Store.snap(this);
        LinearLayout st = UI.card(this);
        if (Store.topic(this) == null) {
            st.addView(UI.text(this, "Pas encore connecté", 18));
            st.addView(UI.muted(this, "Dans Suivi ACM : Données, Administration, Widgets Android, touchez « Connecter mes widgets ». Ou collez le code de connexion ci-dessous."));
        } else if (Store.off(this)) {
            st.addView(UI.text(this, "Widgets déconnectés", 18));
            st.addView(UI.muted(this, "Ils ont été déconnectés dans Suivi ACM. Pour les reconnecter : « Connecter mes widgets » dans Suivi ACM."));
        } else if (f == null) {
            st.addView(UI.text(this, "Connexion en cours…", 18));
            st.addView(UI.muted(this, "Ouvrez Suivi ACM une fois sur ce téléphone (les données partent à son ouverture), puis touchez « Actualiser maintenant »."));
        } else {
            st.addView(UI.text(this, "Connecté à Suivi ACM", 18));
            StringBuilder b = new StringBuilder();
            for (JSONObject s : f.stays) b.append(b.length() > 0 ? "\n" : "").append("- ").append(s.optString("nom")).append(" (").append(Snap.status(s, java.time.LocalDate.now()).toLowerCase(java.util.Locale.FRANCE)).append(")");
            st.addView(UI.muted(this, (b.length() > 0 ? "Séjours : \n" + b : "Aucun séjour en cours ou à venir")
                    + "\nDonnées de Suivi ACM : " + Snap.ago(f.at(), System.currentTimeMillis())
                    + "\nDernière vérification : " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(Store.checkAt(this)))
                    + "\nWidgets installés : " + Widgets.count(this) + Widgets.sizes(this)));
        }
        root.addView(st);

        LinearLayout co = UI.card(this);
        co.addView(UI.text(this, "Code de connexion", 17));
        final EditText code = UI.input(this, "Collez le code copié dans Suivi ACM");
        co.addView(code);
        Button paste = UI.button(this, "Coller et connecter", UI.NAVY);
        paste.setOnClickListener(v -> {
            String t = code.getText().toString().trim();
            if (t.isEmpty()) {
                ClipboardManager cm = getSystemService(ClipboardManager.class);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) { CharSequence x = cm.getPrimaryClip().getItemAt(0).coerceToText(this); t = x == null ? "" : x.toString(); }
            }
            connect(t);
        });
        co.addView(paste);
        Button upd = UI.button(this, "Actualiser maintenant", UI.ACCENT);
        upd.setOnClickListener(v -> refresh(true));
        co.addView(upd);
        root.addView(co);

        LinearLayout help = UI.card(this);
        help.addView(UI.text(this, "Ajouter un widget", 17));
        help.addView(UI.muted(this, "1. Appui long sur un espace vide de l'écran d'accueil\n2. « Widgets », puis « ACM Widgets »\n3. Choisissez : Tableau de bord (toute la page, 5 x 6), Appel (2 x 2), Budget (2 x 2) ou Planning (4 x 2)\n4. Choisissez le séjour (« Séjour en cours » par défaut) et la largeur\n\nPour changer plus tard : appui long sur le widget, puis « Paramètres ». Le bouton Actualiser du widget lit tout de suite les dernières données. Toucher un bouton ouvre Suivi ACM au bon endroit."));
        Button open = UI.button(this, "Ouvrir Suivi ACM", UI.NAVY);
        open.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Store.app(this) + "#rapide"))));
        help.addView(open);
        root.addView(help);

        TextView ver = UI.muted(this, "ACM Widgets " + versionName());
        ver.setGravity(Gravity.CENTER);
        root.addView(ver);
        setContentView(sv);
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }
}
