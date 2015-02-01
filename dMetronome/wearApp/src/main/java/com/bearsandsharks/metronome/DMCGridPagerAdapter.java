package com.bearsandsharks.metronome;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.wearable.view.FragmentGridPagerAdapter;

public class DMCGridPagerAdapter extends FragmentGridPagerAdapter {

    public DMCGridPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount(int i) {
        return 1;
    }

    // This is where we should probably check if the client vairable is true
    // We can then force what fragments exist
    @Override
    public Fragment getFragment(int row, int col) {

            return new DMFSetTempo();

//        switch(col) {
//            case 0:
//                return new DMFSetTempo();
//            case 1:
//                return new DMFPreference();
//            case 2:
//                return new ClientTempo();
//            default:
//                return null;
    }
}
