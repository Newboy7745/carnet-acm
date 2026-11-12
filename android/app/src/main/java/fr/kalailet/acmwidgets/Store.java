package fr.kalailet.acmwidgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Stockage local (sujet, clé, dernier résumé, réglages des widgets) et lecture du sujet ntfy. */
final class Store {
    static final String NTFY = "https://ntfy.sh";
    static final String APP = "https://newboy7745.github.io/carnet-acm/";

    private Store() { }

    static SharedPreferences p(Context c) { return c.getSharedPreferences("acm", Context.MODE_PRIVATE); }

    static String topic(Context c) { String t = p(c).getString("topic", null); return t == null || t.isEmpty() ? null : t; }
    static String key(Context c) { return p(c).getString("key", ""); }
    static String server(Context c) { return p(c).getString("server", NTFY); }
    static String app(Context c) { String a = p(c).getString("app", ""); return a.isEmpty() ? APP : a; }
    static boolean off(Context c) { return p(c).getBoolean("off", false); }

    /**
     * Code de connexion (lien acmwidgets://connect?t=…&k=…&u=…&s=…, ou le même lien collé) :
     * renvoie null si tout va bien, sinon un message.
     */
    static String connect(Context c, String text) {
        if (text == null) return "Code vide.";
        String s = text.trim();
        int i = s.indexOf("connect?");
        if (i < 0) return "Code invalide : copiez-le de nouveau dans Suivi ACM.";
        int h = s.indexOf('#', i);
        Uri u = Uri.parse("acmwidgets://" + (h > 0 ? s.substring(i, h) : s.substring(i)));
        String t = u.getQueryParameter("t"), k = u.getQueryParameter("k"), app = u.getQueryParameter("u"), srv = u.getQueryParameter("s");
        if (t == null || !t.matches("acmw-[a-z2-7]{26}") || k == null || !k.matches("[A-Za-z0-9_-]{43}")) return "Code invalide : copiez-le de nouveau dans Suivi ACM.";
        if (app == null || !app.matches("https?://.+")) app = APP;
        if (srv == null || !srv.matches("https://[A-Za-z0-9.-]+(:\\d+)?(/.*)?")) srv = NTFY;
        p(c).edit().putString("topic", t).putString("key", k).putString("app", app).putString("server", srv.replaceAll("/+$", ""))
                .remove("snap").putLong("snapAt", 0).putLong("checkAt", 0).putBoolean("off", false).apply();
        return null;
    }

    static Snap snap(Context c) {
        String s = p(c).getString("snap", null);
        if (s == null) return null;
        try { return new Snap(s); } catch (Exception e) { return null; }
    }

    static long checkAt(Context c) { return p(c).getLong("checkAt", 0); }
    static boolean stale(Context c) { return System.currentTimeMillis() - checkAt(c) > 10 * 60 * 1000L; }

    /** Lit les messages des 12 dernières heures du sujet et garde le plus récent qui se déchiffre ; null si tout va bien. */
    static synchronized String fetch(Context c) {
        String t = topic(c);
        if (t == null) return "Pas encore connecté : dans Suivi ACM, Données, Widgets Android, touchez « Connecter mes widgets ».";
        HttpURLConnection con = null;
        try {
            URL u = new URL(server(c) + "/" + t + "/json?poll=1&since=12h");
            con = (HttpURLConnection) u.openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(20000);
            con.setRequestProperty("User-Agent", "ACM-Widgets");
            int code = con.getResponseCode();
            String body = read(code >= 400 ? con.getErrorStream() : con.getInputStream());
            if (code >= 400) return "Erreur du serveur ntfy (" + code + ").";
            String found = null;
            boolean bad = false;
            String[] lines = body.split("\n");
            for (int i = lines.length - 1; i >= 0 && found == null; i--) {
                String l = lines[i].trim();
                if (l.isEmpty()) continue;
                JSONObject j;
                try { j = new JSONObject(l); } catch (Exception e) { continue; }
                if (!"message".equals(j.optString("event"))) continue;
                String m = j.optString("message", "");
                if (!m.startsWith("acmw1.")) continue;
                try { found = Crypto.open(m, key(c), t); } catch (Exception e) { bad = true; }
            }
            long now = System.currentTimeMillis();
            p(c).edit().putLong("checkAt", now).apply();
            if (found == null) {
                if (bad) return "Clé périmée : reconnectez les widgets depuis Suivi ACM.";
                return snap(c) == null ? "Aucune donnée pour l'instant : ouvrez Suivi ACM sur ce téléphone." : null;
            }
            Snap s = new Snap(found);
            if (s.off()) { p(c).edit().remove("snap").putBoolean("off", true).apply(); return "Widgets déconnectés dans Suivi ACM."; }
            SharedPreferences.Editor e = p(c).edit().putString("snap", found).putLong("snapAt", now).putBoolean("off", false);
            if (!s.app().isEmpty()) e.putString("app", s.app());
            e.apply();
            return null;
        } catch (Exception e) {
            return "Pas de connexion internet (" + e.getClass().getSimpleName() + ").";
        } finally {
            if (con != null) con.disconnect();
        }
    }

    static String read(InputStream in) throws IOException {
        if (in == null) return "";
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close();
        return b.toString("UTF-8");
    }

    /** Séjour affiché par un widget : "" = séjour en cours (automatique : en cours, sinon le prochain). */
    static String stay(Context c, int id) { return p(c).getString("stay_" + id, ""); }
    static void setStay(Context c, int id, String s) { p(c).edit().putString("stay_" + id, s).apply(); }

    /** Largeur : 0 = standard, 1 = large, 2 = bord à bord (compense la marge ajoutée par l'écran d'accueil). */
    static int edge(Context c, int id) { return p(c).getInt("edge_" + id, 0); }
    static void setEdge(Context c, int id, int v) { p(c).edit().putInt("edge_" + id, v).apply(); }

    static void remove(Context c, int id) { p(c).edit().remove("stay_" + id).remove("edge_" + id).apply(); }
}
