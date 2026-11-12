package fr.kalailet.acmwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/** Dessin des 4 widgets à partir du dernier résumé (aucun accès réseau ici). */
final class Widgets {
    static final Class<?>[] TYPES = {DashWidget.class, AppelWidget.class, BudgetWidget.class, PlanningWidget.class};
    static final String[] NAMES = {"Tableau de bord", "Appel", "Budget", "Planning"};
    static final int[] P = {R.id.p0, R.id.p1, R.id.p2, R.id.p3}, D = {R.id.d0, R.id.d1, R.id.d2, R.id.d3}, A = {R.id.a0, R.id.a1, R.id.a2, R.id.a3};
    static final int RED = 0xFFF2705F, WHITE = 0xFFFFFFFF;
    static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE);

    private Widgets() { }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (Class<?> t : TYPES) for (int id : m.getAppWidgetIds(new ComponentName(c, t))) render(c, m, id, t);
    }

    static int count(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int n = 0;
        for (Class<?> t : TYPES) n += m.getAppWidgetIds(new ComponentName(c, t)).length;
        return n;
    }

    /** Taille réelle donnée par l'écran d'accueil à chaque widget (diagnostic de largeur, comme SJ Widgets) */
    static String sizes(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < TYPES.length; k++) for (int id : m.getAppWidgetIds(new ComponentName(c, TYPES[k]))) {
            android.os.Bundle o = m.getAppWidgetOptions(id);
            b.append("\n- ").append(NAMES[k]).append(" : ").append(o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)).append('-').append(o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH))
                    .append(" x ").append(o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)).append('-').append(o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)).append(" dp");
        }
        return b.toString();
    }

    static volatile boolean busy = false;

    static void refreshing(Context c, boolean on) {
        busy = on;
        updateAll(c);
    }

    static void render(Context c, AppWidgetManager m, int id, Class<?> type) {
        RemoteViews v;
        try {
            Snap s = Store.snap(c);
            JSONObject st = s == null ? null : s.pick(Store.stay(c, id), LocalDate.now());
            if (Store.topic(c) == null) v = empty(c, "Touchez ici pour connecter ACM Widgets à Suivi ACM.");
            else if (Store.off(c)) v = empty(c, "Widgets déconnectés dans Suivi ACM. Touchez ici pour les reconnecter.");
            else if (s == null) v = empty(c, "Chargement… Touchez ici si rien ne s'affiche.");
            else if (st == null) v = empty(c, "Aucun séjour en cours ou à venir dans Suivi ACM.");
            else {
                LocalDate today = LocalDate.now();
                String sid = st.optString("id");
                int layout = type == DashWidget.class ? R.layout.widget_dash : type == AppelWidget.class ? R.layout.widget_appel : type == BudgetWidget.class ? R.layout.widget_budget : R.layout.widget_planning;
                v = new RemoteViews(c.getPackageName(), layout);
                String ago = Snap.ago(s.at(), System.currentTimeMillis());
                if (type == DashWidget.class) {
                    v.setTextViewText(R.id.title, st.optString("nom"));
                    v.setTextViewText(R.id.sub, subtitle(st, today));
                    v.setTextViewText(R.id.upd, ago);
                    appel(v, st, today);
                    budget(v, st);
                    plan(v, st, today, 4);
                    List<String> al = Snap.alerts(st, today);
                    for (int i = 0; i < A.length; i++) {
                        if (i < al.size()) { v.setViewVisibility(A[i], View.VISIBLE); v.setTextViewText(A[i], "• " + al.get(i)); v.setTextColor(A[i], warn(al.get(i)) ? 0xFFFFC9A8 : WHITE); }
                        else v.setViewVisibility(A[i], View.GONE);
                    }
                    v.setOnClickPendingIntent(R.id.appel_btn, open(c, "#rapide/appel@" + sid, id * 10 + 1));
                    v.setOnClickPendingIntent(R.id.bud_btn, open(c, "#rapide/depense@" + sid, id * 10 + 2));
                    v.setOnClickPendingIntent(R.id.t_appel, open(c, "#rapide/appel@" + sid, id * 10 + 1));
                    v.setOnClickPendingIntent(R.id.t_budget, open(c, "#rapide/depense@" + sid, id * 10 + 2));
                    v.setOnClickPendingIntent(R.id.t_plan, open(c, "#rapide/planning@" + sid, id * 10 + 3));
                    v.setOnClickPendingIntent(R.id.t_alert, open(c, "#rapide@" + sid, id * 10 + 4));
                    v.setOnClickPendingIntent(android.R.id.background, open(c, "#rapide@" + sid, id * 10 + 4));
                } else if (type == AppelWidget.class) {
                    v.setTextViewText(R.id.title, st.optString("nom"));
                    v.setTextViewText(R.id.upd, ago);
                    appel(v, st, today);
                    v.setOnClickPendingIntent(R.id.appel_btn, open(c, "#rapide/appel@" + sid, id * 10 + 1));
                    v.setOnClickPendingIntent(android.R.id.background, open(c, "#rapide/appel@" + sid, id * 10 + 1));
                } else if (type == BudgetWidget.class) {
                    v.setTextViewText(R.id.title, st.optString("nom"));
                    v.setTextViewText(R.id.upd, ago);
                    budget(v, st);
                    v.setOnClickPendingIntent(R.id.bud_btn, open(c, "#rapide/depense@" + sid, id * 10 + 2));
                    v.setOnClickPendingIntent(android.R.id.background, open(c, "#rapide@" + sid, id * 10 + 4));
                } else {
                    v.setTextViewText(R.id.title, ("Planning : " + st.optString("nom")).toUpperCase(Locale.FRANCE));
                    v.setTextViewText(R.id.upd, ago);
                    plan(v, st, today, 3);
                    v.setOnClickPendingIntent(android.R.id.background, open(c, "#rapide/planning@" + sid, id * 10 + 3));
                }
                Intent r = new Intent(c, RefreshActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                v.setOnClickPendingIntent(R.id.refresh, PendingIntent.getActivity(c, 9000 + id, r, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                v.setInt(R.id.refresh, "setImageAlpha", busy ? 60 : 255);
                edge(c, v, id, type == DashWidget.class ? 12 : 10);
            }
        } catch (Exception e) {
            v = empty(c, "Erreur d'affichage. Touchez pour actualiser.");
        }
        m.updateAppWidget(id, responsive(v));
    }

    static boolean warn(String s) {
        return !(s.startsWith("Dossiers des jeunes complets") || s.startsWith("Aucun traitement") || s.endsWith("pièces à jour") || s.endsWith(": OK") || s.endsWith("non calculé"));
    }

    static String subtitle(JSONObject st, LocalDate today) {
        String deb = st.optString("deb"), fin = st.optString("fin"), stat = Snap.status(st, today), dates = "";
        try {
            LocalDate a = LocalDate.parse(deb), b = LocalDate.parse(fin);
            dates = a.format(DM) + " - " + b.format(DM);
            if ("En cours".equals(stat)) return dates + " : jour " + (ChronoUnit.DAYS.between(a, today) + 1) + " sur " + (ChronoUnit.DAYS.between(a, b) + 1);
            if ("À venir".equals(stat)) { long n = ChronoUnit.DAYS.between(today, a); return dates + " : dans " + n + (n > 1 ? " jours" : " jour"); }
        } catch (Exception e) { /* dates absentes */ }
        return dates.isEmpty() ? stat : dates + " : " + stat.toLowerCase(Locale.FRANCE);
    }

    static void appel(RemoteViews v, JSONObject st, LocalDate today) {
        JSONObject a = st.optJSONObject("appel");
        int n = a == null ? 0 : a.optInt("n", 0);
        JSONObject last = a == null ? null : a.optJSONObject("last");
        if (last != null && today.toString().equals(st.optString("d"))) {
            v.setTextViewText(R.id.appel_big, last.optInt("p") + " / " + last.optInt("n"));
            v.setTextViewText(R.id.appel_sub, "présents" + (last.optString("h").isEmpty() ? "" : " à " + last.optString("h")));
        } else {
            v.setTextViewText(R.id.appel_big, String.valueOf(n));
            v.setTextViewText(R.id.appel_sub, (n > 1 ? "jeunes attendus" : "jeune attendu") + " : pas encore d'appel aujourd'hui");
        }
    }

    static void budget(RemoteViews v, JSONObject st) {
        JSONObject b = st.optJSONObject("bud");
        double rest = b == null ? 0 : b.optDouble("rest", 0);
        boolean noPct = b == null || b.isNull("pct");
        double pct = noPct ? 0 : b.optDouble("pct", 0);
        v.setTextViewText(R.id.bud_big, Snap.euros(rest));
        v.setTextColor(R.id.bud_big, rest < 0 ? RED : WHITE);
        v.setTextViewText(R.id.bud_sub, noPct ? "restant : budget prévu non renseigné" : "restant : " + Math.round(pct * 100) + " % dépensé");
        v.setViewVisibility(R.id.bud_bar, noPct ? View.GONE : View.VISIBLE);
        v.setProgressBar(R.id.bud_bar, 100, (int) Math.min(100, Math.max(0, Math.round(pct * 100))), false);
    }

    static void plan(RemoteViews v, JSONObject st, LocalDate today, int max) {
        LocalDate dem = today.plusDays(1);
        fill(v, P, Snap.plan(st, today), Snap.hasDay(st, today), max);
        fill(v, D, Snap.plan(st, dem), Snap.hasDay(st, dem), max);
    }

    static void fill(RemoteViews v, int[] ids, List<String> items, boolean known, int max) {
        for (int i = 0; i < Math.min(ids.length, max); i++) {
            String t = null;
            if (i == 0 && !known) t = "Résumé trop ancien : ouvrez Suivi ACM";
            else if (i == 0 && items.isEmpty()) t = "Rien de prévu";
            else if (known && i < items.size()) t = (i == max - 1 && items.size() > max) ? "+ " + (items.size() - max + 1) + " autres" : items.get(i);
            v.setViewVisibility(ids[i], t == null ? View.GONE : View.VISIBLE);
            if (t != null) v.setTextViewText(ids[i], t);
        }
    }

    /** Largeur : standard, large ou bord à bord (marge intérieure réduite, et sur Android 12+ marge négative pour gagner la marge de l'écran d'accueil). */
    static void edge(Context c, RemoteViews v, int id, int pad) {
        int e = Store.edge(c, id), p = UI.dp(c, e == 0 ? pad : e == 1 ? pad / 2f : 3);
        v.setViewPadding(android.R.id.background, p, p, p, p);
        v.setInt(android.R.id.background, "setBackgroundResource", e == 0 ? R.drawable.widget_bg : e == 1 ? R.drawable.widget_bg_large : R.drawable.widget_bg_edge);
        if (e > 0 && Build.VERSION.SDK_INT >= 31) {
            try {
                float mg = e == 1 ? -4f : -8f;
                v.setViewLayoutMargin(android.R.id.background, RemoteViews.MARGIN_LEFT, mg, TypedValue.COMPLEX_UNIT_DIP);
                v.setViewLayoutMargin(android.R.id.background, RemoteViews.MARGIN_RIGHT, mg, TypedValue.COMPLEX_UNIT_DIP);
            } catch (Throwable t) { /* écran d'accueil qui refuse : marge standard */ }
        }
    }

    /** Android 12+ : une seule mise en page déclarée pour que l'écran d'accueil l'affiche à sa taille réelle (correctif de SJ Widgets). */
    static RemoteViews responsive(RemoteViews v) {
        if (Build.VERSION.SDK_INT < 31) return v;
        try {
            java.util.Map<android.util.SizeF, RemoteViews> map = new java.util.HashMap<>();
            map.put(new android.util.SizeF(100f, 100f), v);
            return new RemoteViews(map);
        } catch (Exception e) {
            return v;
        }
    }

    static RemoteViews empty(Context c, String msg) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_empty);
        v.setTextViewText(R.id.msg, msg);
        Intent i = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        v.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return v;
    }

    /** Ouvre Suivi ACM (l'application installée capte l'adresse) au bon endroit : #rapide, #rapide/appel, #rapide/depense, #rapide/planning, @séjour */
    static PendingIntent open(Context c, String hash, int req) {
        String base = Store.app(c);
        int h = base.indexOf('#');
        if (h >= 0) base = base.substring(0, h);
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(base + hash)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
