package blog.richi.pantallamaster;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * GUARDAR EN UNA CARPETA (si se elige en ⚙ opciones; si no, todo va dentro
 * de la app, como en el navegador). Es lo mismo que «mis-datos» en el
 * programa de escritorio (lanzador/almacen.go), con el mismo formato, así
 * que la carpeta se puede llevar del ordenador a la tablet y al revés:
 *   almacen.json        {clave: valor}: lo que la web guarda en localStorage
 *   archivos/<id>       las imágenes, PDF y música que se suben
 *   archivos/<id>.tipo  su tipo (image/png…)
 * La carpeta la elige cada uno con el selector de Android y sigue ahí aunque
 * se desinstale la app.
 *
 * Una sola copia de los datos para toda la app (la ventana del máster, la de
 * los jugadores, el reproductor…): cada ventana pide y pone clave a clave,
 * así ninguna pisa lo que ha guardado otra. Se escribe en la carpeta un
 * momento después del último cambio (y al dejar la app).
 */
final class Carpeta {
    private static final String PREFS = "carpeta";
    private static final String ARBOL = "arbol";
    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_-]{1,80}$");
    private static Carpeta una;

    private final Context ctx;
    private final ContentResolver cr;
    private final Handler hilo;
    private final Object escribiendo = new Object();
    private Uri arbol;
    private String raiz;
    private String archivos;
    private final Map<String, Map<String, String>> hijos = new HashMap<>();
    private Map<String, String> datos;
    private boolean sucio;

    static synchronized Carpeta de(Context c) {
        if (una == null) {
            una = new Carpeta(c.getApplicationContext());
        }
        return una;
    }

    private Carpeta(Context c) {
        ctx = c;
        cr = c.getContentResolver();
        HandlerThread h = new HandlerThread("carpeta");
        h.start();
        hilo = new Handler(h.getLooper());
        cargar();
    }

    // ---- QUÉ CARPETA ----

    private synchronized void cargar() {
        arbol = null;
        raiz = null;
        archivos = null;
        hijos.clear();
        datos = null;
        sucio = false;
        String s = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ARBOL, null);
        if (s == null) {
            return;
        }
        Uri u = Uri.parse(s);
        for (UriPermission p : cr.getPersistedUriPermissions()) {
            if (p.getUri().equals(u) && p.isReadPermission() && p.isWritePermission()) {
                arbol = u;
                raiz = raizDe(u);
                return;
            }
        }
    }

    /** La propia carpeta o, si dentro hay un «mis-datos» con datos (copiado del ordenador), ese. */
    private String raizDe(Uri u) {
        String id = DocumentsContract.getTreeDocumentId(u);
        if (hijo(id, "almacen.json") == null) {
            String md = hijo(id, "mis-datos");
            if (md != null && hijo(md, "almacen.json") != null) {
                return md;
            }
        }
        return id;
    }

    synchronized boolean hay() {
        return arbol != null && raiz != null;
    }

    /** Si se eligió una carpeta y Android ya no deja usarla (se borró, o se quitó el permiso). */
    synchronized boolean perdida() {
        return arbol == null && ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ARBOL, null) != null;
    }

    synchronized String nombre() {
        if (!hay()) {
            return "";
        }
        try (Cursor c = cr.query(doc(raiz), new String[] { Document.COLUMN_DISPLAY_NAME }, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                return c.getString(0);
            }
        } catch (RuntimeException e) {
            // Sin nombre: se dice «tu carpeta».
        }
        return "tu carpeta";
    }

    /** Lo que hay en una carpeta recién elegida: si ya tiene datos de la pantalla de máster. */
    boolean tieneDatos(Uri u) {
        synchronized (this) {
            Uri antes = arbol;
            arbol = u;
            try {
                String r = raizDe(u);
                return hijo(r, "almacen.json") != null;
            } finally {
                arbol = antes;
                hijos.clear();
            }
        }
    }

    /** Usar esta carpeta a partir de ahora (o ninguna, con null: dentro de la app). */
    void usar(Uri u) {
        guardarYa();
        String antes = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ARBOL, null);
        int permisos = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        if (u != null) {
            cr.takePersistableUriPermission(u, permisos);
        }
        if (antes != null && (u == null || !antes.equals(u.toString()))) {
            try {
                cr.releasePersistableUriPermission(Uri.parse(antes), permisos);
            } catch (SecurityException e) {
                // Ya no estaba.
            }
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(ARBOL, u == null ? null : u.toString()).apply();
        cargar();
    }

    // ---- LOS DOCUMENTOS DE LA CARPETA ----

    private Uri doc(String id) {
        return DocumentsContract.buildDocumentUriUsingTree(arbol, id);
    }

    private synchronized Map<String, String> hijosDe(String padre) {
        Map<String, String> m = hijos.get(padre);
        if (m == null) {
            m = new HashMap<>();
            Uri u = DocumentsContract.buildChildDocumentsUriUsingTree(arbol, padre);
            try (Cursor c = cr.query(u, new String[] { Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME },
                    null, null, null)) {
                while (c != null && c.moveToNext()) {
                    m.put(c.getString(1), c.getString(0));
                }
            } catch (RuntimeException e) {
                // Sin poder leerla: vacía.
            }
            hijos.put(padre, m);
        }
        return m;
    }

    private synchronized String hijo(String padre, String nombre) {
        return hijosDe(padre).get(nombre);
    }

    private synchronized String crear(String padre, String nombre, String tipo) throws IOException {
        String ya = hijo(padre, nombre);
        if (ya != null) {
            return ya;
        }
        Uri nuevo = DocumentsContract.createDocument(cr, doc(padre), tipo, nombre);
        if (nuevo == null) {
            throw new IOException("no se ha podido crear " + nombre);
        }
        String id = DocumentsContract.getDocumentId(nuevo);
        hijosDe(padre).put(nombre, id);
        return id;
    }

    private void escribir(String padre, String nombre, String tipo, InputStream in) throws IOException {
        String id = crear(padre, nombre, tipo);
        try (OutputStream o = cr.openOutputStream(doc(id), "wt")) {
            if (o == null) {
                throw new IOException("no se puede escribir " + nombre);
            }
            byte[] b = new byte[65536];
            int n;
            while ((n = in.read(b)) > 0) {
                o.write(b, 0, n);
            }
        }
    }

    private InputStream abrir(String padre, String nombre) throws IOException {
        String id = hijo(padre, nombre);
        return id == null ? null : cr.openInputStream(doc(id));
    }

    private synchronized void borrar(String padre, String nombre) {
        String id = hijo(padre, nombre);
        if (id == null) {
            return;
        }
        try {
            DocumentsContract.deleteDocument(cr, doc(id));
        } catch (IOException | RuntimeException e) {
            // Si no se deja, se queda (no estorba).
        }
        hijosDe(padre).remove(nombre);
    }

    // ---- EL ALMACÉN (almacen.json) ----

    private synchronized Map<String, String> datos() {
        if (datos != null) {
            return datos;
        }
        datos = new LinkedHashMap<>();
        // Si almacen.json quedó a medias (la app se cerró escribiéndolo), vale la copia de antes.
        for (String nombre : new String[] { "almacen.json", "almacen.copia.json" }) {
            try (InputStream in = abrir(raiz, nombre)) {
                if (in == null) {
                    continue;
                }
                JSONObject o = new JSONObject(new String(Web.leer(in), StandardCharsets.UTF_8));
                for (Iterator<String> k = o.keys(); k.hasNext();) {
                    String clave = k.next();
                    datos.put(clave, o.optString(clave, ""));
                }
                break;
            } catch (IOException | JSONException e) {
                datos.clear();
            }
        }
        return datos;
    }

    synchronized String leerDato(String k) {
        return hay() ? datos().get(k) : null;
    }

    synchronized void ponerDato(String k, String v) {
        if (hay()) {
            datos().put(k, v);
            programar();
        }
    }

    synchronized void quitarDato(String k) {
        if (hay() && datos().remove(k) != null) {
            programar();
        }
    }

    synchronized void vaciarDatos() {
        if (hay()) {
            datos().clear();
            programar();
        }
    }

    synchronized String claves() {
        return hay() ? new JSONArray(new ArrayList<>(datos().keySet())).toString() : "[]";
    }

    synchronized boolean vacia() {
        return !hay() || datos().isEmpty();
    }

    private void programar() {
        sucio = true;
        hilo.removeCallbacks(escribirAlmacen);
        hilo.postDelayed(escribirAlmacen, 400);
    }

    private final Runnable escribirAlmacen = this::guardarYa;

    /** Escribir ya lo que falte (al dejar la app, antes de recargar…). */
    boolean guardarYa() {
        synchronized (escribiendo) {
            byte[] b;
            String r;
            synchronized (this) {
                if (!sucio || datos == null || !hay()) {
                    return true;
                }
                b = new JSONObject(datos).toString().getBytes(StandardCharsets.UTF_8);
                r = raiz;
                sucio = false;
            }
            try {
                // Primero la copia y luego el de verdad: si se corta a medias, queda uno entero.
                escribir(r, "almacen.copia.json", "application/json", new java.io.ByteArrayInputStream(b));
                escribir(r, "almacen.json", "application/json", new java.io.ByteArrayInputStream(b));
                return true;
            } catch (IOException | RuntimeException e) {
                synchronized (this) {
                    sucio = true;
                }
                return false;
            }
        }
    }

    // ---- LOS ARCHIVOS (archivos/) ----

    private synchronized String carpetaArchivos() throws IOException {
        if (archivos == null) {
            archivos = crear(raiz, "archivos", Document.MIME_TYPE_DIR);
        }
        return archivos;
    }

    static boolean idValido(String id) {
        return id != null && ID.matcher(id).matches();
    }

    /** [{id, tipo, tam}], como GET /__archivos en el programa. */
    String lista() {
        JSONArray l = new JSONArray();
        if (!hay()) {
            return l.toString();
        }
        try {
            String a = carpetaArchivos();
            Uri u = DocumentsContract.buildChildDocumentsUriUsingTree(arbol, a);
            List<String[]> filas = new ArrayList<>();
            try (Cursor c = cr.query(u, new String[] { Document.COLUMN_DISPLAY_NAME, Document.COLUMN_SIZE }, null, null, null)) {
                while (c != null && c.moveToNext()) {
                    filas.add(new String[] { c.getString(0), String.valueOf(c.getLong(1)) });
                }
            }
            for (String[] f : filas) {
                if (!idValido(f[0])) {
                    continue;
                }
                JSONObject o = new JSONObject();
                o.put("id", f[0]);
                o.put("tipo", tipoDe(f[0]));
                o.put("tam", Long.parseLong(f[1]));
                l.put(o);
            }
        } catch (IOException | JSONException | RuntimeException e) {
            // Lo que se haya podido.
        }
        return l.toString();
    }

    String tipoDe(String id) {
        try (InputStream in = abrir(carpetaArchivos(), id + ".tipo")) {
            return in == null ? "" : new String(Web.leer(in), StandardCharsets.UTF_8).trim();
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }

    InputStream abrirArchivo(String id) {
        if (!hay() || !idValido(id)) {
            return null;
        }
        try {
            return abrir(carpetaArchivos(), id);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    boolean guardarArchivo(String id, String tipo, File f) {
        if (!hay() || !idValido(id)) {
            return false;
        }
        String t = tipo == null || tipo.length() > 100 ? "" : tipo;
        try (InputStream in = new FileInputStream(f)) {
            String a = carpetaArchivos();
            escribir(a, id, "application/octet-stream", in);
            escribir(a, id + ".tipo", "application/octet-stream",
                    new java.io.ByteArrayInputStream(t.getBytes(StandardCharsets.UTF_8)));
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    void borrarArchivo(String id) {
        if (!hay() || !idValido(id)) {
            return;
        }
        try {
            String a = carpetaArchivos();
            borrar(a, id);
            borrar(a, id + ".tipo");
        } catch (IOException | RuntimeException e) {
            // Si no se puede, se queda.
        }
    }
}
