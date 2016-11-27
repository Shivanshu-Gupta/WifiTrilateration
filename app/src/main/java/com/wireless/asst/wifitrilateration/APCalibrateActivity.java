package com.wireless.asst.wifitrilateration;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.util.DoubleArray;

import java.util.ArrayList;

import static android.R.layout.simple_list_item_1;
import static org.apache.commons.math3.util.FastMath.log10;

public class APCalibrateActivity extends AppCompatActivity {
    public final static String TAG = "WifiTrilateration";
//    public final static String ACTION_CALIBRATE_AP = "com.wireless.asst.wifitrilateration.CALIBRATE_AP";

    TextView _datapointsText;
    TextView _msgText;
    Button _scanButton;

    AccessPoint ap;

    private WifiManager wifi;
    private WifiScanReceiver wifiReceiver;
    private boolean scanRequested = false;
    private ProgressDialog progressDialog;

    private double[] distances = {0.1, 0.5, 1.0, 2.0};
    private ArrayList<ArrayList<Double>> points = new ArrayList<>();

    private final static int MAX_SAMPLES = 20;
    private ArrayList<Double> rssi_samples = new ArrayList<>();
    int step = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "APCalibrateActivity: onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apcalibrate);
        _datapointsText = (TextView) findViewById(R.id.tv_datapoints);
        _msgText = (TextView) findViewById(R.id.tv_msg);
        _scanButton = (Button) findViewById(R.id.btn_scan);
        ap = getIntent().getParcelableExtra("ap");

        _msgText.setText("Move device " + distances[step] + "m from the AP and click SCAN.");
        _scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _scanButton.setEnabled(false);
                progressDialog = new ProgressDialog(APCalibrateActivity.this, R.style.AppTheme);
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage("Calibrating....");
                progressDialog.show();
                scanRequested = true;

                rssi_samples.clear();
                wifi.startScan();
            }
        });

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false) {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }

        wifiReceiver = new APCalibrateActivity.WifiScanReceiver();
        registerReceiver(wifiReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void fitDistanceCurve() {
        Log.v(TAG, "APCalibrateActivity: fitDistanceCurve");
        final WeightedObservedPoints obs = new WeightedObservedPoints();
        Log.v(TAG, "points: " + points);
        for(int i = 0; i < distances.length; i++) {
            ArrayList<Double> point = points.get(i);
            Log.v(TAG, String.format("X = %f, Y = %f", point.get(0), point.get(1)));
            obs.add(point.get(0), point.get(1));
        }
        final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        final double[] coeffs = fitter.fit(obs.toList());
        ap.n = coeffs[1];
        ap.A = - coeffs[0];
        Log.v(TAG, String.format("rssi= %fx - %f", ap.n, ap.A));
        Intent _result = new Intent();
        _result.putExtra("ap", ap);
        setResult(RESULT_OK, _result);
        finish();
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "MainActivity: Wifi Scan Results received");
            if(scanRequested) {
                ArrayList<ScanResult> results = (ArrayList) wifi.getScanResults();
                boolean found = false;
                for(ScanResult result : results) {
                    if(result.BSSID.equals(ap.bssid)) {
                        rssi_samples.add((double) result.level);
                        found = true;
                    }
                }
                if(!found) {
                    _scanButton.setEnabled(true);
                    progressDialog.dismiss();
                    scanRequested = false;
                    Toast.makeText(getApplicationContext(), "Access Point not found", Toast.LENGTH_SHORT).show();
                }
                if(rssi_samples.size() <= MAX_SAMPLES) {
                    Log.v(TAG, String.format("samples taken = %d", rssi_samples.size()));
                    wifi.startScan();
                } else {
                    double rssi = 0;
                    for(double sample: rssi_samples) {
                        rssi += sample;
                    }
                    ArrayList<Double> point = new ArrayList<>();
                    point.add(-10.0 * log10(distances[step]));
                    point.add(rssi / rssi_samples.size());
                    points.add(point);
                    _datapointsText.setText(points.toString());
                    _scanButton.setEnabled(true);
                    progressDialog.dismiss();
                    scanRequested = false;
                    if(step < 3) {
                        step++;
                        _msgText.setText("Move device " + distances[step] + "m from the AP and click SCAN.");
                    } else {
                        _msgText.setText("Click Finish");
                        _scanButton.setText("FINISH");
                        fitDistanceCurve();
                    }
                }
            }

        }
    }
}
