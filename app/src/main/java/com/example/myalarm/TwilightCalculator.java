package com.example.myalarm;

import android.text.format.DateUtils;

import java.util.Date;


public class TwilightCalculator {
    /** Value of {@link #mState} if it is currently day */
    public static final int DAY = 0;
    /** Value of {@link #mState} if it is currently night */
    public static final int NIGHT = 1;
    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;
    // element for calculating solar transit.
    private static final double J0 = 0.0009;
    // correction for civil twilight
    private static final double ALTITUDE_CORRECTION_CIVIL_TWILIGHT = -0.104719755;
    // coefficients for calculating Equation of Center.
    private static final double C1 = 0.0334196;
    private static final double C2 = 0.000349066;
    private static final double C3 = 0.000005236;
    private static final double OBLIQUITY = 0.40927971;
    // Java time on Jan 1, 2000 12:00 UTC.
    private static final long UTC_2000 = 946728000000L;
    /**
     * Time of sunset (civil twilight) in milliseconds or -1 in the case the day
     * or night never ends.
     */
    public long mSunset;
    /**
     * Time of sunrise (civil twilight) in milliseconds or -1 in the case the
     * day or night never ends.
     */
    public long mSunrise;
    /** Current state */
    public int mState;
    /**
     * calculates the civil twilight bases on time and geo-coordinates.
     *
     * @param time time in milliseconds.
     * @param latitude latitude in degrees.
     * @param longitude latitude in degrees.
     */
    public void calculateTwilight(long time, double latitude, double longitude) {
        final double daysSince2000 = (time - UTC_2000) / DateUtils.DAY_IN_MILLIS;
        // mean anomaly
        final double meanAnomaly = 6.240059968 + daysSince2000 * 0.01720197;
        // true anomaly
        final double trueAnomaly = meanAnomaly + C1 * Math.sin(meanAnomaly) + C2
                * Math.sin(2 * meanAnomaly) + C3 * Math.sin(3 * meanAnomaly);
        // ecliptic longitude
        final double solarLng = trueAnomaly + 1.796593063 + Math.PI;
        // solar transit in days since 2000
        final double arcLongitude = -longitude / 360;
        float n = Math.round(daysSince2000 - J0 - arcLongitude);
        double solarTransitJ2000 = n + J0 + arcLongitude + 0.0053f * Math.sin(meanAnomaly)
                + -0.0069 * Math.sin(2 * solarLng);
        // declination of sun
        double solarDec = Math.asin(Math.sin(solarLng) * Math.sin(OBLIQUITY));
        final double latRad = latitude * DEGREES_TO_RADIANS;
        double cosHourAngle = (Math.sin(ALTITUDE_CORRECTION_CIVIL_TWILIGHT) - Math.sin(latRad)
                * Math.sin(solarDec)) / (Math.cos(latRad) * Math.cos(solarDec));
        // The day or night never ends for the given date and location, if this value is out of
        // range.
        if (cosHourAngle >= 1) {
            mState = NIGHT;
            mSunset = -1;
            mSunrise = -1;
            return;
        } else if (cosHourAngle <= -1) {
            mState = DAY;
            mSunset = -1;
            mSunrise = -1;
            return;
        }
        float hourAngle = (float) (Math.acos(cosHourAngle) / (2 * Math.PI));
        mSunset = Math.round((solarTransitJ2000 + hourAngle) * DateUtils.DAY_IN_MILLIS) + UTC_2000;
        mSunrise = Math.round((solarTransitJ2000 - hourAngle) * DateUtils.DAY_IN_MILLIS) + UTC_2000;
        if (mSunrise < time && mSunset > time) {
            mState = DAY;
        } else {
            mState = NIGHT;
        }
    }
    public Date getSunrise()
    {
        if (mSunrise > 0) {
            Date sunrise = new Date(mSunrise);
            return sunrise;
        }
        else {
            return null;
        }

    }
    public Date getSunset()
    {
        if (mSunset > 0) {
            Date sunset = new Date(mSunset);
            return sunset;
        }
        else {
            return null;
        }
    }
}
