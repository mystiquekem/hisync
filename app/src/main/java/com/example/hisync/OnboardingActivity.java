package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

import androidx.fragment.app.Fragment;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private LinearLayout dotsLayout;
    private MaterialButton btnNext, btnSkip;

    private static final int PAGE_COUNT = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        pager      = findViewById(R.id.onboardingPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        btnNext    = findViewById(R.id.btnNext);
        btnSkip    = findViewById(R.id.btnSkip);

        pager.setAdapter(new OnboardingAdapter(this));
        setupDots(0);

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setupDots(position);
                if (position == PAGE_COUNT - 1) {
                    btnNext.setText("Get Started");
                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText("Next");
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = pager.getCurrentItem();
            if (current < PAGE_COUNT - 1) {
                pager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void setupDots(int activePage) {
        dotsLayout.removeAllViews();
        for (int i = 0; i < PAGE_COUNT; i++) {
            View dot = new View(this);
            int size = 8;
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            dpToPx(i == activePage ? 20 : size),
                            dpToPx(size)
                    );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(
                    i == activePage ? R.drawable.dot_active : R.drawable.dot_inactive
            );
            dotsLayout.addView(dot);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void finishOnboarding() {
        // Mark onboarding done for this device
        getSharedPreferences("hisync", MODE_PRIVATE).edit()
                .putBoolean("onboarding_done", true)
                .apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    static final int[]    ICONS    = {
            R.drawable.ic_nav_home,
            R.drawable.ic_nav_schedule,
            R.drawable.ic_nav_tasks,
            R.drawable.ic_nav_profile
    };
    static final String[] TITLES   = {
            "Welcome Home",
            "Your Schedule",
            "Your Tasks",
            "Your Profile"
    };
    static final String[] DESCS    = {
            "See your upcoming session and pending tasks at a glance right from the Home tab.",
            "The Schedule tab shows your weekly practice calendar.\nTap any session chip to see the lineup and tasks.",
            "The Tasks tab lists everything assigned to you.\nSwipe down to refresh — tap a task to update its status.",
            "View your info and sign out anytime from the Profile tab."
    };

    private static class OnboardingAdapter extends FragmentStateAdapter {
        OnboardingAdapter(AppCompatActivity a) { super(a); }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return OnboardingPageFragment.newInstance(position);
        }

        @Override
        public int getItemCount() { return PAGE_COUNT; }
    }

    // ── Page Fragment ─────────────────────────────────────────────────────────

    public static class OnboardingPageFragment extends Fragment {
        private static final String ARG_POS = "pos";

        public static OnboardingPageFragment newInstance(int pos) {
            OnboardingPageFragment f = new OnboardingPageFragment();
            Bundle b = new Bundle();
            b.putInt(ARG_POS, pos);
            f.setArguments(b);
            return f;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_page, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            int pos = requireArguments().getInt(ARG_POS);
            ((ImageView) view.findViewById(R.id.ivOnboardingIcon))
                    .setImageResource(ICONS[pos]);
            ((TextView)  view.findViewById(R.id.tvOnboardingTitle))
                    .setText(TITLES[pos]);
            ((TextView)  view.findViewById(R.id.tvOnboardingDesc))
                    .setText(DESCS[pos]);
        }
    }
}