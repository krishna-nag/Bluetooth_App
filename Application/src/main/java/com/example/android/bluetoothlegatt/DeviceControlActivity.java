/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mDataField2;

    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private central mCentralService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private boolean service_Connected = false;

    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            central.LocalBinder binder = (central.LocalBinder) service;
            mCentralService = binder.getService();
            //mBound = true;
            //mCentralService = ((central.LocalBinder) service).getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mCentralService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (central.ACTION_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (central.ACTION_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                //clearUI();
            } else if (central.ACTION_DATA_AVAILABLE.equals(action)) {
                String s= intent.getStringExtra("extra");
                //String s=intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                displayData(s);
//                Intent intent2 = new Intent(context,PILRupload.class);
//                // add infos for the service which file to download and where to store
//                intent2.putExtra("data", s);
//
//                startService(intent2);
            }
        }
    };


    public void buttonclicked(View view){
        Button b = (Button)view;
        String buttonText = b.getText().toString();
        if(buttonText.equalsIgnoreCase("Stop")){
            unbindService(mServiceConnection);
            Intent intent = new Intent(this, central.class);
            stopService(intent);
            service_Connected=false;
            b.setText("Start");
            mDataField2.setText("");
        }
        else if(buttonText.equalsIgnoreCase("start")){
            Intent gattServiceIntent = new Intent(this, central.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            gattServiceIntent.putExtra("address",mDeviceAddress);
            service_Connected = true;


            startService(gattServiceIntent);
            b.setText("Stop");

        }
    }
    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);
        if(central.isServiceRunning){
            mConnectionState = (TextView) findViewById(R.id.connection_state);
            //mDataField = (TextView) findViewById(R.id.data_value);
            mDataField2 = (TextView) findViewById(R.id.textView2);
            mDataField2.setMovementMethod(new ScrollingMovementMethod());
            getActionBar().setTitle(mDeviceName);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            Intent gattServiceIntent = new Intent(this, central.class);
            gattServiceIntent.putExtra("address",mDeviceAddress);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            service_Connected=true;
            Button mButton=(Button)findViewById(R.id.button2);
            mButton.setText("Stop");
            //Button b = (Button)view;

        }
        else{
            final Intent intent = getIntent();
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

            // Sets up UI references.
            //((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
            //mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
            //mGattServicesLis/t.setOnChildClickListener(servicesListClickListner);
            mConnectionState = (TextView) findViewById(R.id.connection_state);
            //mDataField = (TextView) findViewById(R.id.data_value);
            mDataField2 = (TextView) findViewById(R.id.textView2);
            mDataField2.setMovementMethod(new ScrollingMovementMethod());


            getActionBar().setTitle(mDeviceName);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            Intent gattServiceIntent = new Intent(this, central.class);
            gattServiceIntent.putExtra("address",mDeviceAddress);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            startService(gattServiceIntent);
            service_Connected=true;
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(Receiver, centralIntentFilter());
        if (!service_Connected) {
            Intent gattServiceIntent = new Intent(this, central.class);
            //gattServiceIntent.putExtra("address",mDeviceAddress);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(Receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(service_Connected){
            unbindService(mServiceConnection);
            service_Connected=false;
        }
        mCentralService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mCentralService.mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mCentralService.mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if(mDataField2!=null){
            String prev=mDataField2.getText().toString();

            if (data != null && !data.equalsIgnoreCase("null\n") ) {
                //mDataField.setText(data);
                prev=prev+data;
                mDataField2.setText(prev);
            }
        }
        else{
            Log.d("CONTROL","NULL REFERENCE");
        }

    }
    private static IntentFilter centralIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(central.ACTION_CONNECTED);
        intentFilter.addAction(central.ACTION_DISCONNECTED);
        intentFilter.addAction(central.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
