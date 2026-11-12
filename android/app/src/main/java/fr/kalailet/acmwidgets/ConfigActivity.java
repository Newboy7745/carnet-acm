package fr.kalailet.acmwidgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONObject;

/** Réglages d'un widget (à l'ajout ou via « Paramètres ») : séjour affiché et largeur. */
public class ConfigActivity extends Activity {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean loading = false;
    static final String[] EDGE = {"Standard", "Large", "Bord à bord (toute la largeur de l'écran)"};

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        Bundle ex = getIntent().getExtras();
        if (ex != null) widgetId = ex.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }
        build();
    }

    private void build() {
        ScrollView sv = new ScrollView(this);
        sv.setFitsSystemWindows(true); /* Android 15 : affichage bord à bord, le contenu reste sous la barre d'état */
        LinearLayout root = UI.column(this);
        sv.addView(root);
        root.addView(UI.title(this, "Quel séjour afficher ?"));
        if (Store.topic(this) == null) {
            root.addView(UI.muted(this, "Connectez d'abord ACM Widgets : dans Suivi ACM, Données, Widgets Android, touchez « Copier le code de connexion » puis collez-le ici."));
            final EditText et = UI.input(this, "Code de connexion");
            root.addView(et);
            Button b = UI.button(this, "Connecter", UI.NAVY);
            b.setOnClickListener(v -> {
                String err = Store.connect(this, et.getText().toString());
                if (err != null) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
                load();
            });
            root.addView(b);
            setContentView(sv);
            return;
        }
        Snap f = Store.snap(this);
        if (f == null) {
            root.addView(UI.muted(this, "Lecture des données… (ouvrez Suivi ACM une fois si rien ne vient)"));
            setContentView(sv);
            load();
            return;
        }
        final RadioGroup g = new RadioGroup(this);
        String cur = Store.stay(this, widgetId);
        g.addView(radio("Séjour en cours (automatique : en cours, sinon le prochain)", "", cur.isEmpty()));
        for (JSONObject s : f.stays) {
            String id = s.optString("id");
            g.addView(radio(s.optString("nom") + " (" + Widgets.subtitle(s, java.time.LocalDate.now()) + ")", id, id.equals(cur)));
        }
        if (g.getCheckedRadioButtonId() == -1) ((RadioButton) g.getChildAt(0)).setChecked(true);
        root.addView(g);

        root.addView(UI.text(this, "Largeur", 20));
        root.addView(UI.muted(this, "Si le widget ne prend pas toute la largeur de l'écran, choisissez « Large » ou « Bord à bord »."));
        final RadioGroup w = new RadioGroup(this);
        int e = Store.edge(this, widgetId);
        for (int i = 0; i < EDGE.length; i++) w.addView(radio(EDGE[i], String.valueOf(i), i == e));
        root.addView(w);

        Button ok = UI.button(this, "Valider", UI.ACCENT);
        ok.setOnClickListener(v -> {
            RadioButton rb = g.findViewById(g.getCheckedRadioButtonId()), rw = w.findViewById(w.getCheckedRadioButtonId());
            Store.setStay(this, widgetId, rb == null ? "" : String.valueOf(rb.getTag()));
            Store.setEdge(this, widgetId, rw == null ? 0 : Integer.parseInt(String.valueOf(rw.getTag())));
            AppWidgetManager m = AppWidgetManager.getInstance(this);
            Widgets.render(this, m, widgetId, typeOf(m));
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
            finish();
        });
        root.addView(ok);
        setContentView(sv);
    }

    private RadioButton radio(String label, String tag, boolean checked) {
        RadioButton r = new RadioButton(this);
        r.setId(View.generateViewId());
        r.setText(label);
        r.setTag(tag);
        r.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        r.setPadding(UI.dp(this, 8), UI.dp(this, 10), 0, UI.dp(this, 10));
        r.setChecked(checked);
        return r;
    }

    private void load() {
        if (loading) return;
        loading = true;
        new Thread(() -> {
            final String err = Store.fetch(getApplicationContext());
            runOnUiThread(() -> {
                loading = false;
                if (err != null) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                if (Store.snap(this) != null || Store.topic(this) == null) build();
            });
        }).start();
    }

    private Class<?> typeOf(AppWidgetManager m) {
        AppWidgetProviderInfo info = m.getAppWidgetInfo(widgetId);
        if (info != null && info.provider != null) {
            String cn = info.provider.getClassName();
            for (Class<?> t : Widgets.TYPES) if (t.getName().equals(cn)) return t;
        }
        return DashWidget.class;
    }
}
