package com.wireless.asst.wifitrilateration;

import android.annotation.SuppressLint;
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
    EditText _ssidText, _bssidText;
    EditText _xText, _yText, _zText;
    Button _configureButton;
    int ap_index = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "APConfigureActivity: onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_ap);

        _ssidText = (EditText) findViewById(R.id.input_ssid);
        _bssidText = (EditText) findViewById(R.id.input_bssid);
        _xText = (EditText) findViewById(R.id.input_X);
        _yText = (EditText) findViewById(R.id.input_Y);
        _zText = (EditText)findViewById(R.id.input_Z);
        registerBSSIDChangedCallback();
        if (getIntent().getAction().equals(APListActivity.ACTION_EDIT_AP)) {
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
                    _ssidText.setText(ap.ssid);
                    _bssidText.setText(ap.bssid);
                    _xText.setText(String.format("%f", ap.coords.get(0)));
                    _yText.setText(String.format("%f", ap.coords.get(1)));
                    _zText.setText(String.format("%f", ap.coords.get(2)));
                }
            }
        }
        _configureButton = (Button) findViewById(R.id.btn_configure);
        _configureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "APConfigureActivity: Add new AP Button Clicked");
                String ssid = _ssidText.getText().toString();
                String bssid = _bssidText.getText().toString();
                ArrayList<Double> coords = new ArrayList<>();
                coords.add(Double.parseDouble(_xText.getText().toString()));
                coords.add(Double.parseDouble(_yText.getText().toString()));
                coords.add(Double.parseDouble(_zText.getText().toString()));
                AccessPoint newAP = new AccessPoint(ssid, bssid, coords);

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
        });
    }


    /**
     * Registers TextWatcher for MAC EditText field. Automatically adds colons,
     * switches the MAC to upper case and handles the cursor position.
     */
    private void registerBSSIDChangedCallback() {
        _bssidText.addTextChangedListener(new TextWatcher() {
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
                String enteredMac = _bssidText.getText().toString().toLowerCase();
                String cleanMac = clearNonMacCharacters(enteredMac);
                String formattedMac = formatMacAddress(cleanMac);

                int selectionStart = _bssidText.getSelectionStart();
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
                _bssidText.removeTextChangedListener(this);
                if (cleanMac.length() <= 12) {
                    _bssidText.setText(formattedMac);
                    _bssidText.setSelection(selectionStart + lengthDiff);
                    mPreviousMac = formattedMac;
                } else {
                    _bssidText.setText(mPreviousMac);
                    _bssidText.setSelection(mPreviousMac.length());
                }
                _bssidText.addTextChangedListener(this);
            }
        });
    }
}
