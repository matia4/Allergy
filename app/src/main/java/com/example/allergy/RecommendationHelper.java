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

/**
 * Helper class for finding safe product alternatives based on categories
 */
public class RecommendationHelper {
    private static final String TAG = "RecommendationHelper";

    /**
     * Main method calling hierarchical search for alternatives.
     * Works for both new scans and history.
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

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Allergy> activeAllergies = AppDatabase.getInstance(context).allergyDAO().getActiveAllergies();
            
            ((android.app.Activity)context).runOnUiThread(() -> 
                performSearchFallback(context, currentBarcode, categoriesTags, categoriesTags.size() - 1,
                        activeAllergies, pbLoading, tvNoRecs, tvCurrentCategory, rvRecs, new ArrayList<>())
            );
        });
    }

    /**
     * Recursive search function with fallback.
     * Starts from the most specific category and moves to broader ones until at least 3 safe alternatives are found.
     * categoryIndex: index in categoriesTags list currently being checked.
     * accumulatedProducts: list of safe products found in previous steps.
     */
    private static void performSearchFallback(Context context, String barcode, List<String> categoriesTags,
                                              int categoryIndex, List<Allergy> activeAllergies,
                                              ProgressBar pbLoading, TextView tvNoRecs,
                                              TextView tvCurrentCategory, RecyclerView rvRecs,
                                              List<OffProduct> accumulatedProducts) {

        final int MIN_REQUIRED_PRODUCTS = 4;

        // Base case: all categories exhausted OR we reached the threshold
        if (categoryIndex < 0 || accumulatedProducts.size() >= MIN_REQUIRED_PRODUCTS) {
            pbLoading.setVisibility(View.GONE);
            if (accumulatedProducts.isEmpty()) {
                tvNoRecs.setText(R.string.no_alternatives_found);
                tvNoRecs.setVisibility(View.VISIBLE);
                tvCurrentCategory.setVisibility(View.GONE);
            } else {
                displayRecommendations(context, rvRecs, accumulatedProducts);
            }
            return;
        }

        String targetTag = categoriesTags.get(categoryIndex);
        Log.d(TAG, "Searching for alternatives in category: " + targetTag + " (Found so far: " + accumulatedProducts.size() + ")");

        // UI update for current search status
        String categoryReadable = targetTag.replace("en:", "").replace("-", " ");
        tvCurrentCategory.setText(context.getString(R.string.category_label, categoryReadable, categoriesTags.size() - categoryIndex, categoriesTags.size()));
        tvCurrentCategory.setVisibility(View.VISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        rvRecs.setVisibility(View.GONE);
        tvNoRecs.setVisibility(View.GONE);

        // API Query limited to Poland to ensure local availability
        Client.getApiService().searchProductsByCategory("categories", "contains", targetTag, "countries", "contains", "en:poland")
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<OffSearch> call, @NonNull Response<OffSearch> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getProducts() != null) {
                            List<OffProduct> rawProducts = response.body().getProducts();

                            // Filter results to ensure they are safe for the user's active allergies
                            List<OffProduct> safeProducts = filterSafeProducts(rawProducts, barcode, activeAllergies);

                            // Merge unique products into accumulation list
                            for (OffProduct newP : safeProducts) {
                                boolean exists = false;
                                for (OffProduct accP : accumulatedProducts) {
                                    if (Objects.equals(accP.getCode(), newP.getCode())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    accumulatedProducts.add(newP);
                                }
                            }

                            if (accumulatedProducts.size() < MIN_REQUIRED_PRODUCTS && categoryIndex > 0) {
                                // Threshold not met: try search in the next broader category
                                Log.d(TAG, "Threshold not met (" + accumulatedProducts.size() + "/" + MIN_REQUIRED_PRODUCTS + "). Trying broader category.");
                                performSearchFallback(context, barcode, categoriesTags, categoryIndex - 1,
                                        activeAllergies, pbLoading, tvNoRecs, tvCurrentCategory, rvRecs, accumulatedProducts);
                            } else {
                                // Threshold met or no more categories
                                pbLoading.setVisibility(View.GONE);
                                if (accumulatedProducts.isEmpty()) {
                                    tvNoRecs.setText(R.string.no_alternatives_found);
                                    tvNoRecs.setVisibility(View.VISIBLE);
                                } else {
                                    displayRecommendations(context, rvRecs, accumulatedProducts);
                                }
                            }
                        } else {
                            // Fallback on non-successful API response
                            performSearchFallback(context, barcode, categoriesTags, categoryIndex - 1,
                                    activeAllergies, pbLoading, tvNoRecs, tvCurrentCategory, rvRecs, accumulatedProducts);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OffSearch> call, @NonNull Throwable t) {
                        // On failure, if we have some results, show them. Otherwise show error.
                        if (!accumulatedProducts.isEmpty()) {
                            pbLoading.setVisibility(View.GONE);
                            displayRecommendations(context, rvRecs, accumulatedProducts);
                        } else {
                            pbLoading.setVisibility(View.GONE);
                            tvNoRecs.setText(R.string.error_network);
                            tvNoRecs.setVisibility(View.VISIBLE);
                        }
                        Log.e(TAG, "Network error in search fallback: " + t.getMessage());
                    }
                });
    }

    private static void displayRecommendations(Context context, RecyclerView rvRecs, List<OffProduct> products) {
        rvRecs.setVisibility(View.VISIBLE);
        rvRecs.setAdapter(new RecommendationAdapter(products, selectedProduct ->
                new AlertDialog.Builder(context)
                        .setTitle(selectedProduct.getProductName())
                        .setMessage(context.getString(R.string.ingredients_full_title,
                                selectedProduct.getIngredientsText() != null ? selectedProduct.getIngredientsText() : context.getString(R.string.unknown_composition)))
                        .setPositiveButton(R.string.close, null)
                        .show()
        ));
    }

    /**
     * Filters a list of products based on safety criteria.
     * Excludes the current product, unnamed products, and those containing active allergens.
     */
    private static List<OffProduct> filterSafeProducts(List<OffProduct> rawProducts, String currentBarcode, List<Allergy> activeAllergies) {
        List<OffProduct> safeProducts = new ArrayList<>();
        for (OffProduct p : rawProducts) {
            // 1. Skip current product
            if (Objects.equals(p.getCode(), currentBarcode)) continue;

            // 2. Skip unnamed products
            if (p.getProductName() == null || p.getProductName().trim().isEmpty()) continue;

            // 3. Skip products without ingredients
            if (p.getIngredientsText() == null || p.getIngredientsText().trim().isEmpty()) continue;

            // 4. Verify distribution in Poland
            if (!isSoldInPoland(p)) continue;

            if (p.getIngredientsText().trim().length() < 20) continue;

            // 5. Verify allergen safety
            if (isProductSafe(p, activeAllergies)) {
                safeProducts.add(p);
            }
        }
        return safeProducts;
    }

    private static boolean isProductSafe(OffProduct p, List<Allergy> activeAllergies) {
        for (Allergy allergy : activeAllergies) {
            boolean hasTag = p.getAllergensTags() != null && p.getAllergensTags().contains(allergy.getOffTag());
            boolean hasText = hasAllergenInText(p.getIngredientsText(), allergy);

            if (hasTag || hasText) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verify if product has a distribution tag for Poland
     */
    private static boolean isSoldInPoland(OffProduct p) {
        // Check country tags (e.g., "en:poland", "poland")
        if (p.getCountriesTags() != null && !p.getCountriesTags().isEmpty()) {
            for (String tag : p.getCountriesTags()) {
                if ("en:poland".equalsIgnoreCase(tag) || "poland".equalsIgnoreCase(tag) || "polska".equalsIgnoreCase(tag)) {
                    return true;
                }
            }
        }

        // Fallback check in countries string
        if (p.getCountries() != null && !p.getCountries().isEmpty()) {
            String lower = p.getCountries().toLowerCase();
            return lower.contains("poland") || lower.contains("polska") || lower.contains("pl");
        }

        // If API returned result with "en:poland" filter but details are empty, assume allowed
        return p.getCountriesTags() == null && p.getCountries() == null;
    }

    private static boolean hasAllergenInText(String ingredientsText, Allergy allergy) {
        if (ingredientsText == null || allergy == null) return false;

        String textLower = ingredientsText.toLowerCase();

        switch (allergy.getOffTag()) {
            case "en:gluten":
                return textLower.contains("pszen") || textLower.contains("gluten") ||
                        textLower.contains("żyto") || textLower.contains("jęczmień") ||
                        textLower.contains("owies") || textLower.contains("orkisz") || textLower.contains("wheat");

            case "en:milk":
                return textLower.contains("mleko") || textLower.contains("laktoz") ||
                        textLower.contains("serwatk") || textLower.contains("masło") ||
                        textLower.contains("śmietan") || textLower.contains("kazein") || textLower.contains("milk");

            case "en:eggs":
                return textLower.contains("jaj") || textLower.contains("żółtko") ||
                        textLower.contains("białko jaja") || textLower.contains("egg");

            case "en:nuts":
                return textLower.contains("orzech") || textLower.contains("migdał") ||
                        textLower.contains("laskow") || textLower.contains("włosk") ||
                        textLower.contains("nerkowiec") || textLower.contains("pistac") || textLower.contains("hazelnut");

            case "en:peanuts":
                return textLower.contains("arachid") || textLower.contains("fistaszk") ||
                        textLower.contains("orzechy ziemne") || textLower.contains("peanut");

            case "en:soya":
                return textLower.contains("soj") || textLower.contains("soy");

            case "en:fish":
                return textLower.contains("ryb") || textLower.contains("łosoś") ||
                        textLower.contains("dorsz") || textLower.contains("tuńczyk") || textLower.contains("fish");

            case "en:crustaceans":
                return textLower.contains("krewetk") || textLower.contains("krab") ||
                        textLower.contains("homar") || textLower.contains("rak") || textLower.contains("shrimp");

            case "en:molluscs":
                return textLower.contains("małże") || textLower.contains("ostryg") ||
                        textLower.contains("ośmiornic") || textLower.contains("kalmar");

            case "en:celery":
                return textLower.contains("seler") || textLower.contains("celery");

            case "en:mustard":
                return textLower.contains("gorczyc") || textLower.contains("musztard") || textLower.contains("mustard");

            case "en:sesame-seeds":
                return textLower.contains("sezam") || textLower.contains("sesame");

            case "en:sulphur-dioxide-and-sulphites":
                return textLower.contains("siarczyn") || textLower.contains("dwutlenek siarki") ||
                        textLower.contains("e220") || textLower.contains("e224") || textLower.contains("e228");

            case "en:lupin":
                return textLower.contains("łubin") || textLower.contains("lupin");

            case "custom:tomatoes":
                return textLower.contains("pomidor") || textLower.contains("likopen") || textLower.contains("tomato");

            case "custom:cocoa":
                return textLower.contains("kakao") || textLower.contains("kakaow") || textLower.contains("cocoa");

            case "custom:citrus":
                return textLower.contains("cytryn") || textLower.contains("pomarańcz") ||
                        textLower.contains("mandaryn") || textLower.contains("limonk") || textLower.contains("citrus");

            case "custom:corn":
                return textLower.contains("kukurydz") || textLower.contains("corn");

            case "custom:yeast":
                return textLower.contains("drożdż") || textLower.contains("yeast");

            case "custom:carmine":
                return textLower.contains("e120") || textLower.contains("koszenil") ||
                        textLower.contains("karmin") || textLower.contains("cochineal");

            case "custom:palmoil":
                return textLower.contains("palmow") || textLower.contains("palm oil");

            default:
                if (allergy.getDisplayName() != null) {
                    return textLower.contains(allergy.getDisplayName().toLowerCase());
                }
                return false;
        }
    }
}
