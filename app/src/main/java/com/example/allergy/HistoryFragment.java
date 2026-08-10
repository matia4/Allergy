package com.example.allergy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private AppDatabase db;
    private SwitchMaterial switchTestTtl;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.history_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getInstance(requireContext());
        recyclerView = view.findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        switchTestTtl = view.findViewById(R.id.switchTestTtl);
        prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        // 1. Wczytaj zapamiętany stan z SharedPreferences
        boolean isTestTtlActive = prefs.getBoolean("test_ttl_enabled", false);
        switchTestTtl.setChecked(isTestTtlActive);

        // 2. Reaguj na zmianę switcha i zapisuj stan
        switchTestTtl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("test_ttl_enabled", isChecked).apply();
            loadHistory(); // Odśwież historię z nowym czasem ważności
        });

        loadHistory();
    }

    private void loadHistory() {
        List<Product> products = db.productDAO().getAllProducts();
        adapter = new HistoryAdapter(products, product -> {
            Context context = getContext();
            if (context == null) return;

            // Po kliknięciu usuwamy wykrzyknik "nowy alert" i zapisujemy stan
            if (product.isNewAlert()) {
                product.setNewAlert(false);
                db.productDAO().update(product);
                loadHistory(); // Odświeżamy listę
            }

            // Wyświetlamy szczegóły składu
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle(product.getName())
                    .setMessage("SKŁAD:\n" + product.getIngredients())
                    .setPositiveButton("Zamknij", null)
                    .show();
        });
        recyclerView.setAdapter(adapter);
    }
}