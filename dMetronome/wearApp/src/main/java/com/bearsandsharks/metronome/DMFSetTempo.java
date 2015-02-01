package com.bearsandsharks.metronome;

import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.wearable.view.CircledImageView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class DMFSetTempo extends Fragment
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{

    View rootView;
    DMCMetronome metronome;
    TextView hostCount, hostTimeSig, hostTempo;
    CircledImageView hostReset;
    NotificationCompat.Builder notificationBuilder;
    NotificationManagerCompat notificationManager;
    // initialize values for textViews
    int mBpm = 120;
    static public int mPeriod = 4;
    int mCount = 1;

    int tapCount;
    long timeTotal, lastTap, currentTime;
    Context mContext;
    PowerManager.WakeLock wakeLock;
    private GoogleApiClient mGoogleApiClient;
    private long mStartTime;


    private static final String BPM_KEY = "com.example.key.BPM";
    private static final String TIME_KEY = "com.example.key.TIME";

    // Client variable that will be set somewhere

//    SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.host, container, false);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        CircledImageView reset = (CircledImageView) rootView.findViewById(R.id.Reset);
        final CircledImageView miley = (CircledImageView) rootView.findViewById(R.id.Miley);

        final TextView hostCount = (TextView) rootView.findViewById(R.id.Count);
        final TextView hostTimeSig = (TextView) rootView.findViewById(R.id.TimeSig);
        final TextView hostTempo = (TextView) rootView.findViewById(R.id.Tempo);

        hostCount.setText(Integer.toString(mCount));
        hostTimeSig.setText(Integer.toString(mPeriod) + "/4");
        hostTempo.setText(Integer.toString(mBpm));

        mContext = getActivity();

        setTempo(120);

        Intent viewIntent = new Intent(getActivity(), DMAMain.class);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(getActivity(), 0, viewIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(getActivity())
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(viewPendingIntent);

        notificationManager = NotificationManagerCompat.from(getActivity());

        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        metronome = new DMCMetronome(getActivity(), vibrator, rootView.findViewById(R.id.bilBackground));
//        mPeriod = metronome.getMPeriod();


        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getString(R.string.app_name));

        hostTimeSig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPeriod == 9){
                    mPeriod = 0;
                }
                mPeriod++;
                hostTimeSig.setText(Integer.toString(mPeriod) + "/4");

            }
        });

        miley.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(tapCount == 0) {
                    lastTap = System.currentTimeMillis();
                    tapCount++;
                }
                else if(tapCount < (mPeriod - 1)) {
                    currentTime = System.currentTimeMillis();
                    timeTotal += currentTime - lastTap;
                    tapCount++;
                    lastTap = currentTime;
                }
                else if(tapCount == mPeriod - 1) {
                    currentTime = System.currentTimeMillis();
                    timeTotal += currentTime - lastTap;
                    lastTap = currentTime;

                    int total = (int) timeTotal;

                    setTempo((60000 * (mPeriod - 1)) /total);
                    tapCount = mPeriod;
                    hostTempo.setText(Integer.toString((60000 * (mPeriod - 1)) /total));
                    metronome.startTick(mBpm);
                    mStartTime = System.currentTimeMillis();
                    updatePhoneData();
                }
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDestroy();
            }
        });





//        sbTempo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
//                setTempo(i);
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//            }
//        });

//        btStart.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mBpm == 0) {
//                    Toast.makeText(getActivity(), R.string.tempo_zero, Toast.LENGTH_LONG).show();
//                    return;
//                }
//
//                //If it's running stop it, enable it, put tempo back on string, put image, and put arrow keys to visable
//                if (DMCMetronome.mRunning) {
//                    metronome.stopTick();
//
//                    sbTempo.setEnabled(true);
//                    tvTempo.setText(Integer.toString(mBpm));
//                    btStart.setImageDrawable(getResources().getDrawable(R.drawable.ic_start));
//                    btStart.setCircleColor(getResources().getColor(R.color.green));
//                    btPlus.setVisibility(View.VISIBLE);
//                    btMinus.setVisibility(View.VISIBLE);
//
//                    wakeLock.release();
//
//                    notificationManager.cancel(1);
//                } else {
//                    //if not running start the tick, change visibility and stuff
//                    metronome.startTick(mBpm);
//
//                    sbTempo.setEnabled(false);
//                    btStart.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop));
//                    btStart.setCircleColor(getResources().getColor(R.color.red));
//                    btPlus.setVisibility(View.GONE);
//                    btMinus.setVisibility(View.GONE);
//
//                    wakeLock.acquire();
//
//                    notificationBuilder.setContentText(String.format(getString(R.string.notification_running), mBpm));
//                    notificationManager.notify(1, notificationBuilder.build());
//                }
//            }
//        });

//        btPlus.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                setTempo(mBpm + 1);
//            }
//        });
//
//        btMinus.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                setTempo(mBpm - 1);
//            }
//        });

        return rootView;
    }


    public void onDestroy() {
        super.onDestroy();
        metronome.stopTick();
        tapCount = 0;
        timeTotal = 0;

        if (wakeLock.isHeld()) wakeLock.release();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        notificationManager.cancel(1);
    }

    private void setTempo(int tempo) {
        //we changed tempo max to 240
        if (tempo < 0 || tempo > 9999) return;
//        tvTempo.setText(Integer.toString(tempo));
//        sbTempo.setProgress(tempo);
        mBpm = tempo;
    }

    public void updatePhoneData() {
        // Create a DataMap object and send it to the data layer
        DataMap dataMap = new DataMap();
        dataMap.putLong(TIME_KEY, mStartTime);
        dataMap.putInt(BPM_KEY, mBpm);
        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread("/bpm", dataMap).start();
        Toast.makeText(getActivity(), "Sent request to phone", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v("myTag", "Connected to phone");
        Toast.makeText(getActivity(), "Connected to Phone", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v("myTag", "connection failed");
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

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }
}
