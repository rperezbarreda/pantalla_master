package blog.richi.pantallamaster;

import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.webkit.WebView;
import android.widget.Toast;

/**
 * LA PANTALLA DEL MÁSTER (index.html). La de los jugadores sale en la
 * pantalla externa si hay una conectada (una tele o un monitor por HDMI o
 * USB-C) y, si no, en otra ventana de la app, al lado si cabe (en las
 * tablets, a pantalla partida). El reproductor, siempre en otra ventana.
 */
public class Principal extends Base {
    private Jugadores enLaTele;

    @Override
    protected String pagina() {
        return "index.html";
    }

    @Override
    protected void onCreate(Bundle guardado) {
        super.onCreate(guardado);
        limpiarCache();
    }

    @Override
    protected void atrasSinNada() {
        // Sin cerrarla: la partida sigue donde estaba al volver.
        moveTaskToBack(true);
    }

    @Override
    void abrirVentana(String pagina) {
        if ("jugadores.html".equals(pagina)) {
            Display tele = pantallaExterna();
            if (tele != null) {
                if (enLaTele == null || !enLaTele.isShowing() || enLaTele.getDisplay().getDisplayId() != tele.getDisplayId()) {
                    cerrarTele();
                    enLaTele = new Jugadores(this, tele);
                    enLaTele.show();
                }
                Toast.makeText(this, "La pantalla de jugadores, en la pantalla externa.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        Intent i = new Intent(this, Ventana.class);
        i.setData(Uri.parse(Web.RAIZ + pagina));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        startActivity(i);
    }

    private Display pantallaExterna() {
        DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) {
            return null;
        }
        for (Display d : dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)) {
            if (d.getDisplayId() != Display.DEFAULT_DISPLAY) {
                return d;
            }
        }
        return null;
    }

    private void cerrarTele() {
        if (enLaTele != null) {
            enLaTele.dismiss();
            enLaTele = null;
        }
    }

    @Override
    protected void onDestroy() {
        cerrarTele();
        super.onDestroy();
    }

    /** La pantalla de jugadores en la pantalla externa. */
    private static final class Jugadores extends Presentation {
        private final Base duena;
        private WebView web;

        Jugadores(Base duena, Display d) {
            super(duena, d);
            this.duena = duena;
        }

        @Override
        protected void onCreate(Bundle guardado) {
            super.onCreate(guardado);
            web = Web.crear(getContext(), new Puente(duena, true), this::dismiss);
            web.setBackgroundColor(duena.getColor(R.color.fondo));
            setContentView(web);
            web.loadUrl(Web.RAIZ + "jugadores.html");
        }

        @Override
        protected void onStop() {
            if (web != null) {
                web.destroy();
                web = null;
            }
            super.onStop();
        }
    }
}
