package richi.pantallamaster;

import android.app.ActivityManager;
import android.net.Uri;
import android.os.Bundle;

/**
 * Otra ventana de la app: la pantalla de los jugadores (cuando no hay
 * pantalla externa) o el reproductor de música. Cada una es su propia
 * tarea (sale aparte en «recientes» y se puede poner al lado de la del
 * máster); se hablan con la del máster como en el navegador, por un
 * BroadcastChannel.
 */
public class Ventana extends Base {
    private String pagina = "jugadores.html";

    @Override
    protected void onCreate(Bundle guardado) {
        Uri u = getIntent().getData();
        String p = u == null || u.getPath() == null ? "" : u.getPath().replaceFirst("^/", "");
        if (p.equals("jugadores.html") || p.equals("reproductor.html")) {
            pagina = p;
        }
        super.onCreate(guardado);
        String titulo = pagina.equals("reproductor.html") ? "Reproductor de música" : "Pantalla de jugadores";
        setTitle(titulo);
        setTaskDescription(new ActivityManager.TaskDescription(titulo));
    }

    @Override
    protected String pagina() {
        return pagina;
    }

    @Override
    protected void atrasSinNada() {
        finishAndRemoveTask();
    }

    @Override
    void abrirVentana(String otra) {
        // Desde estas ventanas no se abren otras.
    }
}
