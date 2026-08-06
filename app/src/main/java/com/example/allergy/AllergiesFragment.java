package com.example.allergy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AllergiesFragment extends Fragment {

    private RecyclerView recyclerView;
    private AllergiesAdapter adapter;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_allergies, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getInstance(requireContext());
        recyclerView = view.findViewById(R.id.recyclerViewAllergies);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Inicjalizacja domyślnych alergenów, jeśli baza jest pusta
        seedDatabaseIfEmpty();

        // Pobranie danych z bazy i wyświetlenie na liście
        List<Allergy> allergies = db.allergyDAO().getAllAllergies();
        adapter = new AllergiesAdapter(allergies, allergy -> {
            // Zapisz zmianę stanu w bazie Room
            db.allergyDAO().update(allergy);
        });
        recyclerView.setAdapter(adapter);
    }

    private void seedDatabaseIfEmpty() {
        if (db.allergyDAO().getCount() == 0) {
            List<Allergy> defaultAllergies = new ArrayList<>();
            defaultAllergies.add(new Allergy("Gluten", "en:gluten", false));
            defaultAllergies.add(new Allergy("Mleko (Laktoza)", "en:milk", false));
            defaultAllergies.add(new Allergy("Orzeszki ziemne", "en:peanuts", false));
            defaultAllergies.add(new Allergy("Orzechy", "en:nuts", false));
            defaultAllergies.add(new Allergy("Jaja", "en:eggs", false));
            defaultAllergies.add(new Allergy("Soja", "en:soybeans", false));
            defaultAllergies.add(new Allergy("Ryby", "en:fish", false));
            defaultAllergies.add(new Allergy("Skorupiaki", "en:crustaceans", false));
            defaultAllergies.add(new Allergy("Seler", "en:celery", false));
            defaultAllergies.add(new Allergy("Gorczyca (Musztarda)", "en:mustard", false));
            defaultAllergies.add(new Allergy("Nasiona sezamu", "en:sesame-seeds", false));
            defaultAllergies.add(new Allergy("Siarczyny", "en:sulphur-dioxide-and-sulphites", false));
            defaultAllergies.add(new Allergy("Łubin", "en:lupin", false));
            defaultAllergies.add(new Allergy("Mięczaki", "en:molluscs", false));

            db.allergyDAO().insertAll(defaultAllergies);
        }
    }
}