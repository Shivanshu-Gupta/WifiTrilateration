package com.wireless.asst.wifitrilateration;

import android.text.TextUtils;

import java.util.ArrayList;

/**
 * Created by cyberLab on 06-11-2016.
 */
public class AccessPoint {
    String ssid;
    String bssid;
    ArrayList<Double> coords;
    double distance = -1;

    public AccessPoint(String ssid, String bssid, ArrayList<Double> coords) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.coords = coords;
    }

    @Override
    public String toString() {
        if(distance == -1) {
            return ssid + "[" + bssid + "] - (" + TextUtils.join(", ", coords) + ")";
        } else {
            return ssid + "[" + bssid + "] - (" + TextUtils.join(", ", coords) + ")"
                    + "\n" + distance;
        }
    }
}
