package com.example.myalarm;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SetAlarmActivity extends AppCompatActivity {

    private AlarmDbHelper dbHelper;
    private String photoUri = null;
    private Uri outputUri;
    private int id = -1;
    private boolean newAlarm = true;

    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK){
                    Intent data = result.getData();
                    Uri uri = data.getData();
                    ImageView imageView = findViewById(R.id.appliedImageView);
                    imageView.setImageURI(uri);
                    photoUri = uri.toString();
                    //для повторной загрузки при перезапуске
                    ContentResolver resolver = getContentResolver();
                    resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            });

    private ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK){
                    ImageView imageView = findViewById(R.id.appliedImageView);
                    Intent data = result.getData();
                    if (data != null && data.hasExtra("data")) {
                        Bundle extras = data.getExtras();
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        imageView.setImageBitmap(bitmap);
                    }
                    else {
                        imageView.setImageURI(outputUri);
                        photoUri = outputUri.toString();
                    }
                }
            });

    private ActivityResultLauncher<String> requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted){
                    GetSunrisesAndSunsets();
                }
                else {
                    LocationUnavailable();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm);
        TimePicker picker = findViewById(R.id.alarmTimePicker);
        picker.setIs24HourView(true);

        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        String dateMessage = getResources().getString(R.string.current_day) + df.format(Calendar.getInstance().getTime());
        TextView dateText = findViewById(R.id.currentDayTextView);
        dateText.setText(dateMessage);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("new"))
        {
            newAlarm = intent.getExtras().getBoolean("new");
            if (!newAlarm) {
                if (intent.hasExtra("id") && intent.hasExtra("repeat") && intent.hasExtra("hour")
                        && intent.hasExtra("min") && intent.hasExtra("img") && intent.hasExtra("ringtone")) {
                    id = intent.getExtras().getInt("id");
                    boolean repeatAlarm = intent.getExtras().getBoolean("repeat");
                    int hour = intent.getExtras().getInt("hour");
                    int min = intent.getExtras().getInt("min");
                    picker.setHour(hour);
                    picker.setMinute(min);
                    SwitchCompat repeatSwitch = findViewById(R.id.repeatSwitch);
                    repeatSwitch.setChecked(repeatAlarm);
                    photoUri = intent.getExtras().getString("img");
                    int ringtone = intent.getExtras().getInt("ringtone");
                    Spinner ringtoneSpinner = findViewById(R.id.ringtoneSpinner);
                    ringtoneSpinner.setSelection(ringtone);
                    String message = SetMessage(hour, min, repeatAlarm);
                    TextView statusText = findViewById(R.id.setTimeTextView);
                    statusText.setText(message);
                    if (photoUri != null) {
                        Uri uri = Uri.parse(photoUri);
                        ImageView imageView = findViewById(R.id.appliedImageView);
                        imageView.setImageURI(uri);
                    }
                }
                else {
                    Toast.makeText(this, R.string.load_fail, Toast.LENGTH_SHORT);
                    finish();
                }
            }
            dbHelper = new AlarmDbHelper(this);
        }
        else {
            Toast.makeText(this, R.string.load_fail, Toast.LENGTH_SHORT);
            finish();
        }

        GetSunrisesAndSunsets();
    }

    public void SetButtonClick(View v) {
        TimePicker timePicker = findViewById(R.id.alarmTimePicker);
        SwitchCompat repeatSwitch = findViewById(R.id.repeatSwitch);
        Spinner ringtoneSpinner = findViewById(R.id.ringtoneSpinner);
        SaveAlarm(timePicker.getHour(), timePicker.getMinute(), repeatSwitch.isChecked(), ringtoneSpinner.getSelectedItemPosition());
        finish();
    }

    public void ResetButtonClick(View v) {
        if (!newAlarm) {
            Intent intent = new Intent(this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, intent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            ResetTime();
        }
        finish();
    }

    private void ResetTime() {
        new Thread(() -> {
            ContentValues activeValues = new ContentValues();
            activeValues.put(AlarmDbHelper.ACTIVE_COLUMN, 0);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.update(AlarmDbHelper.TABLE_NAME, activeValues, AlarmDbHelper.ID_COLUMN + "=" + id, null);
            dbHelper.close();
        }).start();
    }

    private void SaveAlarm(int hour, int minute, boolean repeat, int ringtone) {
        new Thread(() -> {
            ContentValues newValues = new ContentValues();
            newValues.put(AlarmDbHelper.HOUR_COLUMN, hour);
            newValues.put(AlarmDbHelper.MINUTE_COLUMN, minute);
            newValues.put(AlarmDbHelper.ACTIVE_COLUMN, 1);
            if (repeat){
                newValues.put(AlarmDbHelper.TYPE_COLUMN, AlarmDbHelper.REPEAT);
            }
            else{
                newValues.put(AlarmDbHelper.TYPE_COLUMN, AlarmDbHelper.ONCE);
            }
            newValues.put(AlarmDbHelper.IMG_COLUMN, photoUri);
            newValues.put(AlarmDbHelper.RINGTONE_COLUMN, ringtone);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (newAlarm) {
                id = (int)db.insert(AlarmDbHelper.TABLE_NAME, null, newValues);
            }
            else {
                db.update(AlarmDbHelper.TABLE_NAME, newValues, AlarmDbHelper.ID_COLUMN + "=" + id, null);
            }
            dbHelper.close();
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
        }).start();
    }

    private String SetMessage(int hour, int minute, boolean repeat) {
        String formattedTime = String.format("%02d:%02d", hour, minute);
        String message = getResources().getString(R.string.enabled) + formattedTime;
        if (repeat){
            message += " - " + getResources().getString(R.string.repeat);
        }
        return message;
    }

    private void GetSunrisesAndSunsets() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setCostAllowed(false);
        String provider = locationManager.getBestProvider(criteria, true);
        if (provider != null) {
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    long today = System.currentTimeMillis();
                    long tomorrow = today + DateUtils.DAY_IN_MILLIS;
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    TwilightCalculator calculator = new TwilightCalculator();
                    calculator.calculateTwilight(today, latitude, longitude);
                    Date todaySunrise = calculator.getSunrise();
                    Date todaySunset = calculator.getSunset();
                    calculator.calculateTwilight(tomorrow, latitude, longitude);
                    Date tomorrowSunrise = calculator.getSunrise();
                    Date tomorrowSunset = calculator.getSunset();
                    OutSunrisesAndSunsets(todaySunrise, todaySunset, tomorrowSunrise, tomorrowSunset);
                }
            };
            locationManager.requestSingleUpdate(provider, locationListener, null);
        }
        else
        {
            LocationUnavailable();
        }
    }
    private void LocationUnavailable()
    {
        String unavailable = getResources().getString(R.string.unavailable);
        String todaySunriseMessage = getResources().getString(R.string.today_sunrise) + unavailable;
        String todaySunsetMessage = getResources().getString(R.string.today_sunset) + unavailable;
        String tomorrowSunriseMessage = getResources().getString(R.string.tomorrow_sunrise) + unavailable;
        String tomorrowSunsetMessage = getResources().getString(R.string.tomorrow_sunset) + unavailable;
        TextView todaySunrise = findViewById(R.id.todaySunriseTextView);
        todaySunrise.setText(todaySunriseMessage);
        TextView todaySunset = findViewById(R.id.todaySunsetTextView);
        todaySunset.setText(todaySunsetMessage);
        TextView tomorrowSunrise = findViewById(R.id.tomorrowSunriseTextView);
        tomorrowSunrise.setText(tomorrowSunriseMessage);
        TextView tomorrowSunset = findViewById(R.id.tomorrowSunsetTextView);
        tomorrowSunset.setText(tomorrowSunsetMessage);
    }
    private void OutSunrisesAndSunsets(Date todaySunrise, Date todaySunset, Date tomorrowSunrise, Date tomorrowSunset)
    {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        String no = getResources().getString(R.string.no);
        String todaySunriseMessage, todaySunsetMessage, tomorrowSunriseMessage, tomorrowSunsetMessage;
        if (todaySunrise != null) {
            todaySunriseMessage = getResources().getString(R.string.today_sunrise) + df.format(todaySunrise);
        }
        else {
            todaySunriseMessage = getResources().getString(R.string.today_sunrise) + no;
        }
        if (todaySunset != null){
            todaySunsetMessage = getResources().getString(R.string.today_sunset) + df.format(todaySunset);
        }
        else{
            todaySunsetMessage = getResources().getString(R.string.today_sunset) + no;
        }
        if (tomorrowSunrise != null){
            tomorrowSunriseMessage = getResources().getString(R.string.tomorrow_sunrise) + df.format(tomorrowSunrise);
        }
        else{
            tomorrowSunriseMessage = getResources().getString(R.string.tomorrow_sunrise) + no;
        }
        if (tomorrowSunset != null){
            tomorrowSunsetMessage = getResources().getString(R.string.tomorrow_sunset) + df.format(tomorrowSunset);
        }
        else {
            tomorrowSunsetMessage = getResources().getString(R.string.tomorrow_sunset) + no;
        }

        TextView todaySunriseTextView = findViewById(R.id.todaySunriseTextView);
        todaySunriseTextView.setText(todaySunriseMessage);
        TextView todaySunsetTextView = findViewById(R.id.todaySunsetTextView);
        todaySunsetTextView.setText(todaySunsetMessage);
        TextView tomorrowSunriseTextView = findViewById(R.id.tomorrowSunriseTextView);
        tomorrowSunriseTextView.setText(tomorrowSunriseMessage);
        TextView tomorrowSunsetTextView = findViewById(R.id.tomorrowSunsetTextView);
        tomorrowSunsetTextView.setText(tomorrowSunsetMessage);
    }

    public void AddPhoto(View v)
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryLauncher.launch(intent);
    }

    public void TakePhoto(View v)
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = String.format("IMG_%1$s.jpg", timeStamp);
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(storageDir, fileName);
        outputUri = FileProvider.getUriForFile(this, "igor.customFileProvider", file);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        cameraLauncher.launch(intent);
    }

    public void RemovePhoto(View v)
    {
        photoUri = null;
        ImageView imageView = findViewById(R.id.appliedImageView);
        imageView.setImageDrawable(null);
    }
}