package com.bearsandsharks.metronome;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

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
import android.widget.Toast;


public class ClientTempo extends Fragment implements MetronomeFragment {

    View rootView;
    DMCMetronome metronome;
    TextView tTempo, tTimeSig;
    TextView tCount;
    NotificationCompat.Builder notificationBuilder;
    NotificationManagerCompat notificationManager;
    Context mContext;
    boolean on = false;

    // set default count to 1 and time sig to 4
    int Count = 1;
    static public int timeSig = 4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.client, container, false);

        tTempo = (TextView) rootView.findViewById(R.id.Tempo);
        tTimeSig = (TextView) rootView.findViewById(R.id.TimeSig);
        tCount = (TextView) rootView.findViewById(R.id.Count);

        // set initial values for tempo, timesig and count
        tTempo.setText(Integer.toString(0));
        tTimeSig.setText(Integer.toString(4) + "/4");
        tCount.setText(Integer.toString(Count));


        final CircledImageView triangle = (CircledImageView) rootView.findViewById(R.id.Triangle);

        Intent viewIntent = new Intent(getActivity(), DMAMain.class);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(getActivity(), 0, viewIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(getActivity())
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(viewPendingIntent);
        mContext = getActivity().getApplicationContext();

        notificationManager = NotificationManagerCompat.from(getActivity());

        triangle.setOnClickListener(new View.OnClickListener() {
            //            boolean on = false;
            @Override
            public void onClick(View view) {
                getMetronome().stopTick();
                getMetronome().startTick(getMain().getBpm());
            }
        });
        Toast.makeText(getActivity(), "JUST A TOAST", Toast.LENGTH_LONG).show();
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getMetronome().stopTick();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        notificationManager.cancel(1);
    }

    @Override
    public void updateVisuals() {
//        tTempo.setText(Integer.toString(getMain().getBpm()));
    }

    public DMAMain getMain() {
        return (DMAMain) getActivity();
    }

    @Override
    public DMCMetronome getMetronome() {
        if (metronome == null) {
            Vibrator vibrator = (Vibrator) getMain().getSystemService(Context.VIBRATOR_SERVICE);
            metronome = new DMCMetronome(getMain(), vibrator, rootView.findViewById(R.id.CbilBackground));
        }
        return metronome;
    }
}
