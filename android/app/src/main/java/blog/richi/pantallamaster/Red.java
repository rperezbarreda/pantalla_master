package blog.richi.pantallamaster;

import android.net.Uri;
import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * LO ÚNICO QUE SALE A INTERNET, y sólo cuando se pide: importar un
 * personaje por su enlace (la web no puede preguntar a otras webs; la app
 * sí). Es lo mismo que hace el lanzador de escritorio (lanzador/almacen.go:
 * /__ddb, /__nivel20 y /__imagen), con las mismas comprobaciones.
 */
final class Red {
    private static final Pattern ID_DDB = Pattern.compile("^\\d{4,12}$");
    private static final Pattern RUTA_NIVEL20 = Pattern.compile(
            "^/games/[a-z0-9-]{1,40}/characters/[a-z0-9-]{1,120}(/spell_list\\?[A-Za-z0-9_=&%.-]{0,200})?$");
    private static final Pattern HOSTS_IMAGEN = Pattern.compile(
            "^([a-z0-9-]+\\.)*(dndbeyond\\.com|cursecdn\\.com|nivel20\\.com)$");
    private static final String AGENTE = "Mozilla/5.0 (Pantalla de master)";
    private static final int ESPERA = 20000;

    private Red() {
    }

    /** Un personaje de D&D Beyond (su ficha pública, en JSON). */
    static WebResourceResponse ddb(Uri u) {
        String id = u.getQueryParameter("id");
        if (id == null || !ID_DDB.matcher(id).matches()) {
            return error(400, "Bad Request");
        }
        return pedir("https://character-service.dndbeyond.com/character/v5/character/" + id,
                "application/json", "application/json", 20 << 20);
    }

    /** Un personaje de Nivel20 (su página pública, en HTML). */
    static WebResourceResponse nivel20(Uri u) {
        String ruta = u.getQueryParameter("ruta");
        if (ruta == null || !RUTA_NIVEL20.matcher(ruta).matches()) {
            return error(400, "Bad Request");
        }
        return pedir("https://nivel20.com" + ruta, "text/html", "text/plain", 20 << 20);
    }

    private static WebResourceResponse pedir(String direccion, String acepta, String tipo, int limite) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(direccion).openConnection();
            c.setConnectTimeout(ESPERA);
            c.setReadTimeout(ESPERA);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", AGENTE);
            c.setRequestProperty("Accept", acepta);
            int estado = c.getResponseCode();
            if (estado >= 300 && estado < 400) {
                return error(502, "Bad Gateway");
            }
            InputStream in = estado >= 400 ? c.getErrorStream() : c.getInputStream();
            byte[] b = in == null ? new byte[0] : leerHasta(in, limite);
            return new WebResourceResponse(tipo, "utf-8", estado, frase(estado), sinGuardar(),
                    new ByteArrayInputStream(b));
        } catch (IOException e) {
            return error(502, "Bad Gateway");
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    /** La foto de un personaje que se importa: sólo imágenes y sólo de esas webs. */
    static WebResourceResponse imagen(Uri u) {
        String direccion = u.getQueryParameter("url");
        if (direccion == null) {
            return error(400, "Bad Request");
        }
        HttpURLConnection c = null;
        try {
            for (int saltos = 0; saltos <= 3; saltos++) {
                URL url = new URL(direccion);
                if (!"https".equals(url.getProtocol()) || url.getPort() != -1
                        || !HOSTS_IMAGEN.matcher(url.getHost().toLowerCase(Locale.ROOT)).matches()) {
                    return error(saltos == 0 ? 400 : 502, saltos == 0 ? "Bad Request" : "Bad Gateway");
                }
                c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(ESPERA);
                c.setReadTimeout(ESPERA);
                c.setInstanceFollowRedirects(false);
                c.setRequestProperty("User-Agent", AGENTE);
                int estado = c.getResponseCode();
                if (estado >= 300 && estado < 400) {
                    String otra = c.getHeaderField("Location");
                    c.disconnect();
                    c = null;
                    if (otra == null) {
                        return error(502, "Bad Gateway");
                    }
                    direccion = new URL(url, otra).toString();
                    continue;
                }
                String tipo = c.getContentType() == null ? "" : c.getContentType().toLowerCase(Locale.ROOT);
                if (estado != 200 || !tipo.startsWith("image/") || tipo.contains("svg")) {
                    return error(502, "Bad Gateway");
                }
                byte[] b = leerHasta(c.getInputStream(), 10 << 20);
                Map<String, String> cab = sinGuardar();
                cab.put("X-Content-Type-Options", "nosniff");
                int pc = tipo.indexOf(';');
                return new WebResourceResponse(pc < 0 ? tipo : tipo.substring(0, pc).trim(), null, 200, "OK", cab,
                        new ByteArrayInputStream(b));
            }
            return error(502, "Bad Gateway");
        } catch (IOException | IllegalArgumentException e) {
            return error(502, "Bad Gateway");
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    private static byte[] leerHasta(InputStream in, int limite) throws IOException {
        try (InputStream entrada = in) {
            java.io.ByteArrayOutputStream sale = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[16384];
            int n;
            int total = 0;
            while ((n = entrada.read(b)) > 0 && total < limite) {
                int cuanto = Math.min(n, limite - total);
                sale.write(b, 0, cuanto);
                total += cuanto;
            }
            return sale.toByteArray();
        }
    }

    private static Map<String, String> sinGuardar() {
        Map<String, String> c = new HashMap<>();
        c.put("Cache-Control", "no-store");
        return c;
    }

    private static String frase(int estado) {
        if (estado == 200) {
            return "OK";
        }
        return estado >= 500 ? "Server Error" : "Error";
    }

    private static WebResourceResponse error(int estado, String frase) {
        return new WebResourceResponse("text/plain", "utf-8", estado, frase, sinGuardar(),
                new ByteArrayInputStream(new byte[0]));
    }
}
