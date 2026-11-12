package richi.pantallamaster;

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
 * jugadores o el reproductor, abrir un enlace fuera, pasar un archivo
 * (para guardarlo donde se elija o abrirlo con otra app), lo guardado
 * cuando va en una carpeta ({@link Carpeta}), si suena música
 * ({@link Servicio}), la pantalla encendida y la versión. Los métodos
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

    // ---- LO GUARDADO, EN LA CARPETA (si se eligió una) ----

    private Carpeta carpeta() {
        return Carpeta.de(actividad);
    }

    @JavascriptInterface
    public boolean carpetaHay() {
        return carpeta().hay();
    }

    @JavascriptInterface
    public boolean carpetaPerdida() {
        return carpeta().perdida();
    }

    @JavascriptInterface
    public String carpetaNombre() {
        return carpeta().nombre();
    }

    @JavascriptInterface
    public void carpetaElegir() {
        if (!deLaPantallaExterna) {
            actividad.runOnUiThread(actividad::elegirCarpeta);
        }
    }

    @JavascriptInterface
    public void carpetaQuitar() {
        if (!deLaPantallaExterna) {
            actividad.runOnUiThread(actividad::quitarCarpeta);
        }
    }

    /** El valor en JSON («null» si no está): así se distingue nada de vacío. */
    @JavascriptInterface
    public String datoLeer(String k) {
        String v = carpeta().leerDato(k);
        return v == null ? "null" : org.json.JSONObject.quote(v);
    }

    @JavascriptInterface
    public void datoPoner(String k, String v) {
        carpeta().ponerDato(k, v);
    }

    @JavascriptInterface
    public void datoQuitar(String k) {
        carpeta().quitarDato(k);
    }

    @JavascriptInterface
    public void datosVaciar() {
        carpeta().vaciarDatos();
    }

    @JavascriptInterface
    public String datosClaves() {
        return carpeta().claves();
    }

    @JavascriptInterface
    public boolean datosVacios() {
        return carpeta().vacia();
    }

    @JavascriptInterface
    public boolean datosGuardarYa() {
        return carpeta().guardarYa();
    }

    /** Un archivo subido (imagen, PDF, música) a la carpeta: llega por trozos como una descarga. */
    @JavascriptInterface
    public boolean archivoGuardar(String idDescarga, String id) {
        Descarga d;
        synchronized (descargas) {
            d = descargas.remove(idDescarga);
        }
        if (d == null) {
            return false;
        }
        try {
            d.sale.close();
            return carpeta().guardarArchivo(id, d.tipo, d.archivo);
        } catch (IOException e) {
            return false;
        } finally {
            borrar(d.archivo);
        }
    }

    @JavascriptInterface
    public void archivoBorrar(String id) {
        carpeta().borrarArchivo(id);
    }

    /** «ordenado/» (una copia con nombres, para mirarla): escribir, copiar un archivo subido y borrar. */
    @JavascriptInterface
    public void ordenadoEscribir(String ruta, String texto) {
        carpeta().escribirOrdenado(ruta, texto);
    }

    @JavascriptInterface
    public void ordenadoCopiar(String ruta, String id) {
        carpeta().copiarOrdenado(ruta, id);
    }

    @JavascriptInterface
    public void ordenadoBorrar(String ruta) {
        carpeta().borrarOrdenado(ruta);
    }

    @JavascriptInterface
    public boolean copiaFalta() {
        return carpeta().copiaFalta();
    }

    @JavascriptInterface
    public boolean copiaGuardar(String texto) {
        return carpeta().guardarCopia(texto);
    }

    /** Lo que ha llegado con «Abrir con» o «Compartir», en JSON: [{id, nombre, tipo}]. */
    @JavascriptInterface
    public String recibidos() {
        return deLaPantallaExterna ? "[]" : Recibidos.tomar();
    }

    // ---- LA MÚSICA Y LA PANTALLA ----

    @JavascriptInterface
    public void sonando(boolean si) {
        Servicio.marcar(actividad, this, si);
        if (si && !deLaPantallaExterna) {
            actividad.runOnUiThread(actividad::pedirAvisos);
        }
    }

    @JavascriptInterface
    public void encendida(boolean si) {
        if (!deLaPantallaExterna) {
            actividad.runOnUiThread(() -> actividad.pantallaEncendida(si));
        }
    }

    /** Para que una ventana que se cierra no se quede «sonando». */
    void olvidar() {
        Servicio.marcar(actividad, this, false);
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
