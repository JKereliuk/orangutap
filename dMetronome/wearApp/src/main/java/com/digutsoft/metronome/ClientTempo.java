package com.digutsoft.metronome;

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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.sql.Time;

public class ClientTempo extends Fragment implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    View rootView;
    DMCMetronome metronome;
    TextView tvTempo;
    SeekBar sbTempo;
    NotificationCompat.Builder notificationBuilder;
    NotificationManagerCompat notificationManager;
    int mBpm;
    long mStartTime;
    Context mContext;
    PowerManager.WakeLock wakeLock;
    // Client variable that will be set somewhere
    boolean isClient = true;
    private GoogleApiClient mGoogleApiClient;

    private static final String BPM_KEY = "com.example.key.BPM";
    private static final String TIME_KEY = "com.example.key.TIME";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.settempo, container, false);

        tvTempo = (TextView) rootView.findViewById(R.id.tvTempoClient);
        sbTempo = (SeekBar) rootView.findViewById(R.id.sbTempo);

        final CircledImageView btStart = (CircledImageView) rootView.findViewById(R.id.btStart);
        final CircledImageView btPlus = (CircledImageView) rootView.findViewById(R.id.btPlus);
        final CircledImageView btMinus = (CircledImageView) rootView.findViewById(R.id.btMinus);
        final CircledImageView triangle = (CircledImageView) rootView.findViewById(R.id.Triangle);
        // make all the things from host invisible
        btStart.setVisibility(View.GONE);
        btPlus.setVisibility(View.GONE);
        btMinus.setVisibility(View.GONE);
        sbTempo.setVisibility(View.GONE);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Intent viewIntent = new Intent(getActivity(), DMAMain.class);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(getActivity(), 0, viewIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(getActivity())
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(viewPendingIntent);
        mContext = getActivity().getApplicationContext();

        notificationManager = NotificationManagerCompat.from(getActivity());

        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        metronome = new DMCMetronome(getActivity(), vibrator, rootView.findViewById(R.id.bilBackground));

        mBpm = 150;
        mStartTime = 0;

        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        //wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getString(R.string.app_name));

        // This is the function that should recieve the message about the tempo and offset
        // I'm not exactly sure how the message is recieved, but this https://developer.android.com/training/wearables/data-layer/messages.html
        // says we implement a MessageListener Interface.
        // One hacky way I thought of doing it is making the StartActivity path passed by the message
        // actually hold the info of the tempo and offset value we need.
        // You probably have a better idea but you are sleeping now :)

        //tempo start was here
        triangle.setOnClickListener(new View.OnClickListener() {
            boolean on = false;
            @Override
            public void onClick(View view) {
                if (!on){
                    on = true;
                    tvTempo.setText(Integer.toString(mBpm));
                    // start the metronome at the tempo and offset specified (offset will be implemented)
                    metronome.startTick(mBpm);
                    // set the text to the tempo mark
                    tvTempo.setText(Integer.toString(mBpm));

                }
                else {
                    on = false;
                    metronome.stopTick();
                }
            }
        });

        Toast.makeText(getActivity(), "JUST A TOAST", Toast.LENGTH_LONG).show();

        return rootView;
    }

    public void updateTempo(Long startTime, int tempo) {
        setTempo(tempo);
        mStartTime = startTime;
    }

    public void onDestroy() {
        super.onDestroy();
        metronome.stopTick();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        notificationManager.cancel(1);
    }

    private void setTempo(int tempo) {
        //we changed tempo max to 240
        if (tempo < 0 || tempo > 240) return;
        tvTempo.setText(Integer.toString(tempo));
        mBpm = tempo;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        Log.v("myTag", "Connected to phone");

        Toast.makeText(getActivity(), "Connected to Phone", Toast.LENGTH_LONG).show();
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
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    updateTempo(dataMap.getLong(TIME_KEY), dataMap.getInt(BPM_KEY));
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v("myTag", "connection failed");
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
