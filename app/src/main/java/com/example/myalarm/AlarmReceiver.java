package com.example.myalarm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    public static Ringtone ringtone = null;
    private static final int[] ringtones = new int[] {RingtoneManager.TYPE_ALARM, RingtoneManager.TYPE_RINGTONE};

    @Override
    public void onReceive(Context context, Intent intent) {

        int id = 0;
        int ringtoneId = 0;
        if (intent != null)
        {
            if (intent.hasExtra("id")) {
                id = intent.getExtras().getInt("id");
            }
            if (intent.hasExtra("ringtone")) {
                ringtoneId = intent.getExtras().getInt("ringtone");
            }
        }
        Intent reopenIntent = new Intent(context, MainActivity.class);
        reopenIntent.putExtra("id", id);
        PendingIntent reopenPendingIntent = PendingIntent.getActivity(context, 0, reopenIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID).
                setSmallIcon(R.mipmap.ic_launcher).
                setContentTitle("Будильник").
                setContentText("Время пришло").
                setContentIntent(reopenPendingIntent);

        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);

        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }

        Uri signalUri = RingtoneManager.getDefaultUri(ringtones[ringtoneId]);
        if (signalUri == null) {
            signalUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }
        if (signalUri == null) {
            signalUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        ringtone = RingtoneManager.getRingtone(context, signalUri);
        ringtone.play();
    }
}