package com.example.hisync.schedule;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.hisync.R;
import com.example.hisync.fragments.CreateSessionBottomSheet;
import com.example.hisync.schedule.WeekPagerAdapter;

public class ScheduleFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewPager2 weekPager = view.findViewById(R.id.weekPager);
        WeekPagerAdapter adapter = new WeekPagerAdapter(requireActivity());
        weekPager.setAdapter(adapter);
        weekPager.setCurrentItem(WeekPagerAdapter.getCenterPage(), false);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("hisync", AppCompatActivity.MODE_PRIVATE);
        boolean isLeader = "leader".equals(prefs.getString("role", "member"));
        long bandId = prefs.getLong("bandId", -1);
        long userId = prefs.getLong("userId", -1);

        View fab = view.findViewById(R.id.fabAddSession);
        if (isLeader && bandId != -1 && userId != -1) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> {
                CreateSessionBottomSheet sheet =
                        CreateSessionBottomSheet.newInstance(bandId, userId);
                sheet.setOnCreatedListener(() -> {
                    Fragment current = getChildFragmentManager()
                            .findFragmentByTag("f" + weekPager.getCurrentItem());
                    if (current instanceof WeekFragment) {
                        ((WeekFragment) current).refresh();
                    }
                });
                sheet.show(getParentFragmentManager(), "create_session");
            });
        }
    }
}
