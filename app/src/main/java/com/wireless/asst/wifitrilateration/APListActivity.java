package com.wireless.asst.wifitrilateration;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.gson.Gson;

import java.util.ArrayList;

import static android.R.layout.simple_expandable_list_item_1;
import static android.R.layout.simple_list_item_1;

public class APListActivity extends AppCompatActivity {
    public final static String TAG = "WifiTrilateration";

    SharedPreferences prefs;
    ArrayList aplist;
    ListView _aplistView;
    ArrayAdapter<AccessPoint> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "APListActivity: onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aplist);
        _aplistView = (ListView) findViewById(R.id.view_aplist);

        prefs = PreferenceManager
                .getDefaultSharedPreferences(APListActivity.this);
        String aplistString = prefs.getString("aplist", "");

        Gson gson = new Gson();
        if(aplistString != "")
            aplist = gson.fromJson(aplistString, ArrayList.class);
        else
            aplist = new ArrayList<>();
        Log.d(TAG, aplist.toString());
        adapter = new ArrayAdapter<>(this, simple_list_item_1, aplist);
        _aplistView.setAdapter(adapter);
    }

    public void addAP(View view) {
        Intent intent = new Intent(this, AddAPActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "APListActivity: onCreate");
        super.onResume();
        String aplistString = prefs.getString("aplist", "");
        Gson gson = new Gson();
        aplist.clear();
        if(aplistString != "") {
            aplist.addAll(gson.fromJson(aplistString, ArrayList.class));
        }
        Log.d(TAG, aplist.toString());
        adapter.notifyDataSetChanged();
    }
}
