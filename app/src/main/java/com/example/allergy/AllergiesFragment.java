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

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for managing user allergies
 */
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

        // Set up Grid with 2 columns
        layoutManager = new GridLayoutManager(requireContext(), 2);
        recyclerView.setLayoutManager(layoutManager);

        loadAllergies();

        // Search logic (scroll to searched item)
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
                    // If name contains the search phrase
                    if (allergyList.get(i).getDisplayName().toLowerCase().contains(searchLower)) {
                        // Scroll list so the found item is at the top
                        layoutManager.scrollToPositionWithOffset(i, 0);
                        break; // Found first match, stop loop
                    }
                }
                return true;
            }
        });
    }

    private void loadAllergies() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Ensure default entries exist before loading
            ensureDefault14AllergiesExist();

            allergyList = db.allergyDAO().getAllAllergies();
            sortAllergies();
            
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (adapter == null) {
                        adapter = new AllergyTileAdapter(allergyList, (allergy, position) -> {
                            // Toggle selection status
                            allergy.setActive(!allergy.isActive());

                            // Save to database in background
                            AppDatabase.databaseWriteExecutor.execute(() -> db.allergyDAO().update(allergy));

                            // Re-sort list and refresh view
                            sortAllergies();
                            adapter.updateList(allergyList);

                            // Scroll to top if new item activated to show it in "selected" section
                            if (allergy.isActive()) {
                                recyclerView.scrollToPosition(0);
                            }
                        });
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.updateList(allergyList);
                    }
                });
            }
        });
    }

    private void ensureDefault14AllergiesExist() {
        List<Allergy> defaults = getDefaultAllergiesList();
        for (Allergy def : defaults) {
            Allergy existing = db.allergyDAO().getAllergyByOffTag(def.getOffTag());
            if (existing == null) {
                db.allergyDAO().insertIgnore(def);
            }
        }
    }

    private List<Allergy> getDefaultAllergiesList() {
        List<Allergy> list = new ArrayList<>();
        list.add(new Allergy(getString(R.string.allergy_gluten), "en:gluten", false));
        list.add(new Allergy(getString(R.string.allergy_milk), "en:milk", false));
        list.add(new Allergy(getString(R.string.allergy_eggs), "en:eggs", false));
        list.add(new Allergy(getString(R.string.allergy_nuts), "en:nuts", false));
        list.add(new Allergy(getString(R.string.allergy_peanuts), "en:peanuts", false));
        list.add(new Allergy(getString(R.string.allergy_soya), "en:soya", false));
        list.add(new Allergy(getString(R.string.allergy_fish), "en:fish", false));
        list.add(new Allergy(getString(R.string.allergy_crustaceans), "en:crustaceans", false));
        list.add(new Allergy(getString(R.string.allergy_molluscs), "en:molluscs", false));
        list.add(new Allergy(getString(R.string.allergy_celery), "en:celery", false));
        list.add(new Allergy(getString(R.string.allergy_mustard), "en:mustard", false));
        list.add(new Allergy(getString(R.string.allergy_sesame), "en:sesame-seeds", false));
        list.add(new Allergy(getString(R.string.allergy_sulphites), "en:sulphur-dioxide-and-sulphites", false));
        list.add(new Allergy(getString(R.string.allergy_lupin), "en:lupin", false));
        list.add(new Allergy("custom:tomatoes", "Pomidory", false));
        list.add(new Allergy("custom:cocoa", "Kakao / Czekolada", false));
        list.add(new Allergy("custom:citrus", "Owoce cytrusowe", false));
        list.add(new Allergy("custom:corn", "Kukurydza", false));
        list.add(new Allergy("custom:yeast", "Drożdże", false));
        list.add(new Allergy("custom:honey", "Miód", false));
        list.add(new Allergy("custom:carmine", "Barwnik E120 (Karmin)", false));
        list.add(new Allergy("custom:palmoil", "Olej palmowy", false));
        return list;
    }

    // Sorting: Active allergies at the top, rest alphabetically
    private void sortAllergies() {
        if (allergyList == null) return;
        
        allergyList.sort((a1, a2) -> {
            // 1. Sort by activity status
            if (a1.isActive() && !a2.isActive()) return -1;
            if (!a1.isActive() && a2.isActive()) return 1;

            // 2. Alphabetical sort for same activity status
            return a1.getDisplayName().compareToIgnoreCase(a2.getDisplayName());
        });
    }
}
