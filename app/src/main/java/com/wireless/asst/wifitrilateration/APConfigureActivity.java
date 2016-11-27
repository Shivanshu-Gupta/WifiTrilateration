package com.wireless.asst.wifitrilateration;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class APConfigureActivity extends AppCompatActivity {
    public final static String TAG = "WifiTrilateration";
    public final static String ACTION_CALIBRATE_AP = "com.wireless.asst.wifitrilateration.CALIBRATE_AP";
    EditText _ssidEdit, _bssidEdit;
    EditText _xEdit, _yEdit, _zEdit;
    Button _configureButton;
    int ap_index = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "APConfigureActivity: onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_ap);

        _ssidEdit = (EditText) findViewById(R.id.input_ssid);
        _bssidEdit = (EditText) findViewById(R.id.input_bssid);
        _xEdit = (EditText) findViewById(R.id.input_X);
        _yEdit = (EditText) findViewById(R.id.input_Y);
        _zEdit = (EditText)findViewById(R.id.input_Z);
        registerBSSIDChangedCallback();
        if (getIntent().getAction().equals(APListActivity.ACTION_CONFIGURE_AP)) {
            Log.v(TAG, "Editing AP");
            ap_index = getIntent().getIntExtra("apindex", -1);
            Log.d(TAG, "AP index: " + ap_index);
            if(ap_index >= 0) {
                String aplist_str = PreferenceManager
                        .getDefaultSharedPreferences(APConfigureActivity.this)
                        .getString("aplist", "");
                Gson gson = new Gson();
                if(!aplist_str.equals("")) {
                    Type responseType = new TypeToken<ArrayList<AccessPoint>>() {}.getType();
                    ArrayList<AccessPoint> aplist = gson.fromJson(aplist_str, responseType);
                    AccessPoint ap = aplist.get(ap_index);
                    Log.d(TAG, "AP: " + ap.toString());
                    _ssidEdit.setText(ap.ssid);
                    _bssidEdit.setText(ap.bssid);
                    _xEdit.setText(String.format("%f", ap.coords[0]));
                    _yEdit.setText(String.format("%f", ap.coords[1]));
                    _zEdit.setText(String.format("%f", ap.coords[2]));
                }
            }
        }
        _configureButton = (Button) findViewById(R.id.btn_calibrate);
        _configureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "APConfigureActivity: Add new AP Button Clicked");
                String ssid = _ssidEdit.getText().toString();
                String bssid = _bssidEdit.getText().toString();
                double[] coords = new double[3];
                coords[0] = Double.parseDouble(_xEdit.getText().toString());
                coords[1] = Double.parseDouble(_yEdit.getText().toString());
                coords[2] = Double.parseDouble(_zEdit.getText().toString());
                AccessPoint newAP = new AccessPoint(ssid, bssid, coords);
                Log.v(TAG, "new AP: " + newAP.toString());

                Intent intent = new Intent(v.getContext(), APCalibrateActivity.class);
                intent.putExtra("ap", newAP);
                startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0) {
            if(resultCode == RESULT_OK) {
                AccessPoint newAP = data.getParcelableExtra("ap");

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(APConfigureActivity.this);
                String aplist_str = prefs.getString("aplist", "");

                Gson gson = new Gson();
                ArrayList<AccessPoint> aplist;
                if(!aplist_str.equals("")) {
                    Type responseType= new TypeToken<ArrayList<AccessPoint>>() { }.getType();
                    aplist = gson.fromJson(aplist_str, responseType);
                    if(ap_index >= 0) {
                        aplist.remove(ap_index);
                    }
                } else {
                    aplist = new ArrayList<>();
                }
                aplist.add(newAP);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("aplist", gson.toJson(aplist));
                editor.commit();

                finish();
            }
        }
    }

    /**
     * Registers TextWatcher for MAC EditText field. Automatically adds colons,
     * switches the MAC to upper case and handles the cursor position.
     */
    private void registerBSSIDChangedCallback() {
        _bssidEdit.addTextChangedListener(new TextWatcher() {
            String mPreviousMac = null;

            /* (non-Javadoc)
             * Does nothing.
             * @see android.text.TextWatcher#afterTextChanged(android.text.Editable)
             */
            @Override
            public void afterTextChanged(Editable arg0) {
            }

            /* (non-Javadoc)
             * Does nothing.
             * @see android.text.TextWatcher#beforeTextChanged(java.lang.CharSequence, int, int, int)
             */
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            /* (non-Javadoc)
             * Formats the MAC address and handles the cursor position.
             * @see android.text.TextWatcher#onTextChanged(java.lang.CharSequence, int, int, int)
             */
            @SuppressLint("DefaultLocale")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String enteredMac = _bssidEdit.getText().toString().toLowerCase();
                String cleanMac = clearNonMacCharacters(enteredMac);
                String formattedMac = formatMacAddress(cleanMac);

                int selectionStart = _bssidEdit.getSelectionStart();
                formattedMac = handleColonDeletion(enteredMac, formattedMac, selectionStart);
                int lengthDiff = formattedMac.length() - enteredMac.length();

                setMacEdit(cleanMac, formattedMac, selectionStart, lengthDiff);
            }

            /**
             * Strips all characters from a string except A-F and 0-9.
             * @param mac       User input string.
             * @return          String containing MAC-allowed characters.
             */
            private String clearNonMacCharacters(String mac) {
                return mac.toString().replaceAll("[^A-Fa-f0-9]", "");
            }

            /**
             * Adds a colon character to an unformatted MAC address after
             * every second character (strips full MAC trailing colon)
             * @param cleanMac      Unformatted MAC address.
             * @return              Properly formatted MAC address.
             */
            private String formatMacAddress(String cleanMac) {
                int grouppedCharacters = 0;
                String formattedMac = "";

                for (int i = 0; i < cleanMac.length(); ++i) {
                    formattedMac += cleanMac.charAt(i);
                    ++grouppedCharacters;

                    if (grouppedCharacters == 2) {
                        formattedMac += ":";
                        grouppedCharacters = 0;
                    }
                }

                // Removes trailing colon for complete MAC address
                if (cleanMac.length() == 12)
                    formattedMac = formattedMac.substring(0, formattedMac.length() - 1);

                return formattedMac;
            }

            /**
             * Upon users colon deletion, deletes MAC character preceding deleted colon as well.
             * @param enteredMac            User input MAC.
             * @param formattedMac          Formatted MAC address.
             * @param selectionStart        MAC EditText field cursor position.
             * @return                      Formatted MAC address.
             */
            private String handleColonDeletion(String enteredMac, String formattedMac, int selectionStart) {
                if (mPreviousMac != null && mPreviousMac.length() > 1) {
                    int previousColonCount = colonCount(mPreviousMac);
                    int currentColonCount = colonCount(enteredMac);

                    if (currentColonCount < previousColonCount) {
                        formattedMac = formattedMac.substring(0, selectionStart - 1) + formattedMac.substring(selectionStart);
                        String cleanMac = clearNonMacCharacters(formattedMac);
                        formattedMac = formatMacAddress(cleanMac);
                    }
                }
                return formattedMac;
            }

            /**
             * Gets MAC address current colon count.
             * @param formattedMac      Formatted MAC address.
             * @return                  Current number of colons in MAC address.
             */
            private int colonCount(String formattedMac) {
                return formattedMac.replaceAll("[^:]", "").length();
            }

            /**
             * Removes TextChange listener, sets MAC EditText field value,
             * sets new cursor position and re-initiates the listener.
             * @param cleanMac          Clean MAC address.
             * @param formattedMac      Formatted MAC address.
             * @param selectionStart    MAC EditText field cursor position.
             * @param lengthDiff        Formatted/Entered MAC number of characters difference.
             */
            private void setMacEdit(String cleanMac, String formattedMac, int selectionStart, int lengthDiff) {
                _bssidEdit.removeTextChangedListener(this);
                if (cleanMac.length() <= 12) {
                    _bssidEdit.setText(formattedMac);
                    _bssidEdit.setSelection(selectionStart + lengthDiff);
                    mPreviousMac = formattedMac;
                } else {
                    _bssidEdit.setText(mPreviousMac);
                    _bssidEdit.setSelection(mPreviousMac.length());
                }
                _bssidEdit.addTextChangedListener(this);
            }
        });
    }
}
