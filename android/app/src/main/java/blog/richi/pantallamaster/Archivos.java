package blog.richi.pantallamaster;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Para abrir con otra app (el visor de PDF, la galería…) un archivo de la
 * pantalla de máster: le presta, sólo para leer, los que se han dejado en
 * la caché («ver/»). Nada más sale de aquí.
 */
public class Archivos extends ContentProvider {
    // La del paquete (la versión de prueba, «.prueba», tiene la suya).
    static Uri uriDe(android.content.Context ctx, String nombre) {
        return new Uri.Builder().scheme("content").authority(ctx.getPackageName() + ".archivos")
                .appendPath("ver").appendPath(nombre).build();
    }

    private File archivoDe(Uri u) throws FileNotFoundException {
        List<String> partes = u.getPathSegments();
        if (getContext() == null || partes.size() != 2 || !"ver".equals(partes.get(0))) {
            throw new FileNotFoundException();
        }
        try {
            File carpeta = new File(getContext().getCacheDir(), "ver").getCanonicalFile();
            File f = new File(carpeta, partes.get(1)).getCanonicalFile();
            if (!carpeta.equals(f.getParentFile()) || !f.isFile()) {
                throw new FileNotFoundException();
            }
            return f;
        } catch (IOException e) {
            throw new FileNotFoundException();
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri u, String modo) throws FileNotFoundException {
        if (!"r".equals(modo)) {
            throw new FileNotFoundException();
        }
        return ParcelFileDescriptor.open(archivoDe(u), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri u, String[] columnas, String seleccion, String[] args, String orden) {
        File f;
        try {
            f = archivoDe(u);
        } catch (FileNotFoundException e) {
            return null;
        }
        String[] cols = columnas != null ? columnas : new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE };
        MatrixCursor c = new MatrixCursor(cols, 1);
        Object[] fila = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) {
                fila[i] = f.getName();
            } else if (OpenableColumns.SIZE.equals(cols[i])) {
                fila[i] = f.length();
            }
        }
        c.addRow(fila);
        return c;
    }

    @Override
    public String getType(Uri u) {
        List<String> partes = u.getPathSegments();
        return partes.isEmpty() ? null : Web.tipoDe(partes.get(partes.size() - 1));
    }

    @Override
    public Uri insert(Uri u, ContentValues v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri u, String s, String[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri u, ContentValues v, String s, String[] a) {
        throw new UnsupportedOperationException();
    }
}
