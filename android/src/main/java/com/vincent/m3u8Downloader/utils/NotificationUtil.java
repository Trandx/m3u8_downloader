package com.vincent.m3u8Downloader.utils;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.vincent.m3u8Downloader.bean.M3U8TaskState;

 

 /**
  @Author: Jean-baptiste
 * @CreateAt: 2022/03/19 16:19 
 * @Desc: notification utils
 */
public class NotificationUtil {
    public static final int NOTIFICATION_ID = 9527;
    public static final String NOTIFICATION_CHANNEL_ID = "M3U8_DOWNLOADER_NOTIFICATION";
    public static final String ACTION_SELECT_NOTIFICATION = "SELECT_NOTIFICATION";

    @SuppressLint("StaticFieldLeak")
    private static NotificationUtil instance;
    private NotificationCompat.Builder builder;
    private android.app.NotificationManager notificationManager;
    private Context context;
    private int notificationProgress = -100;

    public static NotificationUtil getInstance(){
        if (null == instance) {
            instance = new NotificationUtil();
        }
        return instance;
    }

   /**
     * Avis de construction
     * @param c Contexte
     */
    public void build(Context c) {
        if (notificationManager != null) return;
        this.context = c;

        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library

            CharSequence name = context.getApplicationInfo().loadLabel(context.getPackageManager());
            int importance = android.app.NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setSound(null, null);

            // Add the channel
            notificationManager = context.getSystemService(android.app.NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Create the notification
        builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_download)  // Icône de notification
                .setOnlyAlertOnce(true)
                .setAutoCancel(true) // Pas d'annulation automatique par défaut
                .setPriority(NotificationCompat.PRIORITY_DEFAULT); // Priorité par défaut
    }

    /**
     * Notification de mise à jour
     * @param state download state
     * @param progress download progress
     */
    public void updateNotification(String fileName, M3U8TaskState state, int progress) {
        if (builder == null) return;

        builder.setContentTitle(fileName == null || fileName.equals("") ? "Télécharger le fichier M3U8" : fileName);
        switch (state) {
            case PREPARE:
                notificationProgress = -100;
                builder.setContentText("Prêt pour le téléchargement").setProgress(0, 0, true);
                builder.setOngoing(true)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done);
            case PENDING:
                notificationProgress = -100;
                builder.setContentText("En attente de téléchargement...").setProgress(0, 0, true);
                builder.setOngoing(true)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done);
                break;
            case DOWNLOADING:
                // Contrôlez la fréquence de rafraîchissement de la notification
                if (progress < 100 && (progress - notificationProgress < 2)) {
                    return;
                }
                notificationProgress = progress;
                builder.setContentText("Téléchargement...")
                        .setProgress(100, progress, false);
                builder.setOngoing(true)
                        .setSmallIcon(android.R.drawable.stat_sys_download);
                break;
            case PAUSE:
                builder.setContentText("Téléchargement en pause");
                builder.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download);
                break;
            case SUCCESS:
                // Cliquez pour sauter
                Intent intent = new Intent(context, getMainActivityClass(context));
                intent.setAction(ACTION_SELECT_NOTIFICATION);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(pendingIntent);

                builder.setContentText("Téléchargement complet").setProgress(0, 0, false);
                builder.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done);
                break;
            case ERROR:
            case ENOSPC:
                builder.setContentText("Téléchargement échoué").setProgress(0, 0, false);
                builder.setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done);
                break;
            default:
                break;
        }
        // Show the notification
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }

    public NotificationCompat.Builder getBuilder() {
        return builder;
    }

    public void cancel() {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
            notificationManager = null;
        }
    }

    private Class getMainActivityClass(Context context) {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
