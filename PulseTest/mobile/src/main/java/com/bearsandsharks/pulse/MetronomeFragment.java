package com.bearsandsharks.pulse;

import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;


public class MetronomeFragment extends Fragment {

    private Button mStartBtn;
    private Button mStopBtn;

    public MetronomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View v = inflater.inflate(R.layout.fragment_metronome, container, false);
        mStartBtn = (Button) v.findViewById(R.id.start_button);
        mStopBtn = (Button) v.findViewById(R.id.stop_button);

        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Started metronome", Toast.LENGTH_SHORT).show();
            }
        });

        mStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Stopped metronome", Toast.LENGTH_SHORT).show();
            }
        });
        return v;

    }

}
