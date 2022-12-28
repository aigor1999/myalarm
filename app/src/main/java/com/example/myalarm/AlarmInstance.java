package com.example.myalarm;

import android.content.Context;

public class AlarmInstance {
    private int id;
    private int hour;
    private int min;
    private boolean repeat;
    private String img;
    private int ringtone;
    private Context context;

    public AlarmInstance(Context context, int id, int hour, int min, boolean repeat, String img, int ringtone)
    {
        this.context = context;
        this.id = id;
        this.hour = hour;
        this.min = min;
        this.repeat = repeat;
        this.img = img;
        this.ringtone = ringtone;
    }

    public int getId() {
        return id;
    }

    public int getHour() {
        return hour;
    }

    public int getMin() {
        return min;
    }

    public boolean canRepeat() {
        return repeat;
    }

    public String getImgPath() {
        return img;
    }

    public int getRingtone() {
        return ringtone;
    }

    @Override
    public String toString()
    {
        String formattedTime = String.format("%02d:%02d", hour, min);
        String message = context.getResources().getString(R.string.enabled) + formattedTime;
        if (repeat){
            message += " - " + context.getResources().getString(R.string.repeat);
        }
        return  message;
    }
}
