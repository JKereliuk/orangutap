package com.bearsandsharks.metronome;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.wearable.view.CircledImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

public class DMFSetTempo extends Fragment implements MetronomeFragment
{
    View rootView;
    DMCMetronome metronome;
    TextView hostCount, hostTimeSig, hostTempo;
    CircledImageView hostReset;
    NotificationCompat.Builder notificationBuilder;
    NotificationManagerCompat notificationManager;
    // initialize values for textViews
    static public int mPeriod = 4;
    int mCount = 1;

    int tapCount;
    long timeTotal, lastTap, currentTime;
    PowerManager.WakeLock wakeLock;

    // Client variable that will be set somewhere

//    SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.host, container, false);

        CircledImageView reset = (CircledImageView) rootView.findViewById(R.id.Reset);
        final CircledImageView miley = (CircledImageView) rootView.findViewById(R.id.Miley);

        final TextView hostCount = (TextView) rootView.findViewById(R.id.Count);
        final TextView hostTimeSig = (TextView) rootView.findViewById(R.id.TimeSig);
        final TextView hostTempo = (TextView) rootView.findViewById(R.id.Tempo);

        hostCount.setText(Integer.toString(mCount));
        hostTimeSig.setText(Integer.toString(mPeriod) + "/4");
        hostTempo.setText(Integer.toString(0));

        Intent viewIntent = new Intent(getActivity(), DMAMain.class);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(getActivity(), 0, viewIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(getActivity())
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(viewPendingIntent);

        notificationManager = NotificationManagerCompat.from(getActivity());

//        mPeriod = metronome.getMPeriod();


//        PowerManager powerManager = (PowerManager) main.getSystemService(Context.POWER_SERVICE);
//        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getString(R.string.app_name));

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


                    getMain().setBpm((60000 * (mPeriod - 1)) /total);
                    getMain().setStartTime(System.currentTimeMillis());
                    tapCount = mPeriod;
                    hostTempo.setText(Integer.toString((60000 * (mPeriod - 1)) / total));
                    getMetronome().startTick(getMain().getBpm());
                    getMain().updatePhoneData();
                }
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDestroy();
            }
        });

        return rootView;
    }


    public void onDestroy() {
        super.onDestroy();
        getMetronome().stopTick();
        tapCount = 0;
        timeTotal = 0;

//        if (wakeLock.isHeld()) wakeLock.release();
//        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        notificationManager.cancel(1);
    }

    @Override
    public DMCMetronome getMetronome() {
        if (metronome == null) {
            Vibrator vibrator = (Vibrator) getMain().getSystemService(Context.VIBRATOR_SERVICE);
            metronome = new DMCMetronome(getMain(), vibrator, rootView.findViewById(R.id.bilBackground));
        }
        return metronome;
    }

    @Override
    public void updateVisuals() {
        //tTempo.setText(Integer.toString(mBpm));
    }

    public DMAMain getMain() {
        return (DMAMain) getActivity();
    }

}
