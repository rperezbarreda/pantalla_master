package richi.pantallamaster;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * «ABRIR CON» Y «COMPARTIR»: lo que otra app le pasa a la pantalla de máster
 * (una copia de seguridad, imágenes, PDF, audios). Se copia a la caché
 * («recibidos/», que se limpia sola) en un hilo aparte, porque el permiso
 * para leerlo dura poco, y luego la web lo pide (android.js →
 * /__recibido/<id>) y decide qué hacer con cada uno (pantalla-master.js,
 * recibirArchivos).
 */
final class Recibidos {
    private static final long MAX = 50L * 1024 * 1024;
    private static final List<JSONObject> listos = new ArrayList<>();

    private Recibidos() {
    }

    @SuppressWarnings("deprecation")
    static void copiar(Context ctx, Intent i, Runnable alTerminar) {
        if (i == null || i.getAction() == null) {
            return;
        }
        List<Uri> uris = new ArrayList<>();
        String accion = i.getAction();
        if (Intent.ACTION_VIEW.equals(accion) && i.getData() != null) {
            uris.add(i.getData());
        } else if (Intent.ACTION_SEND.equals(accion)) {
            Uri u = Build.VERSION.SDK_INT >= 33 ? i.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class)
                    : (Uri) i.getParcelableExtra(Intent.EXTRA_STREAM);
            if (u != null) {
                uris.add(u);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(accion)) {
            ArrayList<Uri> l = Build.VERSION.SDK_INT >= 33 ? i.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri.class)
                    : i.<Uri>getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (l != null) {
                uris.addAll(l);
            }
        }
        ClipData clip = i.getClipData();
        for (int k = 0; uris.isEmpty() && clip != null && k < clip.getItemCount(); k++) {
            if (clip.getItemAt(k).getUri() != null) {
                uris.add(clip.getItemAt(k).getUri());
            }
        }
        if (uris.isEmpty()) {
            return;
        }
        // Que no se vuelva a recibir al girar o volver a la app.
        i.setAction(null);
        Context app = ctx.getApplicationContext();
        new Thread(() -> {
            int malos = 0;
            for (Uri u : uris.subList(0, Math.min(uris.size(), 20))) {
                if (!uno(app, u)) {
                    malos++;
                }
            }
            final int fallos = malos;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (fallos > 0) {
                    Toast.makeText(app, fallos == 1 ? "Un archivo no se ha podido leer (o pasa de 50 MB)."
                            : fallos + " archivos no se han podido leer (o pasan de 50 MB).", Toast.LENGTH_LONG).show();
                }
                alTerminar.run();
            });
        }).start();
    }

    private static boolean uno(Context ctx, Uri u) {
        String nombre = "archivo";
        long tam = -1;
        try (Cursor c = ctx.getContentResolver().query(u, new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE },
                null, null, null)) {
            if (c != null && c.moveToFirst()) {
                if (!c.isNull(0)) {
                    nombre = c.getString(0);
                }
                if (!c.isNull(1)) {
                    tam = c.getLong(1);
                }
            }
        } catch (RuntimeException e) {
            // Sin nombre: «archivo».
        }
        if (tam > MAX) {
            return false;
        }
        String tipo = ctx.getContentResolver().getType(u);
        if (tipo == null || tipo.equals("application/octet-stream")) {
            tipo = Web.tipoDe(nombre);
        }
        File carpeta = new File(ctx.getCacheDir(), "recibidos");
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            return false;
        }
        String id = UUID.randomUUID().toString();
        File f = new File(carpeta, id);
        try (InputStream in = ctx.getContentResolver().openInputStream(u); OutputStream out = new FileOutputStream(f)) {
            if (in == null) {
                return false;
            }
            byte[] b = new byte[65536];
            long total = 0;
            int n;
            while ((n = in.read(b)) > 0) {
                total += n;
                if (total > MAX) {
                    throw new IOException("demasiado grande");
                }
                out.write(b, 0, n);
            }
        } catch (IOException | SecurityException e) {
            Puente.borrar(f);
            return false;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("nombre", nombre.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "-"));
            o.put("tipo", tipo);
            synchronized (listos) {
                listos.add(o);
            }
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /** Los que ya están listos (y se olvidan: la web los pide una vez). */
    static String tomar() {
        synchronized (listos) {
            JSONArray a = new JSONArray(listos);
            listos.clear();
            return a.toString();
        }
    }

    static File archivo(Context ctx, String id) {
        if (id == null || !id.matches("[0-9a-f-]{36}")) {
            return null;
        }
        File f = new File(new File(ctx.getCacheDir(), "recibidos"), id);
        return f.isFile() ? f : null;
    }
}
