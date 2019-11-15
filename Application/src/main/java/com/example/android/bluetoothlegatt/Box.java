package com.example.android.bluetoothlegatt;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxEntity;
import com.box.androidsdk.content.models.BoxError;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;


public class Box extends IntentService implements BoxAuthentication.AuthListener {

    BoxSession mSession = null;
    BoxSession mOldSession = null;





    //private ArrayAdapter<BoxItem> mAdapter;

    private BoxApiFolder mFolderApi;
    private BoxApiFile mFileApi;
    public Box() {
        super("Box");
    }



    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            //final String action = intent.getAction();
            BoxConfig.IS_LOG_ENABLED = true;
            configureClient();
            initSession();
            //if(action=="upload"){

            //}
            uploadSampleFile();

        }
    }
    private void uploadSampleFile() {
        new Thread() {
            @Override
            public void run() {
                try {
                    File file = new File(Environment.getExternalStorageDirectory() + File.separator + "test.jpg");

                    //String uploadFileName = "box_logo.png";
                    InputStream uploadStream = new FileInputStream(file);

                    String destinationFolderId = "0";
                    String uploadName = "BoxSDKUpload2.png";
                    BoxRequestsFile.UploadFile request = mFileApi.getUploadRequest(uploadStream, uploadName, destinationFolderId);
                    final BoxFile uploadFileInfo = request.send();
                } catch (IOException e) {
                    Log.d("EXCEPT",e.toString());
                    e.printStackTrace();
                } catch (BoxException e) {
                    Log.d("EXCEPT",e.toString());
                    e.printStackTrace();
                    BoxError error = e.getAsBoxError();
                    if (error != null && error.getStatus() == HttpURLConnection.HTTP_CONFLICT) {
                        ArrayList<BoxEntity> conflicts = error.getContextInfo().getConflicts();
                        if (conflicts != null && conflicts.size() == 1 && conflicts.get(0) instanceof BoxFile) {
                            return;
                        }
                    }

                }
                catch (Exception e){
                    Log.d("EXCEPT",e.toString());
                }
                finally {

                }
            }
        }.start();

    }
    private void configureClient() {
        BoxConfig.CLIENT_ID = "c7ai5c6k9o45tzn98m62udt8rq9j52xd";
        BoxConfig.CLIENT_SECRET = "apaGFUm7KVNxQZ9KinRDGg9k68AexsBx";

        // needs to match redirect uri in developer settings if set.
        BoxConfig.REDIRECT_URL = "https://0.0.0.0";
    }

    /**
     * Create a BoxSession and authenticate.
     */
    private void initSession() {
        mSession = new BoxSession(this);
        mSession.setSessionAuthListener(this);
        mSession.authenticate();
        try{
            mFolderApi = new BoxApiFolder(mSession);
            mFileApi = new BoxApiFile(mSession);
        }
        catch(Exception e){
            Log.d("boxAUthenticate",e.toString());
        }

    }

    @Override
    public void onRefreshed(BoxAuthentication.BoxAuthenticationInfo info) {
        // do nothing when auth info is refreshed
    }

    @Override
    public void onAuthCreated(BoxAuthentication.BoxAuthenticationInfo info) {
        //Init file, and folder apis; and use them to fetch the root folder
        mFolderApi = new BoxApiFolder(mSession);
        mFileApi = new BoxApiFile(mSession);
    }

    @Override
    public void onAuthFailure(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
        if (ex != null) {

        } else if (info == null && mOldSession != null) {
            mSession = mOldSession;
            mSession.setSessionAuthListener(this);
            mOldSession = null;
            onAuthCreated(mSession.getAuthInfo());
        }
    }

    @Override
    public void onLoggedOut(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
        initSession();
    }


}
