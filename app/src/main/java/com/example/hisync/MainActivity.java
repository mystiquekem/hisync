package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.hisync.fragments.BandFragment;
import com.example.hisync.fragments.HomeFragment;
import com.example.hisync.fragments.LineupsFragment;
import com.example.hisync.fragments.ProfileFragment;
import com.example.hisync.fragments.ScheduleFragment;
import com.example.hisync.fragments.TasksFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayDeque;
import java.util.Deque;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    private boolean isLeader;

    // Leader positions: Home | Lineups | Schedule | Tasks | Band | Profile
    private static final int POS_HOME = 0;

    private final Deque<Integer> backStack = new ArrayDeque<>();
    private int currentPosition = POS_HOME;
    private boolean isNavigatingProgrammatically = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Apply saved theme before setContentView
        int savedTheme = getSharedPreferences("hisync", MODE_PRIVATE)
                .getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);
        if (prefs.getLong("userId", -1) == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String role = prefs.getString("role", "member");
        isLeader = "leader".equals(role) || "admin".equals(role);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(statusBarSpacer, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = top;
            v.setLayoutParams(lp);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(0, 0, 0, bottom);
            return insets;
        });

        FrameLayout fragmentContainer = findViewById(R.id.fragmentContainer);
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.statusBars()).top, 0, 0);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = 56 + bottom;
            v.setLayoutParams(lp);
            return insets;
        });

        bottomNav.inflateMenu(isLeader
                ? R.menu.bottom_nav_menu_leader
                : R.menu.bottom_nav_menu);

        viewPager.setAdapter(new MainPagerAdapter(this, isLeader));
        viewPager.setOffscreenPageLimit(isLeader ? 5 : 3);
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
        if (id == R.id.nav_home)     return 0;
        if (id == R.id.nav_lineups)  return isLeader ? 1 : 0;
        if (id == R.id.nav_schedule) return isLeader ? 2 : 1;
        if (id == R.id.nav_tasks)    return isLeader ? 3 : 2;
        if (id == R.id.nav_band)     return isLeader ? 4 : 0;
        if (id == R.id.nav_profile)  return isLeader ? 5 : 3;
        return 0;
    }

    private int idForPosition(int pos) {
        if (isLeader) {
            switch (pos) {
                case 1: return R.id.nav_lineups;
                case 2: return R.id.nav_schedule;
                case 3: return R.id.nav_tasks;
                case 4: return R.id.nav_band;
                case 5: return R.id.nav_profile;
                default: return R.id.nav_home;
            }
        } else {
            switch (pos) {
                case 1: return R.id.nav_schedule;
                case 2: return R.id.nav_tasks;
                case 3: return R.id.nav_profile;
                default: return R.id.nav_home;
            }
        }
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {
        private final boolean isLeader;

        MainPagerAdapter(AppCompatActivity activity, boolean isLeader) {
            super(activity);
            this.isLeader = isLeader;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (isLeader) {
                switch (position) {
                    case 1: return new LineupsFragment();
                    case 2: return new ScheduleFragment();
                    case 3: return new TasksFragment();
                    case 4: return new BandFragment();
                    case 5: return new ProfileFragment();
                    default: return new HomeFragment();
                }
            } else {
                switch (position) {
                    case 1: return new ScheduleFragment();
                    case 2: return new TasksFragment();
                    case 3: return new ProfileFragment();
                    default: return new HomeFragment();
                }
            }
        }

        @Override
        public int getItemCount() {
            return isLeader ? 6 : 4;
        }
    }
}