package com.example.allergy;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DummyFragment extends Fragment {

    private static final String ARG_TITLE = "title";

    public DummyFragment() {
        // Wymagany pusty konstruktor
    }

    public static DummyFragment newInstance(String title) {
        DummyFragment fragment = new DummyFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);

        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        String title = "";

        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
        }

        TextView textView = new TextView(requireContext());

        textView.setText(title);
        textView.setTextSize(24f);
        textView.setGravity(Gravity.CENTER);

        return textView;
    }
}