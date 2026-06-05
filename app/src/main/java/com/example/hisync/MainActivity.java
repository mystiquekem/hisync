package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.hisync.fragments.HomeFragment;
import com.example.hisync.fragments.ProfileFragment;
import com.example.hisync.fragments.ScheduleFragment;
import com.example.hisync.fragments.SongsFragment;
import com.example.hisync.fragments.TasksFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayDeque;
import java.util.Deque;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    private static final int POS_HOME     = 0;
    private static final int POS_SONGS    = 1;
    private static final int POS_SCHEDULE = 2;
    private static final int POS_TASKS    = 3;
    private static final int POS_PROFILE  = 4;

    private final Deque<Integer> backStack = new ArrayDeque<>();
    private int currentPosition = POS_HOME;
    private boolean isNavigatingProgrammatically = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);
        if (prefs.getLong("userId", -1) == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNav);

        viewPager.setAdapter(new MainPagerAdapter(this));
        viewPager.setOffscreenPageLimit(4);
        viewPager.setUserInputEnabled(true);

        bottomNav.setOnItemSelectedListener(item -> {
            int pos = positionForId(item.getItemId());
            if (pos == currentPosition) return true;
            navigateTo(pos);
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (!isNavigatingProgrammatically) {
                    pushBackStack(currentPosition);
                    currentPosition = position;
                }
                bottomNav.setOnItemSelectedListener(null);
                bottomNav.setSelectedItemId(idForPosition(position));
                bottomNav.setOnItemSelectedListener(item -> {
                    int pos = positionForId(item.getItemId());
                    if (pos == currentPosition) return true;
                    navigateTo(pos);
                    return true;
                });
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!backStack.isEmpty()) {
                    int prev = backStack.pop();
                    isNavigatingProgrammatically = true;
                    currentPosition = prev;
                    viewPager.setCurrentItem(prev, true);
                    isNavigatingProgrammatically = false;
                } else {
                    finish();
                }
            }
        });
    }

    public void signOut() {
        getSharedPreferences("hisync", MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void navigateTo(int position) {
        pushBackStack(currentPosition);
        currentPosition = position;
        isNavigatingProgrammatically = true;
        viewPager.setCurrentItem(position, true);
        isNavigatingProgrammatically = false;
    }

    private void pushBackStack(int position) {
        if (!backStack.isEmpty() && backStack.peek() == position) return;
        backStack.push(position);
    }

    private int positionForId(int id) {
        if (id == R.id.nav_songs)    return POS_SONGS;
        if (id == R.id.nav_schedule) return POS_SCHEDULE;
        if (id == R.id.nav_tasks)    return POS_TASKS;
        if (id == R.id.nav_profile)  return POS_PROFILE;
        return POS_HOME;
    }

    private int idForPosition(int pos) {
        if (pos == POS_SONGS)    return R.id.nav_songs;
        if (pos == POS_SCHEDULE) return R.id.nav_schedule;
        if (pos == POS_TASKS)    return R.id.nav_tasks;
        if (pos == POS_PROFILE)  return R.id.nav_profile;
        return R.id.nav_home;
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {
        MainPagerAdapter(AppCompatActivity activity) { super(activity); }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case POS_SONGS:    return new SongsFragment();
                case POS_SCHEDULE: return new ScheduleFragment();
                case POS_TASKS:    return new TasksFragment();
                case POS_PROFILE:  return new ProfileFragment();
                default:           return new HomeFragment();
            }
        }

        @Override
        public int getItemCount() { return 5; }
    }
}