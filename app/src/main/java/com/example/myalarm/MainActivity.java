package com.example.myalarm;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    public static final String CHANNEL_ID = "MY_ALARM_CHANNEL";
    private AlarmDbHelper dbHelper;
    private ArrayList<AlarmInstance> alarms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        dbHelper = new AlarmDbHelper(this);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "My alarm channel", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Уведомления моего будильника");
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);

        if (AlarmReceiver.ringtone != null && AlarmReceiver.ringtone.isPlaying()) {
            AlarmReceiver.ringtone.stop();
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("id")){
            int id = intent.getExtras().getInt("id");
            GetSavedAlarms(id);
            return;
        }

        GetSavedAlarms(0);
    }

    @Override
    public void onRestart(){
        super.onRestart();
        GetSavedAlarms(0);
    }

    private void GetSavedAlarms(int resetId) {
        new Thread(() -> {
            alarms.clear();
            boolean setNextDay = false;
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (resetId > 0){
                ContentValues activeValues = new ContentValues();
                activeValues.put(AlarmDbHelper.ACTIVE_COLUMN, 0);
                int resetCount = db.update(AlarmDbHelper.TABLE_NAME, activeValues, AlarmDbHelper.ID_COLUMN + "=" + resetId + " AND " +
                        AlarmDbHelper.TYPE_COLUMN + "=" + AlarmDbHelper.ONCE, null);
                if (resetCount == 0){
                    //ежедневный будильник
                    setNextDay = true;
                }
            }
            Cursor cursor = db.query(AlarmDbHelper.TABLE_NAME, null, AlarmDbHelper.ACTIVE_COLUMN + "=1",
                    null, null, null, null, null);
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex(AlarmDbHelper.ID_COLUMN);
                int hourIndex = cursor.getColumnIndex(AlarmDbHelper.HOUR_COLUMN);
                int minuteIndex = cursor.getColumnIndex(AlarmDbHelper.MINUTE_COLUMN);
                int typeIndex = cursor.getColumnIndex(AlarmDbHelper.TYPE_COLUMN);
                int imgIndex = cursor.getColumnIndex(AlarmDbHelper.IMG_COLUMN);
                int ringtoneIndex = cursor.getColumnIndex(AlarmDbHelper.RINGTONE_COLUMN);
                int id = cursor.getInt(idIndex);
                int hour = cursor.getInt(hourIndex);
                int minute = cursor.getInt(minuteIndex);
                int type = cursor.getInt(typeIndex);
                String photoUri = cursor.getString(imgIndex);
                int ringtone = cursor.getInt(ringtoneIndex);
                boolean repeatAlarm = type == AlarmDbHelper.REPEAT;
                AlarmInstance alarm = new AlarmInstance(this, id, hour, minute, repeatAlarm, photoUri, ringtone);
                alarms.add(alarm);
                if (setNextDay && id == resetId){
                    //перезапуск ежедневного будильника
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    Intent intent = new Intent(this, AlarmReceiver.class);
                    intent.putExtra("id", id);
                    intent.putExtra("ringtone", ringtone);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, intent, 0);
                    long time = calendar.getTimeInMillis() - calendar.getTimeInMillis() % DateUtils.MINUTE_IN_MILLIS;
                    if (System.currentTimeMillis() > time) {
                        time += DateUtils.DAY_IN_MILLIS;
                    }
                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
                }
            }
            cursor.close();

            dbHelper.close();
            MainActivity.this.runOnUiThread(() -> {
                TextView header = findViewById(R.id.headerTextView);
                if (alarms.size() == 0) {
                    header.setText(R.string.no_active_alarms);
                }
                else{
                    header.setText(R.string.active_alarm_list);
                }
                ListView alarmListView = findViewById(R.id.alarmListView);
                ArrayAdapter<AlarmInstance> adapter = new ArrayAdapter<AlarmInstance>(this, R.layout.my_list_item, alarms);
                alarmListView.setAdapter(adapter);
                alarmListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        AlarmInstance alarm = alarms.get(position);
                        Intent intent = new Intent(MainActivity.this, SetAlarmActivity.class);
                        intent.putExtra("id", alarm.getId());
                        intent.putExtra("new", false);
                        intent.putExtra("hour", alarm.getHour());
                        intent.putExtra("min", alarm.getMin());
                        intent.putExtra("repeat", alarm.canRepeat());
                        intent.putExtra("img", alarm.getImgPath());
                        intent.putExtra("ringtone", alarm.getRingtone());
                        startActivity(intent);
                    }
                });
            });
        }).start();
    }


    public void AddAlarm(View v){
        Intent intent = new Intent(this, SetAlarmActivity.class);
        intent.putExtra("new", true);
        startActivity(intent);
    }
}