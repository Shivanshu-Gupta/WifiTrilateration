package com.wireless.asst.wifitrilateration;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import com.google.gson.Gson;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    public final static String TAG = "WifiTrilateration";
    TextView _locationTextView;
    Button _locateButton, _listAPButton;

    WifiManager wifi;
    HashMap<String, Double> APDistance = new HashMap<>();
    ArrayList<AccessPoint> AccessPointLocations;

    ProgressDialog progressDialog;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "MainActivity: OnCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _locationTextView = (TextView) findViewById(R.id.tv_location);
        _locateButton = (Button) findViewById(R.id.bt_locate);
        _listAPButton = (Button) findViewById(R.id.btn_listap);

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

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false) {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }
        registerReceiver(new WifiScanReceiver(),
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "MainActivity: onStart");
        super.onStart();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String aplist_str = prefs.getString("aplist", "");
        AccessPointLocations = gson.fromJson(aplist_str, ArrayList.class);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        public double calculateDistance(int signalLevelInDb, double freqInMHz) {
            double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(signalLevelInDb)) / 20.0;
            return Math.pow(10.0, exp);
        }

        public ArrayList<Double> getLocation(HashMap<String, Double> APDistance) {
            // TODO: use APDistances and AccessPointLocations to get device Location

            //For testing default
            //double[][] positions = new double[][]{{5.0, -6.0}, {13.0, -15.0}, {21.0, -3.0}, {12.4, -21.2}};
            //double[] distances = new double[]{8.06, 13.97, 23.32, 15.31};

            Integer size = APDistance.size();

            double[] distances = new double[size];
            double[][] positions = new double [size][2];

            Integer i = 0;

            //Convert APDistances into double[] distances
            for(Double dist : APDistance.values()){
                distances[i] = dist;
                i = i + 1;
            }

            i = 0;
            //Convert Access Point Locations into double [] [] positions

            //For the bssids of the distances given
            for(String key : APDistance.keySet()){
                ArrayList<Double>APCoords;
                for(int x = 0; x<AccessPointLocations.size(); x++){

                    if(key == AccessPointLocations.get(x).ssid){
                        APCoords = AccessPointLocations.get(x).coords;
                    }
                }

                positions[i][0] = APCoords.get(0); //x coords
                positions[i][1] = APCoords.get(1); //y coords
            }

            NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());

            LeastSquaresOptimizer.Optimum optimum = solver.solve();

            //Answer
            double[] centroid = optimum.getPoint().toArray();
            Log.d(TAG, "Obtained Location" + centroid.toString());

            ArrayList<Double> coords = new ArrayList<>();

            return coords;
        }

        public void onReceive(Context c, Intent intent) {
            Log.d(TAG, "MainActivity: Wifi Scan Results received");
            ArrayList<ScanResult> results = (ArrayList) wifi.getScanResults();
            double distance;
            for (ScanResult result : results) {
                distance = calculateDistance(result.level, result.frequency);
                Log.d(TAG, result.SSID + "[" + result.BSSID + "] - " + result.level);
                APDistance.put(result.BSSID, distance);
            }

            ArrayList<Double> coords = getLocation(APDistance);
            if (progressDialog != null)
                progressDialog.dismiss();
            _locationTextView.setText(coords.toString());
        }
    }

}
