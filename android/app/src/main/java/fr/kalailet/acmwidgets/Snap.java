package fr.kalailet.acmwidgets;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Résumé déchiffré (schéma v1, voir wgSnapshot dans Suivi ACM) :
 * {v, at, app, off?, stays:[{id, nom, deb, fin, st, d, appel:{n, last:{h,p,n}|null}, bud:{plan,spent,rest,pct|null},
 *  plan:{"AAAA-MM-JJ":[{h,t,g}]}, al:{inc, trait, eqM, taux:true|false|null, manque, dip}}]}
 */
public final class Snap {
    public final JSONObject o;
    public final List<JSONObject> stays = new ArrayList<>();

    public Snap(String json) throws Exception {
        o = new JSONObject(json);
        if (o.optInt("v", 0) != 1) throw new IllegalArgumentException("version");
        JSONArray a = o.optJSONArray("stays");
        if (a != null) for (int i = 0; i < a.length(); i++) { JSONObject s = a.optJSONObject(i); if (s != null && !s.optString("id").isEmpty()) stays.add(s); }
    }

    public boolean off() { return o.optInt("off", 0) == 1; }
    public long at() { return o.optLong("at", 0); }
    public String app() { String u = o.optString("app", ""); return u.startsWith("http") ? u : ""; }

    /** Séjour à afficher : celui choisi s'il existe, sinon celui en cours (selon la date du téléphone), sinon le prochain. */
    public JSONObject pick(String pref, LocalDate today) {
        if (pref != null && !pref.isEmpty()) for (JSONObject s : stays) if (pref.equals(s.optString("id"))) return s;
        String t = today.toString();
        for (JSONObject s : stays) if (s.optString("deb").compareTo(t) <= 0 && s.optString("fin").compareTo(t) >= 0) return s;
        JSONObject next = null;
        for (JSONObject s : stays) if (s.optString("deb").compareTo(t) > 0 && (next == null || s.optString("deb").compareTo(next.optString("deb")) < 0)) next = s;
        return next != null ? next : stays.isEmpty() ? null : stays.get(0);
    }

    public static String status(JSONObject s, LocalDate today) {
        String t = today.toString();
        if (s.optString("deb").compareTo(t) > 0) return "À venir";
        if (!s.optString("fin").isEmpty() && s.optString("fin").compareTo(t) < 0) return "Terminé";
        return "En cours";
    }

    /** Activités d'un jour : « Matin : Grand jeu (Les Comètes) » */
    public static List<String> plan(JSONObject s, LocalDate d) {
        List<String> out = new ArrayList<>();
        JSONObject p = s.optJSONObject("plan");
        JSONArray a = p == null ? null : p.optJSONArray(d.toString());
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            JSONObject x = a.optJSONObject(i); if (x == null) continue;
            String h = x.optString("h", ""), t = x.optString("t", "Activité"), g = x.optString("g", "");
            out.add((h.isEmpty() ? "" : h + " : ") + t + (g.isEmpty() ? "" : " (" + g + ")"));
        }
        return out;
    }

    /** Le planning est-il connu pour ce jour ? (résumé trop ancien sinon) */
    public static boolean hasDay(JSONObject s, LocalDate d) {
        JSONObject p = s.optJSONObject("plan");
        return p != null && p.has(d.toString());
    }

    /** Lignes d'alertes : nombres seulement, jamais de nom. */
    public static List<String> alerts(JSONObject s, LocalDate today) {
        List<String> out = new ArrayList<>();
        JSONObject al = s.optJSONObject("al");
        if (al == null) return out;
        int inc = al.optInt("inc", 0), trait = al.optInt("trait", 0), eqM = al.optInt("eqM", 0), manque = al.optInt("manque", 0), dip = al.optInt("dip", 0);
        out.add(inc == 0 ? "Dossiers des jeunes complets" : inc + (inc > 1 ? " dossiers incomplets" : " dossier incomplet"));
        if (today.toString().equals(s.optString("d"))) out.add(trait == 0 ? "Aucun traitement aujourd'hui" : trait + (trait > 1 ? " traitements aujourd'hui" : " traitement aujourd'hui"));
        out.add(eqM == 0 ? "Équipe : pièces à jour" : "Équipe : " + eqM + (eqM > 1 ? " personnes avec des pièces manquantes" : " personne avec des pièces manquantes"));
        Object tx = al.opt("taux");
        if (Boolean.TRUE.equals(tx) && dip == 0) out.add("Taux d'encadrement : OK");
        else if (Boolean.TRUE.equals(tx)) out.add("Encadrement : " + dip + (dip > 1 ? " diplômes manquants" : " diplôme manquant"));
        else if (Boolean.FALSE.equals(tx)) out.add("Taux d'encadrement : il manque " + manque + (manque > 1 ? " animateurs" : " animateur"));
        else out.add("Taux d'encadrement : non calculé");
        return out;
    }

    public static String euros(double v) {
        long r = Math.round(v);
        String s = String.valueOf(Math.abs(r));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) { if (i > 0 && (s.length() - i) % 3 == 0) b.append(' '); b.append(s.charAt(i)); }
        return (r < 0 ? "-" : "") + b + " €";
    }

    /** « il y a 5 min », « il y a 2 h », « à l'instant » */
    public static String ago(long at, long now) {
        if (at <= 0) return "";
        long m = Math.max(0, (now - at) / 60000);
        if (m < 1) return "à l'instant";
        if (m < 60) return "il y a " + m + " min";
        long h = m / 60;
        if (h < 48) return "il y a " + h + " h";
        return "il y a " + (h / 24) + " j";
    }
}
