package com.example.allergy;

import android.os.Bundle;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private AppDatabase db;

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

        loadHistory();

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // Nie obsługujemy zmiany kolejności (drag & drop)
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (adapter == null) return;
                
                Product productToDelete = adapter.getProductAt(position);

                // 1. Usuwamy lokalnie z listy w adapterze, żeby UI zareagowało natychmiast
                adapter.removeProductAt(position);

                // 2. Usuwamy z bazy danych Room w osobnym wątku
                Executors.newSingleThreadExecutor().execute(() -> {
                    db.productDAO().deleteProduct(productToDelete);
                });

                // 3. Pokazujemy powiadomienie Snackbar z opcją "Cofnij"
                Snackbar.make(recyclerView, R.string.product_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, v -> {
                            // Przywracamy produkt w tle
                            Executors.newSingleThreadExecutor().execute(() -> {
                                db.productDAO().insertOrUpdate(productToDelete);

                                // Odświeżamy listę na wątku UI
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() -> {
                                        adapter.addProductAt(position, productToDelete);
                                    });
                                }
                            });
                        })
                        .show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void loadHistory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Product> products = db.productDAO().getAllProducts();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter = new HistoryAdapter(products, product -> {
                        Context context = getContext();
                        if (context == null) return;

                        // Po kliknięciu usuwamy wykrzyknik "nowy alert" i zapisujemy stan
                        if (product.isNewAlert()) {
                            product.setNewAlert(false);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                db.productDAO().update(product);
                                loadHistory(); // Odświeżamy listę
                            });
                        }

                        // Wyświetlamy szczegóły składu
                        new androidx.appcompat.app.AlertDialog.Builder(context)
                                .setTitle(product.getName())
                                .setMessage(getString(R.string.ingredients_title) + product.getIngredients())
                                .setPositiveButton(R.string.close, null)
                                .show();
                    });
                    recyclerView.setAdapter(adapter);
                });
            }
        });
    }
}