package com.example.allergy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class AllergiesFragment extends Fragment {
    private RecyclerView recyclerView;
    private AllergyTileAdapter adapter;
    private List<Allergy> allergyList;
    private AppDatabase db;
    private GridLayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.allergy_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getInstance(requireContext());
        recyclerView = view.findViewById(R.id.recyclerViewAllergies);
        SearchView searchView = view.findViewById(R.id.searchViewAllergy);

        // Ustawiamy Grid na 2 kolumny kafelków
        layoutManager = new GridLayoutManager(requireContext(), 2);
        recyclerView.setLayoutManager(layoutManager);

        loadAndSortAllergies();

        // 1. Logika kliknięcia w kafelek
        adapter = new AllergyTileAdapter(allergyList, (allergy, position) -> {
            // Odwracamy stan zaznaczenia
            allergy.setActive(!allergy.isActive());

            // Zapisujemy w bazie w tle
            new Thread(() -> db.allergyDAO().update(allergy)).start();

            // Sortujemy listę na nowo i odświeżamy widok
            sortAllergies();
            adapter.updateList(allergyList);

            // Przewijamy na samą górę, jeśli użytkownik zaznaczył nowy element,
            // żeby od razu widział go w "wybranych"
            if (allergy.isActive()) {
                recyclerView.scrollToPosition(0);
            }
        });
        recyclerView.setAdapter(adapter);

        // 2. Logika wyszukiwania (Scrollowanie do szukanej pozycji)
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) return false;

                String searchLower = newText.toLowerCase();
                for (int i = 0; i < allergyList.size(); i++) {
                    // Jeśli nazwa zawiera wpisaną frazę
                    if (allergyList.get(i).getDisplayName().toLowerCase().contains(searchLower)) {
                        // Scrollujemy listę tak, aby znaleziony element znalazł się na górze ekranu
                        layoutManager.scrollToPositionWithOffset(i, 0);
                        break; // Znaleźliśmy pierwszy pasujący, przerywamy pętlę
                    }
                }
                return true;
            }
        });
    }

    private void loadAndSortAllergies() {
        allergyList = db.allergyDAO().getAllAllergies(); // Pobieramy z bazy
        sortAllergies(); // Sortujemy
    }

    // Funkcja sortująca: Aktywne alergie lądują na samej górze, reszta alfabetycznie na dole
    private void sortAllergies() {
        Collections.sort(allergyList, (a1, a2) -> {
            // 1. Najpierw sortowanie po statusie aktywności
            if (a1.isActive() && !a2.isActive()) return -1; // a1 idzie wyżej
            if (!a1.isActive() && a2.isActive()) return 1;  // a2 idzie wyżej

            // 2. Jeśli oba są aktywne (lub oba nieaktywne), sortujemy alfabetycznie
            return a1.getDisplayName().compareToIgnoreCase(a2.getDisplayName());
        });
    }
}
