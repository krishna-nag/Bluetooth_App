package com.example.android.bluetoothlegatt;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class central extends Service {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static boolean isServiceRunning=false;
    public static FeedReaderDbHelper mDbHelper ;
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mDataField2;
    public final static String ACTION_DISCONNECTED =
            "com.example.android.bluetoothlegatt.ACTION_DISCONNECTED";
    public final static String ACTION_CONNECTED =
            "com.example.android.bluetoothlegatt.ACTION_CONNECTED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.android.bluetoothlegatt.ACTION_DATA_AVAILABLE";
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    protected BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("BLE", "Unable to initialize Bluetooth");
            }
            Log.d("CENTRAL","INITIALISED");
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    public central() {
    }

    //@Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
    public class LocalBinder extends Binder {
        central getService() {
            return central.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new central.LocalBinder();
    // Code to manage Service lifecycle.

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    public int onStartCommand(Intent intent, int flags,int startId){
        mDbHelper = new FeedReaderDbHelper(this);
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);

        SharedPreferences.Editor editor = pref.edit();
        if(intent !=null){
            String dataString= intent.getStringExtra("address");
            this.mDeviceAddress=dataString;


                String id=pref.getString("recentid", null);
                editor.putString("recentaddress",this.mDeviceAddress);
                editor.commit();


            central.isServiceRunning=true;
        }
        else{
            this.mDeviceAddress=pref.getString("recentaddress",null);
        }



        Log.d("CENTRAL","CENTRAL STARTED");
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        if(mBluetoothLeService != null){
            Log.d("IF","ENTERED IF");
            mBluetoothLeService.connect(mDeviceAddress);

        }


        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        this.mDeviceAddress=pref.getString("recentaddress",null);

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        // Start a foreground notification
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("My Awesome App")
                .setContentText("Doing some work...")
                .setContentIntent(pendingIntent).build();
        central.isServiceRunning=true;

        startForeground(1337, notification);
    }

    public void onDestroy(){
        mBluetoothLeService.disconnect();
        broadcastUpdate(ACTION_DISCONNECTED);
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        central.isServiceRunning=false;
        stopForeground(true);
        super.onDestroy();

    }
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                broadcastUpdate(ACTION_CONNECTED);

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //mBluetoothLeService.connect(mDeviceAddress);
                broadcastUpdate(ACTION_DISCONNECTED);

                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String s=intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                byte bytes[]=intent.getByteArrayExtra("bytes");
                if(s != null){

                    //sends  broadcast to device_control+activity to update the received text
//                    final Intent intent3 = new Intent(ACTION_DATA_AVAILABLE);
//                    intent3.putExtra("extra", s+"\n" );
//                    sendBroadcast(intent3);

                    // Start upload service to upload the received data
//                    Intent intent2 = new Intent(context,PILRupload.class);
//                    // add infos for the service which file to download and where to store
//                    intent2.putExtra("data", s);
//                    startService(intent2);


//                    //Start analyse Notify service to analyse the data, using the model, and notify about the eating and chew counts
                    Intent intent4 = new Intent(context,AnalyseNotify.class);
                    // add infos for the service which file to download and where to store
                    intent4.putExtra("data", s);
                    intent4.putExtra("bytes",bytes);

                    startService(intent4);
                }

            }
        }
    };
    //A better name for the function would be configure the app for the services
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();
            if(uuid.equalsIgnoreCase("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")){
// Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();
                    if(uuid.equalsIgnoreCase("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")){
                        mBluetoothLeService.maincharacteristic=gattCharacteristic;
                        mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                        BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(
                                UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBluetoothLeService.mBluetoothGatt.writeDescriptor(descriptor);
                    }
                    else if(uuid.equalsIgnoreCase("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")){
                        //mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
                        mBluetoothLeService.othercharacteristic=gattCharacteristic;

                    }
//                    else if(uuid.equalsIgnoreCase("914f8fb9-e8cd-411d-b7d1-14594de45425")){
//
//                    }

                }
            }

        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
