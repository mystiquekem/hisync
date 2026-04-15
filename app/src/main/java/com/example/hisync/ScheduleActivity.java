package com.example.hisync;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.hisync.schedule.WeekPagerAdapter;

public class ScheduleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        ViewPager2 pager = findViewById(R.id.weekPager);
        WeekPagerAdapter adapter = new WeekPagerAdapter(this);
        pager.setAdapter(adapter);
        pager.setCurrentItem(WeekPagerAdapter.getCenterPage(), false);
    }
}