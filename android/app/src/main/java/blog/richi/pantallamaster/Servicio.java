package blog.richi.pantallamaster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import java.util.HashSet;
import java.util.Set;

/**
 * LA MÚSICA EN SEGUNDO PLANO. Mientras suena algo largo (la música, un
 * ambiente en bucle) en cualquier ventana de la app, este servicio la tiene
 * «en primer plano», con su aviso en la barra de notificaciones: así Android
 * no la para ni cierra la app aunque se deje para mirar otra cosa. Cuando
 * deja de sonar, se va (con un margen, para el cambio de canción).
 */
public class Servicio extends Service {
    private static final String CANAL = "musica";
    private static final int AVISO = 1;
    private static final long MARGEN = 20000;

    private static final Set<Object> suenan = new HashSet<>();
    private static final Handler principal = new Handler(Looper.getMainLooper());
    private static Context app;
    private static boolean enMarcha;
    private PowerManager.WakeLock despierto;

    private static final Runnable parar = () -> {
        if (suenan.isEmpty() && app != null) {
            app.stopService(new Intent(app, Servicio.class));
        }
    };

    /** Lo dice cada ventana (cada puente): si en ella suena algo largo o no. */
    static void marcar(Context c, Object quien, boolean suena) {
        app = c.getApplicationContext();
        principal.post(() -> {
            if (suena) {
                suenan.add(quien);
            } else {
                suenan.remove(quien);
            }
            principal.removeCallbacks(parar);
            if (!suenan.isEmpty()) {
                arrancar();
            } else {
                principal.postDelayed(parar, MARGEN);
            }
        });
    }

    private static void arrancar() {
        if (enMarcha || app == null) {
            return;
        }
        Intent i = new Intent(app, Servicio.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                app.startForegroundService(i);
            } else {
                app.startService(i);
            }
            enMarcha = true;
        } catch (RuntimeException e) {
            // Android no deja empezarlo ahora (la app ya estaba detrás):
            // suena igual, sólo que sin la garantía de seguir.
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {
        Notification n = aviso();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(AVISO, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(AVISO, n);
        }
        if (despierto == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                despierto = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pantallamaster:musica");
                despierto.setReferenceCounted(false);
                despierto.acquire(6 * 60 * 60 * 1000L);
            }
        }
        return START_NOT_STICKY;
    }

    @SuppressWarnings("deprecation")
    private Notification aviso() {
        Intent abrir = new Intent(this, Principal.class);
        abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, abrir,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CANAL) == null) {
                NotificationChannel canal = new NotificationChannel(CANAL, "Música de la partida", NotificationManager.IMPORTANCE_LOW);
                canal.setDescription("Mientras suena la música o un ambiente, para que siga aunque dejes la app.");
                canal.setShowBadge(false);
                nm.createNotificationChannel(canal);
            }
            b = new Notification.Builder(this, CANAL);
        } else {
            b = new Notification.Builder(this).setPriority(Notification.PRIORITY_LOW);
        }
        return b.setSmallIcon(R.drawable.ic_aviso)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Suena la música de la partida. Tócalo para volver.")
                .setContentIntent(pi)
                .setOngoing(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    @Override
    public void onDestroy() {
        enMarcha = false;
        if (despierto != null && despierto.isHeld()) {
            despierto.release();
        }
        despierto = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
