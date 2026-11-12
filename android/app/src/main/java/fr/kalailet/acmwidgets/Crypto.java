package fr.kalailet.acmwidgets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Déchiffrement du résumé publié par Suivi ACM (19q-widgets.js, wgSeal) :
 * « acmw1. » + base64( iv 12 octets | AES-GCM 256 ( gzip(JSON) ) ), données associées = le sujet ntfy.
 * Java pur (aucune dépendance Android) : testé aussi sur un ordinateur.
 */
public final class Crypto {
    private Crypto() { }

    public static String open(String msg, String key64url, String topic) throws Exception {
        if (msg == null || !msg.startsWith("acmw1.")) throw new IllegalArgumentException("format");
        byte[] raw = Base64.getDecoder().decode(msg.substring(6).trim());
        byte[] key = Base64.getUrlDecoder().decode(key64url.trim());
        if (key.length != 32 || raw.length < 12 + 16) throw new IllegalArgumentException("taille");
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, raw, 0, 12));
        c.updateAAD(topic.getBytes(StandardCharsets.UTF_8));
        byte[] gz = c.doFinal(raw, 12, raw.length - 12);
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }
}
