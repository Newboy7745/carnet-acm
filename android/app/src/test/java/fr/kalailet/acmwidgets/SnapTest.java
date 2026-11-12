package fr.kalailet.acmwidgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Compatibilité avec Suivi ACM : le fichier snapshot_fixture.json est produit et chiffré par le code de la PWA
 * (tests/widgets_fixture.js : wgSnapshot + wgSeal, séjour fictif du mode test). Ce test le déchiffre avec la même clé
 * et vérifie que le schéma lu par les widgets correspond.
 */
public class SnapTest {
    private static JSONObject fixture() throws Exception {
        try (InputStream in = SnapTest.class.getResourceAsStream("/snapshot_fixture.json")) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            return new JSONObject(b.toString("UTF-8"));
        }
    }

    @Test
    public void decryptsWhatThePwaEncrypted() throws Exception {
        JSONObject f = fixture();
        String json = Crypto.open(f.getString("message"), f.getString("key"), f.getString("topic"));
        assertTrue("même contenu que le résumé de la PWA", new JSONObject(json).similar(f.getJSONObject("snapshot")));
    }

    @Test
    public void wrongKeyOrTopicIsRefused() throws Exception {
        JSONObject f = fixture();
        try { Crypto.open(f.getString("message"), "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", f.getString("topic")); fail("clé fausse acceptée"); } catch (Exception e) { /* attendu */ }
        try { Crypto.open(f.getString("message"), f.getString("key"), "acmw-autresujetautresujetautresu"); fail("sujet faux accepté"); } catch (Exception e) { /* attendu */ }
    }

    @Test
    public void schemaMatchesWhatTheWidgetsRead() throws Exception {
        JSONObject f = fixture();
        Snap s = new Snap(Crypto.open(f.getString("message"), f.getString("key"), f.getString("topic")));
        LocalDate today = LocalDate.parse(f.getString("today"));
        assertEquals(1, s.stays.size());
        assertTrue(s.at() > 0);
        assertEquals("https://newboy7745.github.io/carnet-acm/", s.app());
        JSONObject st = s.pick("", today);
        assertNotNull(st);
        assertEquals("Colo fictive (TEST)", st.optString("nom"));
        assertEquals("En cours", Snap.status(st, today));
        assertEquals("À venir", Snap.status(st, LocalDate.parse(st.optString("deb")).minusDays(1)));
        assertEquals(st, s.pick("colo-fictive-test", today));
        JSONObject a = st.getJSONObject("appel");
        assertEquals(6, a.getInt("n"));
        assertEquals(5, a.getJSONObject("last").getInt("p"));
        assertEquals("14:05", a.getJSONObject("last").getString("h"));
        JSONObject b = st.getJSONObject("bud");
        assertTrue(b.has("plan") && b.has("spent") && b.has("rest") && b.has("pct"));
        assertEquals("-133 €", Snap.euros(b.getDouble("rest")));
        List<String> p = Snap.plan(st, today);
        assertEquals(2, p.size());
        assertEquals("Matin : Grand jeu de piste dans le village", p.get(0));
        assertEquals(1, Snap.plan(st, today.plusDays(1)).size());
        assertTrue(Snap.hasDay(st, today.plusDays(2)));
        assertTrue(!Snap.hasDay(st, today.plusDays(3)));
        List<String> al = Snap.alerts(st, today);
        assertEquals("6 dossiers incomplets", al.get(0));
        assertEquals("Aucun traitement aujourd'hui", al.get(1));
        assertEquals("Équipe : 3 personnes avec des pièces manquantes", al.get(2));
        assertEquals("Taux d'encadrement : OK", al.get(3));
        assertEquals("il y a 5 min", Snap.ago(1000L, 1000L + 5 * 60000L));
    }
}
