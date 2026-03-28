package com.example.emergencyapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.emergencyapp.fragments.ChatFragment;
import com.example.emergencyapp.fragments.HintsFragment;
import com.example.emergencyapp.fragments.MapFragment;

public class TabPagerAdapter extends FragmentStateAdapter {
    private String emergencyId;
    private boolean isSender;

    public TabPagerAdapter(@NonNull FragmentActivity fragmentActivity, String emergencyId, boolean isSender) {
        super(fragmentActivity);
        this.emergencyId = emergencyId;
        this.isSender = isSender;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return MapFragment.newInstance(emergencyId);
            case 1:
                return ChatFragment.newInstance(emergencyId, isSender);
            case 2:
                return HintsFragment.newInstance(emergencyId);
            default:
                return MapFragment.newInstance(emergencyId);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}