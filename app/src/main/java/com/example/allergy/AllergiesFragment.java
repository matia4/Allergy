package com.example.allergy;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
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

        // 1. Inicjalizacja bazy danych Room oraz RecyclerView
        db = AppDatabase.getInstance(requireContext());
        recyclerView = view.findViewById(R.id.recyclerViewAllergies);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 2. Wypełnienie bazy początkowymi alergenami przy pierwszym uruchomieniu
        seedDatabaseIfEmpty();

        // 3. Pobranie listy alergenów i ustawienie adaptera
        List<Allergy> allergies = db.allergyDAO().getAllAllergies();

        adapter = new AllergiesAdapter(allergies, allergy -> {
            // Zapisz zmianę przełącznika w bazie Room
            db.allergyDAO().update(allergy);

            // MECHANIZM RETROAKTYWNY: Jeśli użytkownik włączył nową alergię,
            // przeszukaj historię skanów i zmień status produktów zawierających ten alergen.
            if (allergy.isActive()) {
                checkHistoryRetroactively(allergy);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    /**
     * Metoda uzupełniająca bazę Room 14 głównymi alergenami Unii Europejskiej,
     * jeśli baza jest jeszcze pusta.
     */
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

    /**
     * Przeszukuje tabelę z historią produktów i oznacza te,
     * które zawierają nowo aktywowany alergen.
     */
    private void checkHistoryRetroactively(Allergy newAllergy) {
        Context context = getContext();
        if (context == null) return;

        List<Product> allProducts = db.productDAO().getAllProducts();
        int newAlertCount = 0;

        for (Product product : allProducts) {
            // Sprawdzamy, czy tagi skanowanego wcześniej produktu zawierają aktywowany alergen
            if (product.getAllergensTagsJson() != null && product.getAllergensTagsJson().contains(newAllergy.getOffTag())) {

                // Jeśli produkt wcześniej był oznaczony jako bezpieczny
                if (!product.isAllergic()) {
                    product.setAllergic(true);
                    product.setNewAlert(true);
                    product.setDetectedAllergens(newAllergy.getDisplayName());

                    db.productDAO().update(product);
                    newAlertCount++;
                }
            }
        }

        // Informujemy użytkownika dyskretnym powiadomieniem Toast
        if (newAlertCount > 0) {
            Toast.makeText(context,
                    "Znaleziono " + newAlertCount + " produktów w historii zawierających: " + newAllergy.getDisplayName(),
                    Toast.LENGTH_LONG).show();
        }
    }
}