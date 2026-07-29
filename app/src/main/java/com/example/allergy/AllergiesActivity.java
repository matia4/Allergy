package com.example.allergy;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AllergiesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    // Tutaj w przyszłości będzie Twój Adapter: private AllergyAdapter adapter;
    private List<Allergy> supportedAllergies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allergies);

        recyclerView = findViewById(R.id.recyclerViewAllergies);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicjalizacja domyślnych alergenów z Open Food Facts (EU Top 14)
        setupDefaultAllergies();

        // TODO: 1. Pobierz z bazy Room zapisane stany (isActive)
        // TODO: 2. Przekaż listę do AllergyAdapter
        // adapter = new AllergyAdapter(supportedAllergies);
        // recyclerView.setAdapter(adapter);
    }

    private void setupDefaultAllergies() {
        supportedAllergies = new ArrayList<>();
        supportedAllergies.add(new Allergy("Gluten", "en:gluten", false));
        supportedAllergies.add(new Allergy("Mleko (Laktoza)", "en:milk", false));
        supportedAllergies.add(new Allergy("Orzeszki ziemne", "en:peanuts", false));
        supportedAllergies.add(new Allergy("Orzechy", "en:nuts", false));
        supportedAllergies.add(new Allergy("Jaja", "en:eggs", false));
        supportedAllergies.add(new Allergy("Soja", "en:soybeans", false));
        supportedAllergies.add(new Allergy("Ryby", "en:fish", false));
        supportedAllergies.add(new Allergy("Skorupiaki", "en:crustaceans", false));
        supportedAllergies.add(new Allergy("Seler", "en:celery", false));
        supportedAllergies.add(new Allergy("Gorczyca (Musztarda)", "en:mustard", false));
        supportedAllergies.add(new Allergy("Nasiona sezamu", "en:sesame-seeds", false));
        supportedAllergies.add(new Allergy("Dwutlenek siarki i siarczyny", "en:sulphur-dioxide-and-sulphites", false));
        supportedAllergies.add(new Allergy("Łubin", "en:lupin", false));
        supportedAllergies.add(new Allergy("Mięczaki", "en:molluscs", false));
    }
}