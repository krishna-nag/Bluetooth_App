package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.support.v13.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity {

    private boolean valid_participant;
    private String pname;
    private String access_code;
    private String project;
    private String info_uri;
    private String participantid;
    private NotificationHandler nHandler;
    TextView t;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //testing ml code

//        byte bytes[]=new byte[1];
//        Intent intent4 = new Intent(this,AnalyseNotify.class);
//        // add infos for the service which file to download and where to store
//        intent4.putExtra("data", "nothing");
//        intent4.putExtra("bytes",bytes);
//
//        startService(intent4);


        // testing box

        //Intent intent4 = new Intent(this,Box.class);
        // add infos for the service which file to download and where to store
        //intent4.putExtra("data", s);
        //intent4.putExtra("bytes",bytes);

        //startService(intent4);

        //See if the data collection is already runnig. If so, go directly to that page.
        if(central.isServiceRunning){
            final Intent intent = new Intent(this, DeviceControlActivity.class);
//            intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
//            intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
//            if (mScanning) {
//                mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                mScanning = false;
//            }
            startActivity(intent);
        }

        Log.d("MAIN","started main");
        Log.d("FILE",Environment.getExternalStorageDirectory() + File.separator + "test.jpg");
        setContentView(R.layout.activity_main);
        TextView t=(TextView) findViewById(R.id.textView3);
        verifyStoragePermissions(this);
        // Check if there is an entry for this participant in the file. If there is, then retreive all info.
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        if(pref.contains("recentid")){
            SharedPreferences.Editor editor = pref.edit();
            String id=pref.getString("recentid", null);
            if(pref.contains("initialised"+id)){
                pname=pref.getString("name"+id,null);
                access_code=pref.getString("access_code"+id,null);
                project=pref.getString("project"+id,null);
                info_uri=pref.getString("info_uri"+id,null);
                participantid=pref.getString("participantid"+id,null);
                String prev=t.getText().toString();
                t.setText(prev+pname);

            }
        }
        else{
            // If the info is not there in file, prompt login
            Intent intent=new Intent(this,LoginActivity.class);
            startActivity(intent);
        }

    }
    public void buttonclicked(View view){
        //If logged in, go to scanning for BLE
        Intent intent = new Intent(this, DeviceScanActivity.class);

        startActivity(intent);
    }

}
