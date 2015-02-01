package com.digutsoft.metronome;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import java.util.Random;


public class MainActivity extends Activity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private boolean mIsHosting;
    private String mFirebaseKey;
    private GoogleApiClient mGoogleApiClient;
    private int mBpm = 0;
    private Long mStartTime;

    private static final String BPM_KEY = "com.example.key.BPM";
    private static final String TIME_KEY = "com.example.key.TIME";

    private Firebase mFirebaseRef;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            Log.v("mytag", "handler is handling! Hosting firebase");

            // this is mostly just for safety not sure if I need it
            hostFirebase();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        mFirebaseRef = new Firebase("https://blazing-fire-4007.firebaseio.com/");

        setContentView(R.layout.activity_main);

        mIsHosting = false;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Button watch = (Button) findViewById(R.id.watch);
        watch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateBPM(System.currentTimeMillis(), new Random().nextInt(40) + 100);
                updateWatchData();
            }
        });

        final Button joinBtn = (Button) findViewById(R.id.join_btn);
        joinBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = ((EditText) findViewById(R.id.firebase_key_input)).getText().toString();
                if (key != null && !key.isEmpty()) {
                    joinFirebase(key);
                }
            }
        });


        final Button hostBtn = (Button) findViewById(R.id.host_btn);
        hostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hostFirebase();
            }
        });
    }

    private void updateWatchData() {
        // Create a DataMap object and send it to the data layer
        DataMap dataMap = new DataMap();
        dataMap.putLong(TIME_KEY, mStartTime);
        dataMap.putInt(BPM_KEY, mBpm);
        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread("/bpm", dataMap).start();
        Toast.makeText(MainActivity.this, "Sent message to watch", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

     public void joinFirebase (String firebaseSessionKey){

         mIsHosting = false;

         mFirebaseRef.child(firebaseSessionKey).addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(DataSnapshot snapshot) {
                 Long fireBaseStartTime = (Long) snapshot.child("startTime").getValue();
                 Long fireBasebpm = (Long) snapshot.child("bpm").getValue();
                 if (fireBaseStartTime != null && fireBasebpm != null ) {
                     long fireBaseStartTimeLong = fireBaseStartTime;
                     int fireBasebpmInt = fireBasebpm.intValue();
                     Toast.makeText(MainActivity.this, String.format("Got BPM %d and time %d", fireBasebpmInt, fireBaseStartTimeLong), Toast.LENGTH_SHORT).show();
                     updateBPM(fireBaseStartTimeLong, fireBasebpmInt);
                     updateWatchData();
                 }
             }

             @Override
             public void onCancelled(FirebaseError firebaseError) {
                 System.out.println("The read failed: " + firebaseError.getMessage());
             }
         });
     }


    @Override
    protected void onResume() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Toast.makeText(MainActivity.this, "Connected to Watch", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/bpm") == 0) {
                    Log.v("mytag", "Got new bpm from watch");
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    updateBPM(dataMap.getLong(TIME_KEY), dataMap.getInt(BPM_KEY));
                    hostFirebase();
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    private String generateFirebaseKey() {
        String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(4);
        for( int i = 0; i < 4; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }

    private void hostFirebase() {
        if(!mIsHosting) {
            mIsHosting = true;
            mFirebaseKey = generateFirebaseKey();
            Log.v("mytag", "now hosting on firebase");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) findViewById(R.id.firebase_key)).setText(mFirebaseKey);

                }
            });
        }
        Firebase usersRef = mFirebaseRef.child(mFirebaseKey);
        usersRef.child("bpm").setValue(mBpm);
        usersRef.child("startTime").setValue(mStartTime);
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(MainActivity.this, "Connection to watch failed", Toast.LENGTH_SHORT).show();
    }

    public void updateBPM(long startTime, int bpm) {
        mStartTime = startTime;
        mBpm = bpm;
    }

    class SendToDataLayerThread extends Thread {
        String path;
        DataMap dataMap;

        // Constructor for sending data objects to the data layer
        SendToDataLayerThread(String p, DataMap data) {
            path = p;
            dataMap = data;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {

                // Construct a DataRequest and send over the data layer
                PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                putDMR.getDataMap().putAll(dataMap);
                PutDataRequest request = putDMR.asPutDataRequest();
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(mGoogleApiClient,request).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send DataMap");
                }
            }
        }
    }
}