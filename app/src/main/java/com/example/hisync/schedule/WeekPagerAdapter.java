package com.example.hisync.schedule;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.time.LocalDate;

public class WeekPagerAdapter extends FragmentStateAdapter {

    // We show a large fixed number of pages centered on today
    // Page 1000 = today's week, lower = past, higher = future
    private static final int TOTAL_PAGES = 2000;
    private static final int CENTER_PAGE = 1000;

    private final LocalDate today;

    public WeekPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
        this.today = LocalDate.now();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int weekOffset = position - CENTER_PAGE;
        LocalDate weekStart = today.with(
                java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)
        ).plusWeeks(weekOffset);
        return WeekFragment.newInstance(weekStart.toString());
    }

    @Override
    public int getItemCount() {
        return TOTAL_PAGES;
    }

    public static int getCenterPage() {
        return CENTER_PAGE;
    }
}