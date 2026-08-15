package com.example.allergy;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecommendationHelper {
    private static final String TAG = "RecommendationHelper";

    /**
     * Główna metoda wywołująca hierarchiczne wyszukiwanie alternatyw.
     * Działa dla skanowania (new/cached) i historii.
     */
    public static void showHierarchicalRecommendationsDialog(Context context, String currentBarcode, List<String> categoriesTags, Runnable onDismissListener) {
        if (categoriesTags == null || categoriesTags.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.no_alternatives_title)
                    .setMessage(R.string.no_categories_error)
                    .setPositiveButton(R.string.close, (dialog, which) -> {
                        if (onDismissListener != null) onDismissListener.run();
                    })
                    .show();
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.recommendations, null);
        ProgressBar pbLoading = dialogView.findViewById(R.id.pbLoadingRecs);
        TextView tvNoRecs = dialogView.findViewById(R.id.tvNoRecs);
        TextView tvCurrentCategory = dialogView.findViewById(R.id.tvCurrentCategoryDisplay);
        RecyclerView rvRecs = dialogView.findViewById(R.id.rvRecommendations);
        rvRecs.setLayoutManager(new LinearLayoutManager(context));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(R.string.close, (d, w) -> {
                    if (onDismissListener != null) onDismissListener.run();
                })
                .setOnCancelListener(d -> {
                    if (onDismissListener != null) onDismissListener.run();
                })
                .create();

        dialog.show();

        AppDatabase db = AppDatabase.getInstance(context);
        List<Allergy> activeAllergies = db.allergyDAO().getActiveAllergies();

        performSearchFallback(context, currentBarcode, categoriesTags, categoriesTags.size() - 1,
                activeAllergies, pbLoading, tvNoRecs, tvCurrentCategory, rvRecs);
    }

    /**
     * Rekurencyjna funkcja wyszukiwania z fallbackiem.
     * categoryIndex: indeks w liście categoriesTags, który aktualnie sprawdzamy.
     */
    private static void performSearchFallback(Context context, String barcode, List<String> categoriesTags,
                                              int categoryIndex, List<Allergy> activeAllergies,
                                              ProgressBar pbLoading, TextView tvNoRecs,
                                              TextView tvCurrentCategory, RecyclerView rvRecs) {

        if (categoryIndex < 0) {
            pbLoading.setVisibility(View.GONE);
            tvNoRecs.setText(R.string.no_alternatives_found);
            tvNoRecs.setVisibility(View.VISIBLE);
            tvCurrentCategory.setVisibility(View.GONE);
            return;
        }

        String targetTag = categoriesTags.get(categoryIndex);
        Log.d(TAG, "Searching for alternatives in category: " + targetTag);

        String categoryReadable = targetTag.replace("en:", "").replace("-", " ");
        tvCurrentCategory.setText(context.getString(R.string.category_label, categoryReadable, categoryIndex + 1, categoriesTags.size()));
        tvCurrentCategory.setVisibility(View.VISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        rvRecs.setVisibility(View.GONE);
        tvNoRecs.setVisibility(View.GONE);

        // Zapytanie do API z ograniczeniem do Polski
        Client.getApiService().searchProductsByCategory("categories", "contains", targetTag, "countries", "contains", "en:poland")
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<OffSearch> call, @NonNull Response<OffSearch> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getProducts() != null) {
                            List<OffProduct> rawProducts = response.body().getProducts();

                            // Podwójny filtr: bezpieczeństwo alergenów ORAZ weryfikacja dystrybucji w Polsce
                            List<OffProduct> safeProducts = filterSafeProducts(rawProducts, barcode, activeAllergies);

                            if (safeProducts.isEmpty()) {
                                Log.d(TAG, "No safe products in " + targetTag + ". Trying broader category.");
                                performSearchFallback(context, barcode, categoriesTags, categoryIndex - 1,
                                        activeAllergies, pbLoading, tvNoRecs, tvCurrentCategory, rvRecs);
                            } else {
                                pbLoading.setVisibility(View.GONE);
                                rvRecs.setVisibility(View.VISIBLE);
                                rvRecs.setAdapter(new RecommendationAdapter(safeProducts, selectedProduct ->
                                        new AlertDialog.Builder(context)
                                                .setTitle(selectedProduct.getProductName())
                                                .setMessage(context.getString(R.string.ingredients_full_title,
                                                        selectedProduct.getIngredientsText() != null ? selectedProduct.getIngredientsText() : context.getString(R.string.unknown_composition)))
                                                .setPositiveButton(R.string.close, null)
                                                .show()
                                ));
                            }
                        } else {
                            performSearchFallback(context, barcode, categoriesTags, categoryIndex - 1,
                                    activeAllergies, pbLoading, tvNoRecs, tvCurrentCategory, rvRecs);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OffSearch> call, @NonNull Throwable t) {
                        pbLoading.setVisibility(View.GONE);
                        tvNoRecs.setText(R.string.error_network);
                        tvNoRecs.setVisibility(View.VISIBLE);
                        Log.e(TAG, "Network error in search fallback: " + t.getMessage());
                    }
                });
    }

    private static List<OffProduct> filterSafeProducts(List<OffProduct> rawProducts, String currentBarcode, List<Allergy> activeAllergies) {
        List<OffProduct> safeProducts = new ArrayList<>();
        for (OffProduct p : rawProducts) {
            // 1. Odrzucamy aktualnie skanowany produkt
            if (Objects.equals(p.getCode(), currentBarcode)) continue;

            // 2. Odrzucamy produkty bez nazwy
            if (p.getProductName() == null || p.getProductName().trim().isEmpty()) continue;

            // 3. Odrzucamy produkty, które NIE MAJĄ podanego składu
            if (p.getIngredientsText() == null || p.getIngredientsText().trim().isEmpty()) continue;

            // 4. Sprawdzamy czy produkt jest sprzedawany w Polsce
            if (!isSoldInPoland(p)) continue;

            if (p.getIngredientsText().trim().length() < 20) continue;

            // 5. Sprawdzamy bezpieczeństwo pod kątem alergenów
            boolean isSafe = true;
            for (Allergy allergy : activeAllergies) {
                boolean hasTag = p.getAllergensTags() != null && p.getAllergensTags().contains(allergy.getOffTag());
                boolean hasText = hasAllergenInText(p.getIngredientsText(), allergy);

                if (hasTag || hasText) {
                    isSafe = false;
                    break;
                }
            }

            if (isSafe) {
                safeProducts.add(p);
            }
        }
        return safeProducts;
    }

    /**
     * Pomocnicza weryfikacja czy produkt posiada tag dystrybucji w Polsce
     */
    private static boolean isSoldInPoland(OffProduct p) {
        // Sprawdzenie po listach tagów krajów (np. "en:poland", "poland")
        if (p.getCountriesTags() != null && !p.getCountriesTags().isEmpty()) {
            for (String tag : p.getCountriesTags()) {
                if ("en:poland".equalsIgnoreCase(tag) || "poland".equalsIgnoreCase(tag) || "polska".equalsIgnoreCase(tag)) {
                    return true;
                }
            }
        }

        // Zapasowe sprawdzenie ciągu tekstowego w polu countries
        if (p.getCountries() != null && !p.getCountries().isEmpty()) {
            String lower = p.getCountries().toLowerCase();
            return lower.contains("poland") || lower.contains("polska") || lower.contains("pl");
        }

        // Jeśli API wygenerowało wynik przy filtrze "en:poland", ale dane szczegółowe są puste, traktujemy jako dopuszczony
        return p.getCountriesTags() == null && p.getCountries() == null;
    }

    private static boolean hasAllergenInText(String ingredientsText, Allergy allergy) {
        if (ingredientsText == null || allergy == null) return false;

        String textLower = ingredientsText.toLowerCase();

        switch (allergy.getOffTag()) {
            case "en:milk":
                return textLower.contains("mleko") || textLower.contains("laktoz") ||
                        textLower.contains("serwatk") || textLower.contains("masło") || textLower.contains("milk");
            case "en:gluten":
                return textLower.contains("pszen") || textLower.contains("gluten") ||
                        textLower.contains("żyto") || textLower.contains("jęczmień") || textLower.contains("wheat");
            case "en:nuts":
                return textLower.contains("orzech") || textLower.contains("migdał") || textLower.contains("hazelnut");
            case "en:eggs":
                return textLower.contains("jaj") || textLower.contains("egg");
            case "en:soya":
                return textLower.contains("soj") || textLower.contains("soy");
            default:
                if (allergy.getDisplayName() != null) {
                    return textLower.contains(allergy.getDisplayName().toLowerCase());
                }
                return false;
        }
    }
}