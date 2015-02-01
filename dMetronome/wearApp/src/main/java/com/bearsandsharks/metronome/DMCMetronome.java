package com.bearsandsharks.metronome;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.wearable.view.CircledImageView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DMCMetronome {

    private static final int MSG = 1;
    protected static boolean mRunning = false;

    Vibrator mVibrator;

    View mBackground;
    TextView tCount;
    Drawable mDefaultBackground;

    int mCount = 0;
    int mPeriod;
    long mTickDuration;
    // set this somwhere
    boolean isClient = true;
    CircledImageView miley;

    Context mContext;

    public DMCMetronome(Context context, Vibrator vibrator, View view) {
        mContext = context;
        mVibrator = vibrator;
        mBackground = view;
        tCount = (TextView) view.findViewById(R.id.Count);
        if(!DMAMain.isClient) {
            miley = (CircledImageView) view.findViewById(R.id.Miley);
        }

        mDefaultBackground = view.getBackground();
//        mSharedPreferences = context.getSharedPreferences("dMetronome", 0);
    }

    public int getMPeriod() {
        return mPeriod;
    }

    public void startTick(int ticksPerSec) {

        mRunning = true;
        mCount = 0;
        //mPeriod is count default set to 4s
        if(DMAMain.isClient) {
            mPeriod = ClientTempo.timeSig;
        }
        else {
            mPeriod = DMFSetTempo.mPeriod;
        }

        tCount.setText(Integer.toString(1));

//      if (alwaysOnStatus) {
//          ((Activity) mContext).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//      }

        mTickDuration = 60000 / ticksPerSec;
        //call tick when you start tick
        tick();
    }

    private void tick() {
        if (!mRunning) return;
            if (mCount - mPeriod == 0) {
                mCount = 0;
                if(!DMAMain.isClient) {
                    miley.setCircleRadius(miley.getCircleRadius() - 10f);
                    }
                mVibrator.vibrate(120);
                }
            else {
                if(!DMAMain.isClient) {
                    miley.setCircleRadius(miley.getCircleRadius() - 10f);
                }
                mVibrator.vibrate(100);
            }


            //calls the Handler with the delay of mTickDuration for example 60000 / 60bpm = a tick every 1 second
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG), mTickDuration);
    }


    public void stopTick() {
            mRunning = false;
            mCount = 0;
            mBackground.setBackground(mDefaultBackground);
            tCount.setTextColor(Color.parseColor("#000000"));

            mHandler.removeMessages(MSG);

//            if (alwaysOnStatus) {
//                ((Activity) mContext).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//            }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            mCount++;
            tick();
            tCount.setText(Integer.toString(mCount + 1));

        }
    };

    private Handler metronomeHandler = new Handler() {
        public void handleMessage(Message message) {
            Log.v("mytag", "handler is handling!");

            // this is mostly just for safety not sure if I need it
            Bundle data = message.getData();
            int bpm = data.getInt("bpm");
            stopTick();
            startTick(bpm);
        }
    };

    public Handler getHandler() {
        return metronomeHandler;
    }
}
