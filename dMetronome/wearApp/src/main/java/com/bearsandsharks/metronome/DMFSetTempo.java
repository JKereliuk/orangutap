package com.bearsandsharks.metronome;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.wearable.view.CircledImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.model.Circle;

public class DMFSetTempo extends Fragment {

    View rootView;
    DMCMetronome metronome;
    TextView hostCount, hostTimeSig, hostTempo;
    CircledImageView hostReset, miley;
    NotificationCompat.Builder notificationBuilder;
    NotificationManagerCompat notificationManager;
    // initialize values for textViews
    int mTempo = 120;
    static public int mPeriod = 4;
    int mCount = 1;
    boolean on;

    int tapCount;
    long timeTotal, lastTap, currentTime;
    Context mContext;
    PowerManager.WakeLock wakeLock;


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
        hostTempo.setText(Integer.toString(mTempo));

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
                if (!on) {
                    if (mPeriod == 9) {
                        mPeriod = 0;
                    }
                    mPeriod++;
                    hostTimeSig.setText(Integer.toString(mPeriod) + "/4");
                }
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
                    miley.setImageDrawable(getResources().getDrawable(R.drawable.red_circle));
                    metronome.startTick(mTempo);

                    on = true;
                }
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hostCount.setText(Integer.toString(1));
                miley.setImageDrawable(getResources().getDrawable(R.drawable.tap));
                onDestroy();
            }
        });

        return rootView;
    }


    public void onDestroy() {

        super.onDestroy();
        metronome.stopTick();
        tapCount = 0;
        timeTotal = 0;

        on = false;


        if (wakeLock.isHeld()) wakeLock.release();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        notificationManager.cancel(1);
    }

    private void setTempo(int tempo) {
        if (tempo < 0 || tempo > 9999) return;
        mTempo = tempo;
    }
}
