package com.bearsandsharks.metronome;

import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;

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

import android.provider.Settings.Secure;


public class DMAMain extends FragmentActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;

    private int mBpm = 120;
    private long mStartTime;
    private MetronomeFragment clientFragment;
    private MetronomeFragment hostFragment;
    private MetronomeFragment mCurrentFragment;

    public static boolean isClient = false;
    private static final String BPM_KEY = "com.example.key.BPM";
    private static final String TIME_KEY = "com.example.key.TIME";
    private static final String OWNER_KEY = "com.example.key.OWNER";
    private String mAndroidId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAndroidId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        clientFragment = new ClientTempo();
        hostFragment = new DMFSetTempo();

        mCurrentFragment = hostFragment;

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);

        if(fragment == null) {
            fragment = (Fragment) mCurrentFragment;
            fm.beginTransaction()
                    .add(R.id.fragmentContainer, fragment).commit();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Log.v("myTag", "Connected to phone");
        Toast.makeText(DMAMain.this, "Connected to Phone", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        Log.v("myTag", "connection success");

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/bpm") == 0) {
                    final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                    if(!dataMap.getString(OWNER_KEY).equals(mAndroidId)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setClientMode(dataMap.getLong(TIME_KEY), dataMap.getInt(BPM_KEY));
                            }
                        });
                    }
                }
            }
        }
    }

    public void setClientMode(Long startTime, int bpm) {

        //update startTime and bpm variables
        setBpm(bpm);
        setStartTime(startTime);
        mStartTime = startTime;

        isClient = true;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

// Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack
        transaction.replace(R.id.fragmentContainer, (Fragment) clientFragment);
        transaction.addToBackStack(null);

// Commit the transaction
        transaction.commit();

        //update visuals, inflate fragment
        clientFragment.updateVisuals();

        // set the tempo text to what the phone sent
        //tTempo.setText(Integer.toString(mBpm));

        Log.v("mytag", String.format("The bpm is %d", bpm));
        long scheduledTime = (System.currentTimeMillis() - startTime) * (1 + getBpm()/60000) + startTime;
        Log.v("myTag", String.format("tick scheduled in %d", (scheduledTime - System.currentTimeMillis()) / 1000));
        mCurrentFragment.getMetronome().stopTick();
        Handler handler = mCurrentFragment.getMetronome().getHandler();
        Message message = new Message();
        Bundle b = new Bundle();
        b.putInt("bpm", getBpm());
        Log.v("mytag", String.format("The bundled bpm is  %d", bpm));
        message.setData(b);
        handler.sendMessageDelayed(message, scheduledTime - System.currentTimeMillis());
    }

    public void updatePhoneData() {
        // Create a DataMap object and send it to the data layer
        DataMap dataMap = new DataMap();
        dataMap.putString(OWNER_KEY, mAndroidId);
        dataMap.putLong(TIME_KEY, mStartTime);
        dataMap.putInt(BPM_KEY, mBpm);
        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread("/bpm", dataMap).start();
        Toast.makeText(DMAMain.this, "Sent request to phone", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v("myTag", "connection failed");
    }

    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    public int getBpm() {
        return mBpm;
    }

    public void setBpm(int bpm) {
        if (bpm <= 0 || bpm > 9999) return;
        mBpm = bpm;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public void setStartTime(long startTime) {
        this.mStartTime = startTime;
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
