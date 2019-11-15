//package com.example.android.bluetoothlegatt;
//
//import android.app.Service;
//import android.content.Intent;
//import android.os.IBinder;
//
//public class PILRupload extends Service {
//    public PILRupload() {
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
//}

package com.example.android.bluetoothlegatt;

        import android.app.IntentService;
        import android.app.Service;
        import android.content.Context;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.os.AsyncTask;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.widget.TextView;

        import org.apache.http.HttpEntity;
        import org.apache.http.util.EntityUtils;
        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.io.UnsupportedEncodingException;
        import java.nio.charset.StandardCharsets;
        import java.text.SimpleDateFormat;
        import java.util.Date;
        import java.util.UUID;

        import org.apache.http.HttpResponse;
        import org.apache.http.client.ClientProtocolException;
        import org.apache.http.client.HttpClient;
        import org.apache.http.client.methods.HttpGet;
        import org.apache.http.client.methods.HttpPost;
        import org.apache.http.entity.StringEntity;
        import org.apache.http.impl.client.DefaultHttpClient;
        import org.apache.http.protocol.BasicHttpContext;
        import org.apache.http.protocol.HTTP;
        import org.apache.http.protocol.HttpContext;

        import static java.net.Proxy.Type.HTTP;
        import static org.apache.http.protocol.HTTP.UTF_8;

public class PILRupload extends IntentService {
    private String mAuthorization = "Authorization", mBearer = "Bearer";
    private boolean mHeader = false;
    private Context mContext;
    public static String message;
    //private List<Dataset> mStreams;
    static InputStream mIs = null;
    private String mKey, mUrl, mResult;
    public boolean uploadSuccess = false;
    JSONArray tempr;
    TextView textView;
    private String pname;
    private String access_code;
    private String project;
    private String info_uri;
    private String participantid;
    private String host_url;
    private String instrument;
    public PILRupload(){
        super("PILRupload");
    }
    public class background extends AsyncTask<String,String,String> {

        //public TextView textView;
        protected String doInBackground(String... params) {
            try
            {
                HttpClient httpclient = new DefaultHttpClient();
                Log.d("the url",params[0]);
                HttpPost post = new HttpPost(params[0]);
                post.setEntity(new StringEntity(tempr.toString(), UTF_8));
                //post.setHeader("Authorization", "Bearer" + " " + "1d4ddbce-c961-46ef-96cf-d77dae3a92df");
                post.setHeader("Authorization", "Bearer" + " " + access_code);
                Log.d("EXE","EXECUTING******************************************************************");
                //Log.d("EXE2",tempr.toString());
                Log.d("EXE2",access_code);

                HttpResponse response = httpclient.execute(post);
                HttpEntity entity = response.getEntity();
                String r=EntityUtils.toString(entity);
                Log.d("SERVER RESPONSE",r);
                if(entity != null){
                    return r;
                }
                else{
                    return "No string.";
                }

//            HttpClient mHttpClient = new DefaultHttpClient();
//            HttpContext mHttpContext = new BasicHttpContext();
//            HttpPost post = new HttpPost(url);
//            post.setEntity(new StringEntity(json.toString(), UTF_8));
//            post.setHeader(mAuthorization, mBearer + " " + "1d4ddbce-c961-46ef-96cf-d77dae3a92df");
//
//            HttpResponse response = mHttpClient.execute(post, mHttpContext);
//            mIs = response.getEntity().getContent();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(mIs, "UTF-8"));
//            StringBuilder sb = new StringBuilder();
//            String line = null;
//            while ((line = reader.readLine()) != null) {
//                sb.append(line + "\n");
//            }
//            mIs.close();
//            mResult = sb.toString();
//            Log.d("ApiManager", "Upload result:" + mResult);
//            if (mResult.length() == 0) {
//                mResult = null;
//            }
            }
            catch (UnsupportedEncodingException e) {
                Log.d("ApiMAnager", "UnsupportedEncodingException: " + e);
                e.printStackTrace();
                return "UnsupportedEncodingException: " + e;
            } catch (ClientProtocolException e) {
                Log.d("ApiMAnager", "ClientProtocolException: " + e);
                e.printStackTrace();
                return "ClientProtocolException: " + e;
            } catch (IOException e) {
                Log.d("ApiMAnager", "IOException: " + e);
                e.printStackTrace();
                return "IOException: " + e;
            }

        }

        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            // update textview here
            //textView.setText("Server message is "+result);
        }
    }
    protected String geturl(int type){
        if(type==0){
            String url="/api/v1/"+project+"/instrument/"+instrument+"/participant/"+participantid+"/dataset/pilrhealth:activity_monitoring_app:raw/1/data";
            return url;
        }
        else{
            String url="/api/v1/"+project+"/instrument/"+instrument+"/participant/"+participantid+"/dataset/pilrhealth:activity_monitoring_app:trial4/1/data";
            return url;

        }
        //"/api/v1/ema_sample_99909/instrument/ema_ots/participant/512/dataset/pilrhealth:activity_monitoring_app:trial2/1/data"
        //return url;
    }
    protected JSONArray make_json(int acc,String dataString) {

            try {
                JSONObject metadata = new JSONObject();
                metadata.put("pt", participantid);
                Long tsLong = System.currentTimeMillis(); ///1000
                Date mydate = new Date();
                mydate.setTime(tsLong);
                //String ts = tsLong.toString();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'TZD'");
                String ts = df.format(mydate);
                metadata.put("timestamp", ts);
                metadata.put("id", UUID.randomUUID().toString());
                if (dataString != null) {
                    if(acc>=1) { //this means incoming is accelerometer feature data
                        JSONObject data = new JSONObject();
                        //data.put("trial",dataString);
                        String array1[] = dataString.split(" ");

                        double f1 = Double.parseDouble(array1[0]);
                        double f2 = Double.parseDouble(array1[1]);
                        double f3 = Double.parseDouble(array1[2]);
                        double f4 = Double.parseDouble(array1[3]);

                        String h,min,s,day,month,y;
                        day=array1[4];
                        month=array1[5];
                        y=array1[6];
                        h=array1[7];
                        min=array1[8];
                        s=array1[9];

                        data.put("no_of_peaks_x", f1);
                        data.put("no_of_peaks_y", f4);
                        data.put("no_of_slopes", f3);
                        data.put("waveform", f2);
                        String timeStamp=day+"/"+month+"/"+y+" , "+h+":"+min+":"+s;
                        data.put("mctime",timeStamp);


                        JSONObject obj = new JSONObject();
                        obj.put("metadata", metadata);
                        obj.put("data", data);
                        JSONArray a = new JSONArray();
                        a.put(obj);
                        return a;
                    }
                    //this means raw data
                    else {
                        JSONObject data = new JSONObject();
                        //data.put("trial",dataString);

                        data.put("rawstring", dataString);

                        JSONObject obj = new JSONObject();
                        obj.put("metadata", metadata);
                        obj.put("data", data);
                        JSONArray a = new JSONArray();
                        a.put(obj);
                        return a;
                    }
                }

            }
            catch (JSONException ex) {
                Log.d("ApiMAnager", "JSONException Queue: " + ex);
                return null;
            }
        return null;
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        //String dataString = workIntent.getDataString();
        host_url=getString(R.string.host_URL);
        String dataString= workIntent.getStringExtra("data");
        int type=workIntent.getIntExtra("type",0);
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);

            SharedPreferences.Editor editor = pref.edit();
            String id=pref.getString("recentid", null);
            pname=pref.getString("name"+id,null);
            access_code=pref.getString("access_code"+id,null);
            project=pref.getString("project"+id,null);
            info_uri=pref.getString("info_uri"+id,null);
            participantid=pref.getString("participantid"+id,null);
            instrument=pref.getString("instrument"+id,null);


        //setContentView(R.layout.activity_display_message);
        // Get the Intent that started this activity and extract the string
//        Intent intent = getIntent();
//        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        //String message =dataString;
//        String message =this.message;
        Log.d("MSG",dataString+"##################################");
        try {
            JSONArray a;
            a=make_json(type,dataString);
            //tempr=a;
            String url=host_url+geturl(type);
            postData(url,a);
        }
        catch (Exception ex) {
            Log.d("ApiMAnager", "Exception Queue: " + ex);
        }

//        String url = AuthCredentials.getUrl() + "/api/" + AuthCredentials.getApiVersion() + "/" + Project.getProjectId() + "/instrument/"
//                + InstrumentConfig.getName() + "/participant/" + AuthCredentials.getParticipantId() + "/dataset/" + stream.getStreamId() + "/"
//                + stream.getSchemaVersion() + "/data";
        //if(tempr != null)
        // Capture the layout's TextView and set the string as its text
//        textView= (TextView) findViewById(R.id.textView2);
//        textView.setText(message);
    }


    private String postData(String url, JSONArray json) {
//        try {
//            HttpClient mHttpClient = new DefaultHttpClient();
//            HttpContext mHttpContext = new BasicHttpContext();
//            HttpPost post = new HttpPost(url);
//            post.setEntity(new StringEntity(json.toString(), UTF_8));
//            post.setHeader(mAuthorization, mBearer + " " + "1d4ddbce-c961-46ef-96cf-d77dae3a92df");
//
//            HttpResponse response = mHttpClient.execute(post, mHttpContext);
//            mIs = response.getEntity().getContent();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(mIs, "UTF-8"));
//            StringBuilder sb = new StringBuilder();
//            String line = null;
//            while ((line = reader.readLine()) != null) {
//                sb.append(line + "\n");
//            }
//            mIs.close();
//            mResult = sb.toString();
//            Log.d("ApiManager", "Upload result:" + mResult);
//            if (mResult.length() == 0) {
//                mResult = null;
//            }
//        } catch (UnsupportedEncodingException e) {
//            Log.d("ApiMAnager", "UnsupportedEncodingException: " + e);
//            e.printStackTrace();
//        } catch (ClientProtocolException e) {
//            Log.d("ApiMAnager", "ClientProtocolException: " + e);
//            e.printStackTrace();
//        } catch (IOException e) {
//            Log.d("ApiMAnager", "IOException: " + e);
//            e.printStackTrace();
//        }
//        return mResult;
        this.tempr=json;
        new background().execute(url);
        return "done";
    }

}
