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
import com.google.gson.reflect.TypeToken;
import com.wireless.asst.wifitrilateration.trilateration.NonLinearLeastSquaresSolver;
import com.wireless.asst.wifitrilateration.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

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
    ArrayList<AccessPoint> APLocations;
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
//        _scanButton = (Button) findViewById(R.id.btn_scan);

        _locateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "MainActivity: Locate Button Clicked");
                _locateButton.setEnabled(false);
                APDistance.clear();
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

//        _scanButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                wifi.startScan();
//            }
//        });

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
            APLocations = gson.fromJson(aplist_str, responseType);
        } else {
            APLocations = new ArrayList<>();
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

        public double[] getLocation(HashMap<String, Double> APDistance) {
            //For testing default
//            double[][] positions = new double[][]{{5.0, -6.0}, {13.0, -15.0}, {21.0, -3.0}, {12.4, -21.2}};
//            double[] distances = new double[]{8.06, 13.97, 23.32, 15.31};

            ArrayList<Double> distanceList = new ArrayList<>();
            ArrayList<double[]> positionList = new ArrayList<>();
            usedAPs.clear();
            int aps_found = 0;
            for(int k = 0; k < APLocations.size(); k++) {
                Log.d(TAG, APLocations.get(k).toString());
                AccessPoint ap = APLocations.get(k);

                if(APDistance.containsKey(ap.bssid)) {
                    Log.d(TAG, ap.bssid + " found with distance " + APDistance.get(ap.bssid));
                    double[] pos = new double[ap.coords.size()];
                    for(int i = 0; i < ap.coords.size(); i++) {
                        pos[i] = ap.coords.get(i);
                    }
                    positionList.add(pos);
                    distanceList.add(APDistance.get(ap.bssid));
                    ap.distance = APDistance.get(ap.bssid);
                    usedAPs.add(ap);
                    aps_found++;
                }
            }

            if(aps_found < 2) {
                return null;
            }

            double[][] positions = new double[aps_found][positionList.get(0).length];
            double[] distances = new double[aps_found];

            for(int i = 0; i < aps_found; i++) {
                positions[i] = positionList.get(i);
                distances[i] = distanceList.get(i);
            }

            NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());

            LeastSquaresOptimizer.Optimum optimum = solver.solve();

            //Answer
            double[] centroid = optimum.getPoint().toArray();
            Log.d(TAG, "Obtained Location" + Arrays.toString(centroid));
            return centroid;
        }

        public void onReceive(Context c, Intent intent) {
            Log.d(TAG, "MainActivity: Wifi Scan Results received");

            ArrayList<ScanResult> results = (ArrayList) wifi.getScanResults();
            double distance;
            for (ScanResult result : results) {
                distance = calculateDistance(result.level, result.frequency);
                if(result.SSID.equals("AndroidAP")) {
                    Log.d(TAG, result.SSID + "[" + result.BSSID + "] : " + result.level + " : " + distance);
                }
                APDistance.put(result.BSSID, distance);
            }

            if(scanRequested) {
                double[] coords = getLocation(APDistance);
                if (progressDialog != null) {
                    usedAPsAdapter.notifyDataSetChanged();
                    _locateButton.setEnabled(true);
                    progressDialog.dismiss();
                    scanRequested = false;
                }
                if(coords != null) {
                    _locationTextView.setText("Device Location: " + Arrays.toString(coords));
                } else {
                    Toast.makeText(getApplicationContext(), "Could not locate device...not enough access points configured", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
