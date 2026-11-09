package blog.richi.pantallamaster;

import android.net.Uri;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * EL PUENTE entre la web (android.js, como window.ocAndroidNativo) y la
 * app. Sólo lo justo: el color de las barras, abrir la pantalla de los
 * jugadores o el reproductor, abrir un enlace fuera y pasar un archivo
 * (para guardarlo donde se elija o abrirlo con otra app). Los métodos
 * llegan desde un hilo aparte, de uno en uno.
 */
final class Puente {
    private final Base actividad;
    // En la pantalla externa (la tele) no se pintan las barras de la tablet.
    private final boolean deLaPantallaExterna;
    private final Map<String, Descarga> descargas = new HashMap<>();

    private static final class Descarga {
        final File archivo;
        final FileOutputStream sale;
        final String nombre;
        final String tipo;

        Descarga(File archivo, FileOutputStream sale, String nombre, String tipo) {
            this.archivo = archivo;
            this.sale = sale;
            this.nombre = nombre;
            this.tipo = tipo;
        }
    }

    Puente(Base actividad, boolean deLaPantallaExterna) {
        this.actividad = actividad;
        this.deLaPantallaExterna = deLaPantallaExterna;
    }

    @JavascriptInterface
    public String version() {
        try {
            return actividad.getPackageManager().getPackageInfo(actividad.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public void color(String hex) {
        if (deLaPantallaExterna) {
            return;
        }
        final int c;
        try {
            c = android.graphics.Color.parseColor(hex.trim());
        } catch (RuntimeException e) {
            return;
        }
        actividad.runOnUiThread(() -> actividad.pintarBarras(c));
    }

    @JavascriptInterface
    public void aviso(String texto) {
        actividad.runOnUiThread(() -> Toast.makeText(actividad, texto, Toast.LENGTH_LONG).show());
    }

    @JavascriptInterface
    public void abrirVentana(String pagina) {
        if (!"jugadores.html".equals(pagina) && !"reproductor.html".equals(pagina)) {
            return;
        }
        actividad.runOnUiThread(() -> actividad.abrirVentana(pagina));
    }

    @JavascriptInterface
    public void abrirFuera(String direccion) {
        actividad.runOnUiThread(() -> Web.abrirFuera(actividad, Uri.parse(direccion)));
    }

    // ---- UN ARCHIVO, por trozos (en base64) ----

    @JavascriptInterface
    public String descargaEmpezar(String nombre, String tipo) {
        File carpeta = new File(actividad.getCacheDir(), "descargas");
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            return "";
        }
        String id = UUID.randomUUID().toString();
        File f = new File(carpeta, id);
        try {
            FileOutputStream sale = new FileOutputStream(f);
            synchronized (descargas) {
                descargas.put(id, new Descarga(f, sale, nombre, tipo));
            }
            return id;
        } catch (IOException e) {
            return "";
        }
    }

    @JavascriptInterface
    public boolean descargaTrozo(String id, String base64) {
        Descarga d;
        synchronized (descargas) {
            d = descargas.get(id);
        }
        if (d == null) {
            return false;
        }
        try {
            d.sale.write(Base64.decode(base64, Base64.DEFAULT));
            return true;
        } catch (IOException | IllegalArgumentException e) {
            descargaCancelar(id);
            return false;
        }
    }

    /** modo: «guardar» (elegir dónde) o «ver» (abrirlo con otra app). */
    @JavascriptInterface
    public void descargaTerminar(String id, String modo) {
        Descarga d;
        synchronized (descargas) {
            d = descargas.remove(id);
        }
        if (d == null) {
            return;
        }
        try {
            d.sale.close();
        } catch (IOException e) {
            borrar(d.archivo);
            aviso("No se ha podido preparar el archivo.");
            return;
        }
        final boolean ver = "ver".equals(modo);
        actividad.runOnUiThread(() -> {
            if (ver) {
                actividad.verArchivo(d.archivo, d.nombre, d.tipo);
            } else {
                actividad.guardarArchivo(d.archivo, d.nombre, d.tipo);
            }
        });
    }

    @JavascriptInterface
    public void descargaCancelar(String id) {
        Descarga d;
        synchronized (descargas) {
            d = descargas.remove(id);
        }
        if (d != null) {
            try {
                d.sale.close();
            } catch (IOException e) {
                // Da igual: se borra.
            }
            borrar(d.archivo);
        }
    }

    static void borrar(File f) {
        if (f != null && f.exists() && !f.delete()) {
            f.deleteOnExit();
        }
    }
}
