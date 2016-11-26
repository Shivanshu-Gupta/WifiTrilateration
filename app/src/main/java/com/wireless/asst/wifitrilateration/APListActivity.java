package com.wireless.asst.wifitrilateration;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import static android.R.layout.simple_list_item_1;

public class APListActivity extends AppCompatActivity {
    public final static String TAG = "WifiTrilateration";
    public final static String ACTION_ADD_AP = "com.wireless.asst.wifitrilateration.ADD_AP";
    public final static String ACTION_EDIT_AP = "com.wireless.asst.wifitrilateration.EDIT_AP";
    SharedPreferences prefs;
    ArrayList<AccessPoint> aplist;
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
        aplist = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, simple_list_item_1, aplist);
        _aplistView.setAdapter(adapter);
        registerForContextMenu(_aplistView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        Log.v(TAG, "APListActivity: onCreateContextMenu");
        if (v.getId()==R.id.view_aplist) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            Log.d(TAG, "AP selected: "
                    + info.position + ". "
                    + aplist.get(info.position).toString());
            AccessPoint ap = aplist.get(info.position);
            String title = ap.ssid + "[" + ap.bssid + "]";
            menu.setHeaderTitle(title);
            String[] menuItems = getResources().getStringArray(R.array.menu);
            for (int i = 0; i<menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.v(TAG, "APListActivity: onContextItemSelected");
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String[] menuItems = getResources().getStringArray(R.array.menu);
        String menuItemName = menuItems[menuItemIndex];
        if(menuItemName.equals("Edit")) {
            Intent intent = new Intent();
            intent.setAction(ACTION_EDIT_AP);
            intent.putExtra("apindex", info.position);
            startActivity(intent);
        } else if(menuItemName.equals("Delete")) {
            aplist.remove(info.position);
            adapter.notifyDataSetChanged();
        }
        return true;
    }

    public void addAP(View view) {
        Intent intent = new Intent();
        intent.setAction(ACTION_ADD_AP);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "APListActivity: onResume");
        super.onResume();
        String aplist_str = prefs.getString("aplist", "");
        Gson gson = new Gson();
        aplist.clear();
        if(!aplist_str.equals("")) {
            Type responseType= new TypeToken<ArrayList<AccessPoint>>() { }.getType();
            aplist.addAll((ArrayList<AccessPoint>)gson.fromJson(aplist_str, responseType));
        }
        Log.d(TAG, aplist.toString());
        adapter.notifyDataSetChanged();
    }
}
