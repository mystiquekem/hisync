package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.LoginResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InstrumentSetupActivity extends AppCompatActivity {

    private static final List<String> INSTRUMENTS = Arrays.asList(
            "guitar", "bass", "drums", "keyboard",
            "vocals", "violin", "saxophone", "other"
    );

    private static final Map<String, String> LABELS = new HashMap<String, String>() {{
        put("guitar",     "🎸 Guitar");
        put("bass",       "🎸 Bass");
        put("drums",      "🥁 Drums");
        put("keyboard",   "🎹 Keyboard");
        put("vocals",     "🎤 Vocals");
        put("violin",     "🎻 Violin");
        put("saxophone",  "🎷 Saxophone");
        put("other",      "🎵 Other");
    }};

    private ChipGroup chipGroup;
    private MaterialButton btnContinue;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_instrument_setup);

        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);
        userId = prefs.getLong("userId", -1);

        chipGroup   = findViewById(R.id.chipGroupInstruments);
        btnContinue = findViewById(R.id.btnContinue);

        // Build chips programmatically so we don't need one XML per instrument
        for (String key : INSTRUMENTS) {
            Chip chip = new Chip(this);
            chip.setText(LABELS.get(key));
            chip.setTag(key);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setChipBackgroundColorResource(R.color.bg_surface);
            chip.setTextColor(getColor(R.color.text_primary));
            chip.setRippleColorResource(R.color.purple_primary);
            chipGroup.addView(chip);
        }

        btnContinue.setOnClickListener(v -> {
            List<String> selected = getSelected();
            if (selected.isEmpty()) {
                Toast.makeText(this, "Pick at least one instrument", Toast.LENGTH_SHORT).show();
                return;
            }
            saveInstruments(selected);
        });
    }

    private List<String> getSelected() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.isChecked()) result.add((String) chip.getTag());
        }
        return result;
    }

    private void saveInstruments(List<String> instruments) {
        btnContinue.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("instruments", instruments);

        RetrofitClient.getInstance().getApi()
                .updateProfile(userId, body)
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        btnContinue.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            // Persist locally
                            List<String> saved = response.body().getInstruments();
                            if (saved != null) {
                                getSharedPreferences("hisync", MODE_PRIVATE).edit()
                                        .putString("instruments",
                                                android.text.TextUtils.join(",", saved))
                                        .apply();
                            }
                            navigateNext();
                        } else {
                            Toast.makeText(InstrumentSetupActivity.this,
                                    "Failed to save instruments", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        btnContinue.setEnabled(true);
                        Toast.makeText(InstrumentSetupActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateNext() {
        // If coming from onboarding/register → go to BandSetup
        // If coming from profile (editing) → go back
        boolean fromProfile = getIntent().getBooleanExtra("fromProfile", false);
        if (fromProfile) {
            finish();
        } else {
            long bandId = getSharedPreferences("hisync", MODE_PRIVATE).getLong("bandId", -1);
            Intent next = bandId == -1
                    ? new Intent(this, BandSetupActivity.class)
                    : new Intent(this, MainActivity.class);
            startActivity(next);
            finishAffinity();
        }
    }
}