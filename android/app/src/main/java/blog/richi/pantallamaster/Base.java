package blog.richi.pantallamaster;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Locale;

/**
 * Lo común a las dos clases de ventana de la app (la pantalla del máster,
 * {@link Principal}, y la de los jugadores o el reproductor,
 * {@link Ventana}): el WebView, los colores de las barras, elegir archivos,
 * guardarlos o abrirlos con otra app, la pantalla completa de un vídeo y el
 * botón de atrás.
 */
public abstract class Base extends Activity {
    private static final int PIDE_ARCHIVO = 1;
    private static final int PIDE_GUARDAR = 2;
    private static final int PIDE_CARPETA = 3;
    private static final int PIDE_AVISOS = 4;
    // Las ventanas abiertas: al cambiar dónde se guarda, se recargan todas.
    private static final Set<Base> ABIERTAS = Collections.newSetFromMap(new WeakHashMap<>());
    // Android 15 en adelante: la app se pinta también debajo de las barras
    // del sistema y hay que dejarles su hueco a mano.
    private static final boolean BAJO_LAS_BARRAS = Build.VERSION.SDK_INT >= 35;

    protected WebView web;
    private Puente puente;
    private FrameLayout raiz;
    private ValueCallback<Uri[]> eligiendo;
    private File porGuardar;
    private String nombrePorGuardar;
    private View pantallaCompleta;
    private WebChromeClient.CustomViewCallback alSalirDePantallaCompleta;

    /** La página de esta ventana (index.html, jugadores.html…). */
    protected abstract String pagina();

    /** Lo que hace atrás cuando la página no tiene nada abierto que cerrar. */
    protected abstract void atrasSinNada();

    /** Abrir la pantalla de los jugadores o el reproductor. */
    abstract void abrirVentana(String pagina);

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        raiz = new FrameLayout(this);
        raiz.setBackgroundColor(getColor(R.color.fondo));
        setContentView(raiz);
        prepararBarras();
        ABIERTAS.add(this);
        montarWeb();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::atras);
        }
    }

    private void montarWeb() {
        if (web != null) {
            raiz.removeView(web);
            web.destroy();
        }
        if (puente != null) {
            puente.olvidar();
        }
        puente = new Puente(this, false);
        web = Web.crear(this, puente, () -> runOnUiThread(this::montarWeb));
        web.setWebChromeClient(new Cromo());
        raiz.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        web.loadUrl(Web.RAIZ + pagina());
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        if (web != null) {
            Web.textoDelSistema(web);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Lo que falte por escribir en la carpeta, ya (por si Android cierra la app).
        new Thread(() -> Carpeta.de(this).guardarYa()).start();
    }

    @Override
    protected void onDestroy() {
        ABIERTAS.remove(this);
        if (puente != null) {
            puente.olvidar();
        }
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    // ---- DÓNDE SE GUARDA: una carpeta (como «mis-datos») o dentro de la app ----

    void elegirCarpeta() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(i, PIDE_CARPETA);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Este dispositivo no deja elegir carpetas.", Toast.LENGTH_LONG).show();
        }
    }

    private void alElegirCarpeta(Uri u) {
        String nombre = "esa carpeta";
        try (android.database.Cursor c = getContentResolver().query(
                DocumentsContract.buildDocumentUriUsingTree(u, DocumentsContract.getTreeDocumentId(u)),
                new String[] { DocumentsContract.Document.COLUMN_DISPLAY_NAME }, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                nombre = "«" + c.getString(0) + "»";
            }
        } catch (RuntimeException e) {
            // Sin nombre.
        }
        boolean conDatos = Carpeta.de(this).tieneDatos(u);
        String texto = conDatos
                ? "La carpeta " + nombre + " ya tiene datos de la pantalla de máster: a partir de ahora se usarán esos."
                    + " Lo que hay dentro de la app se queda ahí, por si vuelves."
                : "A partir de ahora se guardará en " + nombre + " (almacen.json y la carpeta «archivos», como «mis-datos»"
                    + " en el programa del ordenador). Al volver, la entrada te ofrece traer lo que tienes dentro de la app.";
        new AlertDialog.Builder(this)
                .setTitle("Guardar en una carpeta")
                .setMessage(texto)
                .setPositiveButton("Usar esta carpeta", (d, w) -> {
                    try {
                        Carpeta.de(this).usar(u);
                        recargarTodas();
                    } catch (SecurityException e) {
                        Toast.makeText(this, "Android no deja usar esa carpeta: elige otra.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    void quitarCarpeta() {
        new AlertDialog.Builder(this)
                .setTitle("Guardarlo dentro de la app")
                .setMessage("Lo de la carpeta se queda en la carpeta (no se borra nada). Dentro de la app verás lo que"
                        + " había antes de elegirla; si quieres lo de la carpeta, exporta antes una copia de seguridad"
                        + " (⚙ → exportar) e impórtala después.")
                .setPositiveButton("Guardar dentro de la app", (d, w) -> {
                    Carpeta.de(this).usar(null);
                    recargarTodas();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static void recargarTodas() {
        for (Base b : new ArrayList<>(ABIERTAS)) {
            if (b.web != null) {
                b.web.reload();
            }
        }
    }

    // ---- LA PANTALLA ENCENDIDA Y EL PERMISO DE LOS AVISOS ----

    void pantallaEncendida(boolean si) {
        if (si) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /** El aviso de «suena la música» (Android 13 en adelante lo pide una vez). */
    void pedirAvisos() {
        if (Build.VERSION.SDK_INT < 33 || !hasWindowFocus()
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }
        android.content.SharedPreferences p = getSharedPreferences("avisos", MODE_PRIVATE);
        if (p.getBoolean("pedido", false)) {
            return;
        }
        p.edit().putBoolean("pedido", true).apply();
        requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, PIDE_AVISOS);
    }

    // ---- ATRÁS: primero cierra lo que haya abierto en la página ----

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        atras();
    }

    private void atras() {
        if (pantallaCompleta != null) {
            salirDePantallaCompleta();
            return;
        }
        if (web == null) {
            atrasSinNada();
            return;
        }
        web.evaluateJavascript("(window.ocAndroid && window.ocAndroid.atras()) ? 1 : 0", (r) -> {
            if (!"1".equals(r)) {
                atrasSinNada();
            }
        });
    }

    // ---- LAS BARRAS DEL SISTEMA, del color del tema ----

    private void prepararBarras() {
        if (!BAJO_LAS_BARRAS) {
            return;
        }
        raiz.setOnApplyWindowInsetsListener((v, ins) -> {
            Insets barras = ins.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            Insets teclado = ins.getInsets(WindowInsets.Type.ime());
            v.setPadding(barras.left, barras.top, barras.right, Math.max(barras.bottom, teclado.bottom));
            return WindowInsets.CONSUMED;
        });
    }

    @SuppressWarnings("deprecation")
    void pintarBarras(int color) {
        raiz.setBackgroundColor(color);
        getWindow().getDecorView().setBackgroundColor(color);
        if (!BAJO_LAS_BARRAS) {
            getWindow().setStatusBarColor(color);
            getWindow().setNavigationBarColor(color);
        }
        // Iconos oscuros sobre fondo claro y al revés (que se lean).
        double l = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        boolean claro = l > 0.6;
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                int ambos = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                c.setSystemBarsAppearance(claro ? ambos : 0, ambos);
            }
        } else {
            View d = getWindow().getDecorView();
            int f = d.getSystemUiVisibility();
            int luz = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) {
                luz |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            d.setSystemUiVisibility(claro ? (f | luz) : (f & ~luz));
        }
    }

    // ---- ELEGIR ARCHIVOS (los <input type="file"> de la página) ----

    private final class Cromo extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
            if (eligiendo != null) {
                eligiendo.onReceiveValue(null);
            }
            eligiendo = cb;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            String[] tipos = tiposDe(p.getAcceptTypes());
            if (tipos.length == 1) {
                i.setType(tipos[0]);
            } else {
                i.setType("*/*");
                if (tipos.length > 1) {
                    i.putExtra(Intent.EXTRA_MIME_TYPES, tipos);
                }
            }
            if (p.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            try {
                startActivityForResult(i, PIDE_ARCHIVO);
            } catch (ActivityNotFoundException e) {
                eligiendo = null;
                cb.onReceiveValue(null);
                Toast.makeText(Base.this, "No hay ninguna app para elegir archivos.", Toast.LENGTH_LONG).show();
            }
            return true;
        }

        @Override
        public void onShowCustomView(View v, CustomViewCallback cb) {
            if (pantallaCompleta != null) {
                cb.onCustomViewHidden();
                return;
            }
            pantallaCompleta = v;
            alSalirDePantallaCompleta = cb;
            ((ViewGroup) getWindow().getDecorView()).addView(v, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            if (Build.VERSION.SDK_INT >= 30 && getWindow().getInsetsController() != null) {
                getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
            }
        }

        @Override
        public void onHideCustomView() {
            salirDePantallaCompleta();
        }
    }

    private void salirDePantallaCompleta() {
        if (pantallaCompleta == null) {
            return;
        }
        ((ViewGroup) getWindow().getDecorView()).removeView(pantallaCompleta);
        pantallaCompleta = null;
        if (Build.VERSION.SDK_INT >= 30 && getWindow().getInsetsController() != null) {
            getWindow().getInsetsController().show(WindowInsets.Type.systemBars());
        }
        if (alSalirDePantallaCompleta != null) {
            alSalirDePantallaCompleta.onCustomViewHidden();
            alSalirDePantallaCompleta = null;
        }
    }

    /**
     * Los tipos del «accept» de la página, para el selector de Android. Las
     * imágenes, el audio y los PDF se filtran; los JSON y las páginas HTML
     * no (cada app los marca a su manera y alguno se quedaría sin poder
     * elegirse).
     */
    static String[] tiposDe(String[] acepta) {
        List<String> tipos = new ArrayList<>();
        if (acepta != null) {
            for (String a : acepta) {
                for (String t : a.split(",")) {
                    String x = t.trim().toLowerCase(Locale.ROOT);
                    if (x.isEmpty()) {
                        continue;
                    }
                    if (!x.startsWith("image/") && !x.startsWith("audio/") && !x.startsWith("video/")
                            && !x.equals("application/pdf")) {
                        return new String[0];
                    }
                    if (!tipos.contains(x)) {
                        tipos.add(x);
                    }
                }
            }
        }
        return tipos.toArray(new String[0]);
    }

    // ---- GUARDAR UN ARCHIVO donde se elija, o ABRIRLO con otra app ----

    void guardarArchivo(File f, String nombre, String tipo) {
        if (porGuardar != null) {
            Puente.borrar(porGuardar);
        }
        String n = nombreSeguro(nombre, tipo);
        porGuardar = f;
        nombrePorGuardar = n;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(tipoSeguro(tipo, n));
        i.putExtra(Intent.EXTRA_TITLE, n);
        try {
            startActivityForResult(i, PIDE_GUARDAR);
        } catch (ActivityNotFoundException e) {
            Puente.borrar(f);
            porGuardar = null;
            Toast.makeText(this, "No hay dónde guardar archivos en este dispositivo.", Toast.LENGTH_LONG).show();
        }
    }

    void verArchivo(File f, String nombre, String tipo) {
        String n = nombreSeguro(nombre, tipo);
        File carpeta = new File(getCacheDir(), "ver");
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            Puente.borrar(f);
            return;
        }
        File destino = new File(carpeta, n);
        Puente.borrar(destino);
        if (!f.renameTo(destino)) {
            Puente.borrar(f);
            Toast.makeText(this, "No se ha podido abrir el archivo.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Archivos.uriDe(this, n), tipoSeguro(tipo, n));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay ninguna app para abrir este archivo: descárgalo.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int pedido, int resultado, Intent datos) {
        if (pedido == PIDE_ARCHIVO) {
            ValueCallback<Uri[]> cb = eligiendo;
            eligiendo = null;
            if (cb == null) {
                return;
            }
            Uri[] elegidos = null;
            if (resultado == RESULT_OK && datos != null) {
                ClipData varios = datos.getClipData();
                if (varios != null && varios.getItemCount() > 0) {
                    elegidos = new Uri[varios.getItemCount()];
                    for (int k = 0; k < elegidos.length; k++) {
                        elegidos[k] = varios.getItemAt(k).getUri();
                    }
                } else if (datos.getData() != null) {
                    elegidos = new Uri[] { datos.getData() };
                }
            }
            cb.onReceiveValue(elegidos);
            return;
        }
        if (pedido == PIDE_CARPETA) {
            if (resultado == RESULT_OK && datos != null && datos.getData() != null) {
                alElegirCarpeta(datos.getData());
            }
            return;
        }
        if (pedido == PIDE_GUARDAR) {
            final File f = porGuardar;
            final String n = nombrePorGuardar;
            porGuardar = null;
            if (f == null) {
                return;
            }
            if (resultado != RESULT_OK || datos == null || datos.getData() == null) {
                Puente.borrar(f);
                Toast.makeText(this, "No se ha guardado.", Toast.LENGTH_SHORT).show();
                return;
            }
            final Uri destino = datos.getData();
            new Thread(() -> {
                boolean bien = copiar(f, destino);
                Puente.borrar(f);
                runOnUiThread(() -> Toast.makeText(this, bien ? "Guardado: " + n + "."
                        : "No se ha podido guardar " + n + ".", Toast.LENGTH_LONG).show());
            }).start();
            return;
        }
        super.onActivityResult(pedido, resultado, datos);
    }

    private boolean copiar(File f, Uri destino) {
        try (InputStream in = new FileInputStream(f);
             OutputStream out = getContentResolver().openOutputStream(destino, "w")) {
            if (out == null) {
                return false;
            }
            byte[] b = new byte[65536];
            int n;
            while ((n = in.read(b)) > 0) {
                out.write(b, 0, n);
            }
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }

    static String nombreSeguro(String nombre, String tipo) {
        String n = nombre == null ? "" : nombre.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "-").trim();
        if (n.startsWith(".")) {
            n = n.substring(1);
        }
        if (n.length() > 120) {
            n = n.substring(0, 120);
        }
        if (n.isEmpty()) {
            n = "archivo";
        }
        if (n.lastIndexOf('.') <= 0) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(soloTipo(tipo));
            if (ext != null) {
                n = n + "." + ext;
            }
        }
        return n;
    }

    static String tipoSeguro(String tipo, String nombre) {
        String t = soloTipo(tipo);
        if (!t.isEmpty() && t.contains("/")) {
            return t;
        }
        String d = Web.tipoDe(nombre);
        return d;
    }

    private static String soloTipo(String tipo) {
        if (tipo == null) {
            return "";
        }
        int pc = tipo.indexOf(';');
        return (pc < 0 ? tipo : tipo.substring(0, pc)).trim().toLowerCase(Locale.ROOT);
    }

    /** Lo de la caché que ya no hace falta (archivos a medias, los vistos de hace días). */
    protected void limpiarCache() {
        new Thread(() -> {
            long antes = System.currentTimeMillis() - 24L * 60 * 60 * 1000;
            for (String c : new String[] { "descargas", "ver" }) {
                File[] fs = new File(getCacheDir(), c).listFiles();
                if (fs == null) {
                    continue;
                }
                for (File f : fs) {
                    if (c.equals("descargas") || f.lastModified() < antes) {
                        Puente.borrar(f);
                    }
                }
            }
        }).start();
    }
}
