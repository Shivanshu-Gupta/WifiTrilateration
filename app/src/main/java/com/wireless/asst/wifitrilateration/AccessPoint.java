package com.wireless.asst.wifitrilateration;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import static android.R.attr.name;

/**
 * Created by cyberLab on 06-11-2016.
 */
public class AccessPoint implements Parcelable {
    public final static String TAG = "WifiTrilateration";
    String ssid;
    String bssid;
//    ArrayList<Double> coords;
    double[] coords = new double[3];
    double distance1 = -1, distance2 = -1;
    double n, A;


    public AccessPoint(String ssid, String bssid, double[] coords) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.coords = coords.clone();
    }

    @Override
    public String toString() {
        String str = ssid + "[" + bssid + "] : " + Arrays.toString(coords);

        str += "\nn = " + n + ", A = " + A;

        if(distance1 != -1) {
            str += "\nMethod1: " + distance1 + "m";
        }

        if(distance2 != -1) {
            str += "\nMethod2: " + distance2 + "m";
        }

        return str;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(ssid);
        parcel.writeString(bssid);
        parcel.writeDoubleArray(coords);
        parcel.writeDouble(n);
        parcel.writeDouble(A);
    }

    // Creator
    public static final Parcelable.Creator CREATOR
            = new Parcelable.Creator() {
        public AccessPoint createFromParcel(Parcel in) {
            return new AccessPoint(in);
        }

        public AccessPoint[] newArray(int size) {
            return new AccessPoint[size];
        }
    };

    // "De-parcel object
    public AccessPoint(Parcel in) {
        ssid = in.readString();
        bssid = in.readString();
        in.readDoubleArray(coords);
        n = in.readDouble();
        A = in.readDouble();
    }
}
