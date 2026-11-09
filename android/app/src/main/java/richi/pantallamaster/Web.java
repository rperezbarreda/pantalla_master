package richi.pantallamaster;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * LA WEB DENTRO DE LA APP. Los archivos de la pantalla de máster van en el
 * APK (assets/web/, copiados de la raíz del repo al compilar) y se sirven
 * desde aquí con una dirección https de mentira
 * (https://appassets.androidplatform.net/, la que reserva Android para
 * esto): así la app va sin conexión desde el primer momento y sin depender
 * de ninguna web. Lo único que sale a internet es lo que hacía el lanzador
 * de escritorio cuando se le pide (importar un personaje por enlace), en
 * {@link Red}.
 */
final class Web {
    static final String HOST = "appassets.androidplatform.net";
    static final String RAIZ = "https://" + HOST + "/";

    private Web() {
    }

    /** Un WebView ya preparado, con el puente a lo nativo. */
    static WebView crear(Context ctx, Puente puente, Runnable siSeCae) {
        WebView w = new WebView(ctx);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        // Como en Chrome: la página manda con su <meta name="viewport">
        // (ancho del dispositivo, sin el zoom del doble toque ni la espera
        // de cada toque que trae), y se puede ampliar con dos dedos (sin
        // los botones de zoom) donde la página no usa ese gesto (en el
        // tablero, los dos dedos son su zoom).
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(false);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " PantallaMaster-Android");
        textoDelSistema(w);
        w.setBackgroundColor(Color.TRANSPARENT);
        w.addJavascriptInterface(puente, "ocAndroidNativo");
        w.setWebViewClient(new Cliente(ctx.getApplicationContext(), siSeCae));
        return w;
    }

    /** El tamaño de letra del sistema (Ajustes → Pantalla) también vale aquí. */
    static void textoDelSistema(WebView w) {
        float escala = w.getContext().getResources().getConfiguration().fontScale;
        w.getSettings().setTextZoom(Math.round(escala * 100));
    }

    /** Abre fuera de la app (en el navegador, el correo…) lo que no es suyo. */
    static void abrirFuera(Context ctx, Uri u) {
        String esquema = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
        if (!esquema.equals("https") && !esquema.equals("http") && !esquema.equals("mailto")) {
            return;
        }
        Intent i = new Intent(Intent.ACTION_VIEW, u);
        i.addCategory(Intent.CATEGORY_BROWSABLE);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, "No hay ninguna app para abrir ese enlace.", Toast.LENGTH_LONG).show();
        }
    }

    static final class Cliente extends WebViewClient {
        private final Context ctx;
        private final Runnable siSeCae;

        Cliente(Context ctx, Runnable siSeCae) {
            this.ctx = ctx;
            this.siSeCae = siSeCae;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
            Uri u = r.getUrl();
            if (HOST.equals(u.getHost()) && "https".equals(u.getScheme())) {
                return false;
            }
            // Lo de fuera, a su app; lo demás (blob:, data:, intent:…), nada:
            // la pantalla de máster no se va nunca de la app.
            abrirFuera(v.getContext(), u);
            return true;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
            Uri u = r.getUrl();
            if (!HOST.equals(u.getHost())) {
                return null;
            }
            String ruta = u.getPath();
            if (ruta == null || ruta.isEmpty() || ruta.equals("/")) {
                ruta = "/index.html";
            }
            switch (ruta) {
                case "/__android.js":
                    return archivo(ctx.getAssets(), "android.js", null);
                case "/__ddb":
                    return Red.ddb(u);
                case "/__nivel20":
                    return Red.nivel20(u);
                case "/__imagen":
                    return Red.imagen(u);
                case "/__actualizacion":
                    return Red.actualizacion(ctx);
                case "/__archivos":
                    return Carpeta.de(ctx).hay() ? texto("application/json", Carpeta.de(ctx).lista()) : noEsta();
                case "/sw.js":
                    // Sin service worker: los archivos ya están en la app.
                    return noEsta();
                default:
                    if (ruta.startsWith("/__recibido/")) {
                        java.io.File f = Recibidos.archivo(ctx, ruta.substring("/__recibido/".length()));
                        try {
                            return f == null ? noEsta() : new WebResourceResponse("application/octet-stream", null, 200, "OK",
                                    cabecerasBase(), new java.io.FileInputStream(f));
                        } catch (IOException e) {
                            return noEsta();
                        }
                    }
                    if (ruta.startsWith("/__archivos/")) {
                        return deLaCarpeta(ctx, ruta.substring("/__archivos/".length()), r.getRequestHeaders());
                    }
                    if (ruta.contains("..") || ruta.contains("\\")) {
                        return noEsta();
                    }
                    return archivo(ctx.getAssets(), "web" + ruta, r.getRequestHeaders());
            }
        }

        @Override
        public boolean onRenderProcessGone(WebView v, android.webkit.RenderProcessGoneDetail d) {
            // Si el motor de la web se cae, se rehace la ventana en vez de
            // cerrarse la app entera (lo guardado está a salvo).
            if (siSeCae != null) {
                siSeCae.run();
            }
            return true;
        }
    }

    // ---- LOS ARCHIVOS DE LA APP ----

    private static final Map<String, String> TIPOS = new HashMap<>();

    static {
        TIPOS.put("html", "text/html");
        TIPOS.put("htm", "text/html");
        TIPOS.put("js", "text/javascript");
        TIPOS.put("css", "text/css");
        TIPOS.put("json", "application/json");
        TIPOS.put("webmanifest", "application/manifest+json");
        TIPOS.put("txt", "text/plain");
        TIPOS.put("svg", "image/svg+xml");
        TIPOS.put("png", "image/png");
        TIPOS.put("jpg", "image/jpeg");
        TIPOS.put("jpeg", "image/jpeg");
        TIPOS.put("webp", "image/webp");
        TIPOS.put("gif", "image/gif");
        TIPOS.put("ico", "image/x-icon");
        TIPOS.put("woff2", "font/woff2");
        TIPOS.put("woff", "font/woff");
        TIPOS.put("ttf", "font/ttf");
        TIPOS.put("otf", "font/otf");
        TIPOS.put("ogg", "audio/ogg");
        TIPOS.put("mp3", "audio/mpeg");
        TIPOS.put("wav", "audio/wav");
        TIPOS.put("m4a", "audio/mp4");
        TIPOS.put("pdf", "application/pdf");
    }

    static String tipoDe(String nombre) {
        int punto = nombre.lastIndexOf('.');
        String ext = punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        String t = TIPOS.get(ext);
        return t == null ? "application/octet-stream" : t;
    }

    private static boolean esTexto(String tipo) {
        return tipo.startsWith("text/") || tipo.endsWith("json") || tipo.equals("image/svg+xml");
    }

    private static Map<String, String> cabecerasBase() {
        Map<String, String> c = new HashMap<>();
        // Lo de dentro cambia con cada versión de la app: que no se guarde.
        c.put("Cache-Control", "no-store");
        c.put("X-Content-Type-Options", "nosniff");
        return c;
    }

    static WebResourceResponse texto(String tipo, String t) {
        return new WebResourceResponse(tipo, "utf-8", 200, "OK", cabecerasBase(),
                new ByteArrayInputStream(t.getBytes(StandardCharsets.UTF_8)));
    }

    /** Un archivo subido, de la carpeta (como GET /__archivos/<id> en el programa). */
    private static WebResourceResponse deLaCarpeta(Context ctx, String id, Map<String, String> pedidas) {
        Carpeta c = Carpeta.de(ctx);
        InputStream in = c.abrirArchivo(id);
        if (in == null) {
            return noEsta();
        }
        String tipo = c.tipoDe(id);
        if (tipo.isEmpty()) {
            tipo = "application/octet-stream";
        }
        String rango = pedidas == null ? null : cabecera(pedidas, "Range");
        try {
            if (rango != null && (tipo.startsWith("audio/") || tipo.startsWith("video/"))) {
                return trozo(leer(in), rango, tipo, cabecerasBase());
            }
        } catch (IOException e) {
            return noEsta();
        }
        return new WebResourceResponse(tipo, null, 200, "OK", cabecerasBase(), in);
    }

    static WebResourceResponse noEsta() {
        return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", cabecerasBase(),
                new ByteArrayInputStream(new byte[0]));
    }

    private static WebResourceResponse archivo(AssetManager am, String nombre, Map<String, String> pedidas) {
        String tipo = tipoDe(nombre);
        String cod = esTexto(tipo) ? "utf-8" : null;
        InputStream in;
        try {
            in = am.open(nombre);
        } catch (IOException e) {
            return noEsta();
        }
        Map<String, String> cab = cabecerasBase();
        try {
            if (tipo.equals("text/html")) {
                return new WebResourceResponse(tipo, cod, 200, "OK", cab,
                        new ByteArrayInputStream(conPuente(leer(in)).getBytes(StandardCharsets.UTF_8)));
            }
            String rango = pedidas == null ? null : cabecera(pedidas, "Range");
            if (rango != null && (tipo.startsWith("audio/") || tipo.startsWith("video/"))) {
                return trozo(leer(in), rango, tipo, cab);
            }
            if (tipo.startsWith("audio/") || tipo.startsWith("video/")) {
                cab.put("Accept-Ranges", "bytes");
            }
            return new WebResourceResponse(tipo, cod, 200, "OK", cab, in);
        } catch (IOException e) {
            return noEsta();
        }
    }

    /**
     * Las páginas llevan, antes que nada, __android.js: lo que en el
     * navegador hace el propio navegador (abrir ventanas, guardar archivos…)
     * y aquí hace la app.
     */
    private static String conPuente(byte[] html) {
        String t = new String(html, StandardCharsets.UTF_8);
        String puente = "<script src=\"/__android.js\"></script>\n    ";
        int i = t.indexOf("<script");
        if (i < 0) {
            i = t.indexOf("</head>");
        }
        return i < 0 ? puente + t : t.substring(0, i) + puente + t.substring(i);
    }

    /** Para mover la música adelante y atrás: sólo el trozo que se pide. */
    private static WebResourceResponse trozo(byte[] todo, String rango, String tipo, Map<String, String> cab) {
        long total = todo.length;
        long desde = 0;
        long hasta = total - 1;
        String r = rango.trim();
        if (r.startsWith("bytes=") && !r.contains(",")) {
            String[] p = r.substring(6).split("-", -1);
            try {
                if (p[0].isEmpty()) {
                    desde = Math.max(0, total - Long.parseLong(p[1]));
                } else {
                    desde = Long.parseLong(p[0]);
                    if (p.length > 1 && !p[1].isEmpty()) {
                        hasta = Math.min(hasta, Long.parseLong(p[1]));
                    }
                }
            } catch (NumberFormatException e) {
                desde = 0;
                hasta = total - 1;
            }
        }
        if (desde > hasta || desde >= total) {
            cab.put("Content-Range", "bytes */" + total);
            return new WebResourceResponse(tipo, null, 416, "Range Not Satisfiable", cab,
                    new ByteArrayInputStream(new byte[0]));
        }
        int largo = (int) (hasta - desde + 1);
        cab.put("Accept-Ranges", "bytes");
        cab.put("Content-Range", "bytes " + desde + "-" + hasta + "/" + total);
        cab.put("Content-Length", String.valueOf(largo));
        return new WebResourceResponse(tipo, null, 206, "Partial Content", cab,
                new ByteArrayInputStream(todo, (int) desde, largo));
    }

    static String cabecera(Map<String, String> c, String nombre) {
        for (Map.Entry<String, String> e : c.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(nombre)) {
                return e.getValue();
            }
        }
        return null;
    }

    static byte[] leer(InputStream in) throws IOException {
        try (InputStream entrada = in) {
            ByteArrayOutputStream sale = new ByteArrayOutputStream();
            byte[] b = new byte[16384];
            int n;
            while ((n = entrada.read(b)) > 0) {
                sale.write(b, 0, n);
            }
            return sale.toByteArray();
        }
    }
}
