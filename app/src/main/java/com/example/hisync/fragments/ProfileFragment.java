package com.example.hisync.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.hisync.MainActivity;
import com.example.hisync.R;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("hisync", AppCompatActivity.MODE_PRIVATE);
        String displayName = prefs.getString("displayName", "");
        String email       = prefs.getString("email", "");
        String name = (displayName != null && !displayName.isEmpty())
                ? displayName : email.split("@")[0];

        ((TextView) view.findViewById(R.id.tvProfileName)).setText(name);
        ((TextView) view.findViewById(R.id.tvProfileEmail)).setText(email);
        ((TextView) view.findViewById(R.id.tvProfileAvatar))
                .setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());

        MaterialButton btnSignOut = view.findViewById(R.id.btnProfileSignOut);
        btnSignOut.setOnClickListener(v -> ((MainActivity) requireActivity()).signOut());
    }
}