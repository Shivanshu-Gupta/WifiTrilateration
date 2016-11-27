package com.wireless.asst.wifitrilateration;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.wireless.asst.wifitrilateration.trilateration.NonLinearLeastSquaresSolver;
import com.wireless.asst.wifitrilateration.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.util.DoubleArray;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static android.R.layout.simple_list_item_1;


public class MainActivity extends AppCompatActivity {
    public final static String TAG = "WifiTrilateration";
    ListView _usedAPsListView;
    TextView _locationTextView;
    Button _locateButton, _listAPButton, _scanButton;

    WifiManager wifi;
    WifiScanReceiver wifiReceiver;
    boolean scanRequested = false;

    ProgressDialog progressDialog;
    ArrayAdapter<AccessPoint> usedAPsAdapter;

    HashMap<String, Double> APDistance = new HashMap<>();
    HashMap<String, Double> scannedAPs = new HashMap<>();
    ArrayList<AccessPoint> apList;
    ArrayList<AccessPoint> usedAPs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "MainActivity: OnCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _usedAPsListView = (ListView) findViewById(R.id.list_apsused);
        _locationTextView = (TextView) findViewById(R.id.tv_location);
        _locateButton = (Button) findViewById(R.id.bt_locate);
        _listAPButton = (Button) findViewById(R.id.btn_listap);
        _scanButton = (Button) findViewById(R.id.btn_scan);

        _locateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "MainActivity: Locate Button Clicked");
                _locateButton.setEnabled(false);
                APDistance.clear();
                scannedAPs.clear();
                progressDialog = new ProgressDialog(MainActivity.this,
                        R.style.AppTheme);
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage("Scanning for Access Points....");
                progressDialog.show();
                scanRequested = true;
                wifi.startScan();
            }
        });

        _listAPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "MainActivity: See AP Button Clicked");
                Intent intent = new Intent(MainActivity.this, APListActivity.class);
                startActivity(intent);
            }
        });

        _scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifi.startScan();
            }
        });

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false) {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }

        wifiReceiver = new WifiScanReceiver();
        registerReceiver(wifiReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        usedAPs = new ArrayList<>();
        usedAPsAdapter = new ArrayAdapter<>(this, simple_list_item_1, usedAPs);
        _usedAPsListView.setAdapter(usedAPsAdapter);
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "MainActivity: onStart");
        super.onStart();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String aplist_str = prefs.getString("aplist", "");
        if(!aplist_str.equals("")) {
            Type responseType= new TypeToken<ArrayList<AccessPoint>>() { }.getType();
            apList = gson.fromJson(aplist_str, responseType);
        } else {
            apList = new ArrayList<>();
            Toast.makeText(getApplicationContext(), "Configure AP Locations to be able to locate your device.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
//        unregisterReceiver(wifiReceiver);
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        public double calculateDistance(int signalLevelInDb, double freqInMHz) {
            double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(signalLevelInDb)) / 20.0;
            return Math.pow(10.0, exp);
        }

        public ArrayList<double[]> getLocation(HashMap<String, Double> scannedAPs,
                                               HashMap<String, Double> APDistance) {
            //For testing default
//            double[][] positions = new double[][]{{5.0, -6.0}, {13.0, -15.0}, {21.0, -3.0}, {12.4, -21.2}};
//            double[] distances = new double[]{8.06, 13.97, 23.32, 15.31};

            ArrayList<Double> distanceList1 = new ArrayList<>(), distanceList2 = new ArrayList<>();
            ArrayList<double[]> positionList = new ArrayList<>();
            usedAPs.clear();
            int aps_found = 0;
            for(int k = 0; k < apList.size(); k++) {
                Log.d(TAG, apList.get(k).toString());
                AccessPoint ap = apList.get(k);

                if(scannedAPs.containsKey(ap.bssid)) {
                    positionList.add(ap.coords);

                    double distance1 = APDistance.get(ap.bssid);
                    distanceList1.add(distance1);
                    ap.distance1 = distance1;

                    double rssi = scannedAPs.get(ap.bssid);
                    double distance2 = Math.pow(10.0, (rssi + ap.A) / (-10.0 * ap.n));
                    distanceList2.add(distance2);
                    ap.distance2 = distance2;

                    usedAPs.add(ap);
                    aps_found++;
                }
            }

            if(aps_found < 2) {
                return null;
            }

            double[][] positions = new double[aps_found][positionList.get(0).length];
            double[] distances1 = new double[aps_found], distances2 = new double[aps_found];

            for(int i = 0; i < aps_found; i++) {
                positions[i] = positionList.get(i);
                distances1[i] = distanceList1.get(i);
                distances2[i] = distanceList2.get(i);
            }

            NonLinearLeastSquaresSolver solver1 = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances1), new LevenbergMarquardtOptimizer());
            LeastSquaresOptimizer.Optimum optimum1 = solver1.solve();
            double[] centroid1 = optimum1.getPoint().toArray();

            NonLinearLeastSquaresSolver solver2 = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances2), new LevenbergMarquardtOptimizer());
            LeastSquaresOptimizer.Optimum optimum2 = solver2.solve();
            double[] centroid2 = optimum2.getPoint().toArray();

//            Log.d(TAG, "Obtained Location" + Arrays.toString(centroid));
            ArrayList<double[]> locations = new ArrayList<>();
            locations.add(centroid1);
            locations.add(centroid2);
            return locations;
        }

        public void onReceive(Context c, Intent intent) {
            Log.d(TAG, "MainActivity: Wifi Scan Results received");

            ArrayList<ScanResult> results = (ArrayList) wifi.getScanResults();
            double distance;
            for (ScanResult result : results) {
                distance = calculateDistance(result.level, result.frequency);
                if(result.SSID.equals("Micromax") || result.SSID.equals("myAP")) {
                    Log.d(TAG, result.SSID + "[" + result.BSSID + "] : " + result.level + " : " + distance);
                }
                APDistance.put(result.BSSID, distance);
                scannedAPs.put(result.BSSID, (double) result.level);
            }

            ArrayList<double[]> locations = getLocation(scannedAPs, APDistance);
            usedAPsAdapter.notifyDataSetChanged();
            if(scanRequested) {

                if (progressDialog != null) {
                    _locateButton.setEnabled(true);
                    progressDialog.dismiss();
                    scanRequested = false;
                }
                if(locations != null) {
                    _locationTextView.setText("Device Location"
                            + "\nMethod 1: " + Arrays.toString(locations.get(0))
                            + "\nMehtod 2: " + Arrays.toString(locations.get(1)));
                } else {
                    Toast.makeText(getApplicationContext(), "Could not locate device...not enough access points configured", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
