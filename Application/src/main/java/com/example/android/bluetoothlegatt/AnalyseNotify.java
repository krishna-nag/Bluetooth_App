package com.example.android.bluetoothlegatt;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
//import android.util.Rational;

//import java.lang.Object;
//import javax.measure.Measurable;
//import org.jscience.mathematics.*;
import org.jscience.mathematics.number.Float64;
import org.jscience.mathematics.vector.Float64Matrix;
import org.jscience.mathematics.vector.Float64Vector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import static java.lang.Math.exp;


//import static org.jscience.physics.units.SI.*;
//import org.jscience.mathematics.vector.Matrix;
/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class AnalyseNotify extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "com.example.android.bluetoothlegatt.action.FOO";
    private static final String ACTION_BAZ = "com.example.android.bluetoothlegatt.action.BAZ";
    protected static String current_image_name="test";
    protected static int latest_image_packet=0;
    protected static int latest_actual_image_packet=0;

    protected static int no_of_packet=0;
    protected static int no_of_60_pack=0;



    protected File file;
    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.example.android.bluetoothlegatt.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.example.android.bluetoothlegatt.extra.PARAM2";
    private int k[] = {1, 2};
    private double prior[] = {0.036654, 0.963346};
    private int cost[][] = {{0, 1}, {1, 0}};
    private Float64Vector sig0 = Float64Vector.valueOf(30.38267, 154.596, 0.051324, 43.71637);
    private Float64Vector sig1 = Float64Vector.valueOf(154.596, 1211.444, -40.9401, 240.7672);
    private Float64Vector sig2 = Float64Vector.valueOf(0.051324, -40.9401, 424.5009, 12.96477);
    private Float64Vector sig3 = Float64Vector.valueOf(43.71637, 240.7672, 12.96477, 97.58213);
    private Float64Matrix sigma = Float64Matrix.valueOf(sig0, sig1, sig2, sig3).transpose();
    private Float64Vector mean0 = Float64Vector.valueOf(13.42628, 47.84525, 82.75646, 28.83159);
    private Float64Vector mean1 = Float64Vector.valueOf(4.319015, 32.47262, 56.38581, 9.433946);
    private Float64Matrix mu = Float64Matrix.valueOf(mean0, mean1).transpose();
    //private Float64Matrix mu2=Float64Matrix.valueOf(mean1);
    private NotificationHandler nHandler;


    public AnalyseNotify() {
        super("AnalyseNotify");
    }

    double prior_probability(Float64Matrix x, int k) {
        double d = sigma.determinant().doubleValue();
        double coeff = 1 / (2 * Math.PI * d);
        double power = -0.5;
        k=k-1;
        Float64Matrix term1 = x.minus((Float64Matrix.valueOf(mu.getColumn(k))).transpose()).transpose();
        Float64Matrix term2 = sigma.inverse();
        Float64Matrix term3 = x.minus(Float64Matrix.valueOf(mu.getColumn(k)).transpose());
        Float64Matrix result = (term1.times(term2)).times(term3);
        Float64Vector scalar = Float64Vector.valueOf(-0.5);
        double res=result.get(0,0).doubleValue()*(-0.5);
        //result = result.times(Float64.valueOf(power));
        double answer = exp(res);
        Log.d("res",result.toString());

        return answer;

    }

    double posteriour_probability(int k, Float64Matrix x) {
        double pk = prior[k - 1];
        double prior_probability1 = prior_probability(x, 1);
        double prior_probability2 = prior_probability(x, 2);
        double numerator;
        if (k == 1) {
            numerator = pk * prior_probability1;
        } else {
            numerator = pk * prior_probability2;
        }
        double denominator = (prior_probability1 * prior[0]) + prior_probability2 * prior[1];
        return numerator / denominator;

    }

    protected int getclass(Float64Matrix x) {
        double c1=posteriour_probability(1,x)*cost[0][0] + posteriour_probability(2,x)*cost[0][1];
        double c2=posteriour_probability(1,x)*cost[1][0]+ posteriour_probability(2,x)*cost[1][1];
        Log.d("POSTERIORc1",Double.toString(c1));
        Log.d("POSTERIORc2",Double.toString(c2));

        if(c1 <= c2){
            return 1;
        }
        else{
            return 2;
        }
    }

    protected void manage_data2(String data,byte[] bytes){

        Log.d("BYTELENGTH",""+bytes.length);
        String data_original[]= data.split(" ");
        Log.d("STRINGLENGTH",""+data_original.length);

        int max_header_bytes=8;
        char header[]=new char[max_header_bytes];
        for(int i=0;i<max_header_bytes;i++){
            try{
                header[i]=(char)(int)Integer.valueOf(data_original[i]);
            }
            catch(Exception e){
                //simply put something so that it matches no header
                header[i]='o';
            }
        }
        //check header
        int x=0;
        int start;
        if(header[x]=='+'){
            x=x+1;
        }
        start=x;
        //IF image
        if(header[x]=='I' && header[x+1]=='M' && header[x+2]=='G'){
            no_of_packet++;
            Log.d("DATATYPE","IMG RECEIVED");
            SQLiteDatabase db = central.mDbHelper.getWritableDatabase();

            byte[] data_bytes=new byte[bytes.length - x -5 -1];
            String ssssss[]=new String[bytes.length - x -5 -1];

            // to print in the debug section of the app
            String data_string=new String(bytes);
            final Intent intent3 = new Intent(central.ACTION_DATA_AVAILABLE);
            intent3.putExtra("extra", data+"\n" );
            sendBroadcast(intent3);

            //check whether START of image
            if(header[x+4]=='S'){  //was x+4 before, for some reason
                current_image_name=Long.toString(System.currentTimeMillis() / 1000L)+".jpg";
                latest_image_packet=0;
                no_of_packet=1;
                latest_actual_image_packet=0;
                ContentValues values = new ContentValues();
                values.put(FeedReaderDbHelper.COLUMN_NAME_TITLE, "image0");
                values.put(FeedReaderDbHelper.COLUMN_NAME_SUBTITLE, data_bytes);

                // Insert the new row, returning the primary key value of the new row
                long newRowId = db.insert(FeedReaderDbHelper.TABLE_NAME, null, values);
//

            }
            else if(header[x+4]=='E'){
                no_of_packet=0;
                no_of_60_pack=0;
                ContentValues values = new ContentValues();
                values.put(FeedReaderDbHelper.COLUMN_NAME_TITLE, "image"+(++latest_actual_image_packet));
                values.put(FeedReaderDbHelper.COLUMN_NAME_SUBTITLE, data_bytes);

                // Insert the new row, returning the primary key value of the new row
                long newRowId = db.insert(FeedReaderDbHelper.TABLE_NAME, null, values);

                SQLiteDatabase db2 = central.mDbHelper.getReadableDatabase();
                String[] projection = {
                        FeedReaderDbHelper.id,
                        FeedReaderDbHelper.COLUMN_NAME_TITLE,
                        FeedReaderDbHelper.COLUMN_NAME_SUBTITLE
                };


                file = new File(Environment.getExternalStorageDirectory() + File.separator + "test.jpg");
                Log.d("FILEPATH",Environment.getExternalStorageDirectory() + File.separator + "test.jpg");
                try{
                    file.createNewFile();

                }
                catch(Exception e){
                    Log.d("FILE",e.toString());
                }


                for(int i=0;i<latest_actual_image_packet;i++){
                    // Filter results WHERE "title" = 'My Title'
                    String selection = FeedReaderDbHelper.COLUMN_NAME_TITLE + " = ?";

                    String[] selectionArgs = { "image"+i };

                    // How you want the results sorted in the resulting Cursor
                    String sortOrder =
                            FeedReaderDbHelper.COLUMN_NAME_SUBTITLE + " DESC";

                    Cursor cursor = db.query(
                            FeedReaderDbHelper.TABLE_NAME,   // The table to query
                            projection,             // The array of columns to return (pass null to get all)
                            selection,              // The columns for the WHERE clause
                            selectionArgs,          // The values for the WHERE clause
                            null,                   // don't group the rows
                            null,                   // don't filter by row groups
                            sortOrder               // The sort order
                    );

                    while(cursor.moveToNext()) {
                        byte bytesimage[] = cursor.getBlob(
                                cursor.getColumnIndexOrThrow(FeedReaderDbHelper.COLUMN_NAME_SUBTITLE));
                        System.out.println(bytesimage);
                        if(file.exists() && i==0)
                        {
                            try{
                                OutputStream fo = new FileOutputStream(file,false);
                                Log.d("WRITE","image"+i+"  length-"+bytesimage.length);
                                fo.write(bytesimage);
                                fo.close();
                            }
                            catch(Exception e){
                                Log.d("FILE",e.toString());
                            }


                        }
                        else if(file.exists() && i>0){
                            try{
                                OutputStream fo = new FileOutputStream(file,true);
                                Log.d("WRITE","image"+i+"  length-"+bytesimage.length);

                                fo.write(bytesimage);
                                fo.close();
                            }
                            catch(Exception e){
                                Log.d("FILE",e.toString());
                            }
                        }
                        break;
                    }
                }



            }
            else{
                int packet_no=0x0000FF & (int)bytes[x+4];
                Log.d("PACKET1",""+packet_no);
                //no_of_packet++;
                if(packet_no == 255){
                    packet_no=13;
                }

                if(packet_no>latest_image_packet){
                    latest_image_packet=packet_no;

                }
                int actual_packet_no=no_of_60_pack*59+packet_no;
                if(actual_packet_no> latest_actual_image_packet){
                    latest_actual_image_packet=actual_packet_no;

                }
                if(latest_image_packet==59 && no_of_packet%60==0 && no_of_packet !=0){
                    latest_image_packet=-1;
                    no_of_60_pack++;
                    Log.d("60pack",""+no_of_60_pack);


                }
                Log.d("PACKET2",""+actual_packet_no);
                Log.d("NOPACKET",""+no_of_packet);


                ContentValues values = new ContentValues();
                values.put(FeedReaderDbHelper.COLUMN_NAME_TITLE, "image"+actual_packet_no);
                values.put(FeedReaderDbHelper.COLUMN_NAME_SUBTITLE, data_bytes);
                //Log.d("storedd",String.join("", ssssss));
                Log.d("stored",""+data_bytes.length);


                // Insert the new row, returning the primary key value of the new row
                long newRowId = db.insert(FeedReaderDbHelper.TABLE_NAME, null, values);
            }
        }



        //IF accelerometer data
        else if(header[x]=='A'){
        // upload to PILR
            Intent intent2 = new Intent(this,PILRupload.class);
            // add infos for the service which file to download and where to store
            intent2.putExtra("data", data);
            intent2.putExtra("type",1);
            startService(intent2);

        }
        //IF any other raw data
        else{
            String data_string=new String(bytes);
            final Intent intent3 = new Intent(central.ACTION_DATA_AVAILABLE);
            intent3.putExtra("extra", data_string+"\n" );
            sendBroadcast(intent3);
        }
    }


    protected void manage_data(String data,byte[] bytes){

        Log.d("BYTELENGTH",""+bytes.length);
        String data_original[]= data.split(" ");
        Log.d("STRINGLENGTH",""+data_original.length);

        int max_header_bytes=8;
        char header[]=new char[max_header_bytes];
        for(int i=0;i<max_header_bytes;i++){
            try{
                header[i]=(char)(int)Integer.valueOf(data_original[i]);
            }
            catch(Exception e){
                //simply put something so that it matches no header
                header[i]='o';
            }
        }
        //check header
        int x=0;
        int start;
        if(header[x]=='+'){
            x=x+1;
        }
        start=x;
        //IF image
        if(header[x]=='I' && header[x+1]=='M' && header[x+2]=='G'){
            no_of_packet++;
            Log.d("DATATYPE","IMG RECEIVED");
            SQLiteDatabase db = central.mDbHelper.getWritableDatabase();

            byte[] data_bytes=new byte[bytes.length - x -5 -1];
            for(int t=0;t<data_bytes.length;t++){
                data_bytes[t]=bytes[t+x+5];
            }

            //For printing on the debug screen in the app
            String data_string=new String(bytes);
            final Intent intent3 = new Intent(central.ACTION_DATA_AVAILABLE);
            intent3.putExtra("extra", data+"\n" );
            sendBroadcast(intent3);

            //check whether START of image
            if(header[x+4]=='S'){
                current_image_name=Long.toString(System.currentTimeMillis() / 1000L)+".jpg";
                latest_image_packet=0;
                no_of_packet=1;
                latest_actual_image_packet=0;
                this.file = new File(Environment.getExternalStorageDirectory() + File.separator + "test.jpg");
                Log.d("FILEPATH",Environment.getExternalStorageDirectory() + File.separator + "test.jpg");
                try{
                    this.file.createNewFile();

                }
                catch(Exception e){
                    Log.d("FILE",e.toString());
                }
                try{
                    OutputStream fo = new FileOutputStream(file,false);
                    Log.d("WRITE","image"+0+"  length-"+data_bytes.length);
                    fo.write(data_bytes);
                    fo.close();
                }
                catch(Exception e){
                    Log.d("FILE",e.toString());
                }
                // Insert the new row, returning the primary key value of the new row
                //long newRowId = db.insert(FeedReaderDbHelper.TABLE_NAME, null, values);
//

            }
            else if(header[x+4]=='E'){
                no_of_packet=0;
                no_of_60_pack=0;
                ContentValues values = new ContentValues();
                values.put(FeedReaderDbHelper.COLUMN_NAME_TITLE, "image"+(++latest_actual_image_packet));
                values.put(FeedReaderDbHelper.COLUMN_NAME_SUBTITLE, data_bytes);
                this.file = new File(Environment.getExternalStorageDirectory() + File.separator + "test.jpg");
                Log.d("FILEPATH",Environment.getExternalStorageDirectory() + File.separator + "test.jpg");
                try{
                    this.file.createNewFile();

                }
                catch(Exception e){
                    Log.d("FILE",e.toString());
                }
                try{
                    OutputStream fo = new FileOutputStream(this.file,true);
                    Log.d("WRITE","image"+0+"  length-"+data_bytes.length);
                    fo.write(data_bytes);
                    fo.close();
                }
                catch(Exception e){
                    Log.d("FILE",e.toString());
                }
            }
            else{
                int packet_no=0x0000FF & (int)bytes[x+4];
                this.file = new File(Environment.getExternalStorageDirectory() + File.separator + "test.jpg");
                Log.d("FILEPATH",Environment.getExternalStorageDirectory() + File.separator + "test.jpg");
                try{
                    this.file.createNewFile();

                }
                catch(Exception e){
                    Log.d("FILE",e.toString());
                }
                try{
                    OutputStream fo = new FileOutputStream(this.file,true);
                    Log.d("WRITE","image"+packet_no+"  length-"+data_bytes.length);
                    fo.write(data_bytes);
                    //fo.close();
                }
                catch(Exception e){
                    Log.d("FILE",e.toString());
                }
            }
        }



        //IF accelerometer data
        else if(header[x]=='A' && header[x+1]=='C' && header[x+2]=='C'){
            byte[] data_bytes=new byte[bytes.length - x -3 -1];
            for(int t=0;t<data_bytes.length;t++){
                data_bytes[t]=bytes[t+x+4];
            }

            String datas="";
            for(int t=0;t<data_bytes.length;t++){
                if(t==0){
                    datas=datas+data_original[t+x+4];
                }
                else{
                    datas=datas+" "+data_original[t+x+4];

                }
            }
            double feat1=data_bytes[0];
            double feat2=data_bytes[1];
            double feat3=data_bytes[2];
            double feat4=data_bytes[3];
            Float64Vector column0 = Float64Vector.valueOf(feat1,feat2,feat3,feat4);
            //Float64Vector<Rational> column1 = Float64Vector.valueOf(...);
            Float64Matrix temp;
            temp = Float64Matrix.valueOf(column0).transpose();
            int eclass;
            eclass=getclass(temp);
            nHandler = NotificationHandler.getInstance(this);

            if(eclass==1){
                nHandler.createSimpleNotification(this);

            }
Log.d("NOTIFY","reached here");


            // upload to PILR
            Intent intent2 = new Intent(this,PILRupload.class);
            // add infos for the service which file to download and where to store

            intent2.putExtra("data", datas);
            intent2.putExtra("type",1);
            startService(intent2);

            //Show on screen
            String data_string=new String(bytes);
            final Intent intent3 = new Intent(central.ACTION_DATA_AVAILABLE);
            intent3.putExtra("extra", datas+"\n" );
            sendBroadcast(intent3);


        }
        //IF any other raw data
        else{
            String data_string=new String(bytes);
            final Intent intent3 = new Intent(central.ACTION_DATA_AVAILABLE);
            intent3.putExtra("extra", data_string+"\n" );
            sendBroadcast(intent3);
        }
    }



    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            //final String action = intent.getAction();
            String dataString= intent.getStringExtra("data");
            byte bytes[]=intent.getByteArrayExtra("bytes");
//
            manage_data(dataString,bytes);
            //eating:  13	34.99596	81	30
            //not eating: 7	21.61256	32	11
//            double f1=7;
//            double f2=21.61256;
//            double f3=32;
//            double f4=11;
//            Float64Vector column0 = Float64Vector.valueOf(f1,f2,f3,f4);
//            //Float64Vector<Rational> column1 = Float64Vector.valueOf(...);
//            Float64Matrix x;
//            x = Float64Matrix.valueOf(column0).transpose();
//            int eclass;
//            eclass=getclass(x);
////            if(f1>30){
////                eclass=1;
////            }
////            else{
////                eclass=2;
////            }
//            Log.d("CLASSSSS",Integer.toString(eclass));
//            nHandler = NotificationHandler.getInstance(this);
//            nHandler.createSimpleNotification(this);
//
//            if(eclass==1){
//                nHandler.createSimpleNotification(this);
//
//            }




        }
    }


}
