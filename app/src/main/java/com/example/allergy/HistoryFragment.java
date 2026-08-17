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

import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment displaying the history of scanned products
 */
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

        // Configure swipe-to-delete functionality for history items
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (adapter == null) return;
                
                Product productToDelete = adapter.getProductAt(position);

                // 1. Remove locally from adapter list for immediate UI response
                adapter.removeProductAt(position);

                // 2. Remove from Room database in background using the shared executor
                AppDatabase.databaseWriteExecutor.execute(() -> db.productDAO().deleteProduct(productToDelete));

                // 3. Show Snackbar with "Undo" option to allow restoring the deleted product
                Snackbar.make(recyclerView, R.string.product_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, v -> {
                            // Restore product in database
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                db.productDAO().insertOrUpdate(productToDelete);

                                // Restore locally in adapter on the UI thread
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() -> adapter.addProductAt(position, productToDelete));
                                }
                            });
                        })
                        .show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    /**
     * Loads the scan history from the database asynchronously.
     */
    private void loadHistory() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Product> products = db.productDAO().getAllProducts();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter = new HistoryAdapter(products, product -> {
                        Context context = getContext();
                        if (context == null) return;

                        // Click removes "new alert" status and saves state
                        if (product.isNewAlert()) {
                            product.setNewAlert(false);
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                db.productDAO().update(product);
                                loadHistory(); // Refresh list
                            });
                        }

                        // Display product analysis dialog instead of simple AlertDialog
                        showProductDetailsDialog(context, product);
                    });
                    recyclerView.setAdapter(adapter);
                });
            }
        });
    }

    /**
     * Shows a detailed dialog for a product from history, similar to the scanner dialog.
     */
    private void showProductDetailsDialog(Context context, Product product) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        String displayTitle = (product.getName() != null && !product.getName().trim().isEmpty()) ? product.getName() : getString(R.string.unknown_product);
        builder.setTitle(displayTitle);

        StringBuilder msg = new StringBuilder();

        // Inflate custom dialog layout to show product image
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_product_result, null);
        ImageView ivProduct = dialogView.findViewById(R.id.ivDialogProductImage);
        
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(this).load(product.getImageUrl()).into(ivProduct);
        } else {
            ivProduct.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        builder.setView(dialogView);

        // Check if data is old (1 year TTL)
        long ttlDuration = 365L * 24 * 60 * 60 * 1000L;
        if ((System.currentTimeMillis() - product.getLastUpdated()) > ttlDuration) {
            msg.append(getString(R.string.outdated_warning));
        }

        // Show detected allergens above ingredients as requested
        if (product.isAllergic()) {
            msg.append(getString(R.string.detected_allergens_warning, product.getDetectedAllergens().replace(", ", "\n- ")));
            builder.setIcon(android.R.drawable.ic_dialog_alert);

            // Set up "Safe Alternative" button
            List<String> categories = new ArrayList<>();
            if (product.getCategoriesTagsJson() != null && !product.getCategoriesTagsJson().isEmpty()) {
                try {
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<String>>(){}.getType();
                    categories = new com.google.gson.Gson().fromJson(product.getCategoriesTagsJson(), listType);
                } catch (Exception ignored) {}
            }
            final List<String> finalCategories = categories;
            builder.setNeutralButton(R.string.safe_alternative, (dialog, which) ->
                    RecommendationHelper.showHierarchicalRecommendationsDialog(context, product.getBarcode(), finalCategories, null)
            );
        } else {
            msg.append(getString(R.string.product_safe));
            builder.setIcon(android.R.drawable.ic_dialog_info);
        }

        String displayIngredients = (product.getIngredients() != null && !product.getIngredients().trim().isEmpty()) ? product.getIngredients() : getString(R.string.composition_data_missing);
        msg.append("\n\n").append(getString(R.string.ingredients_title)).append(displayIngredients);
        builder.setMessage(msg.toString());

        builder.setPositiveButton(R.string.close, null);
        builder.show();
    }
}