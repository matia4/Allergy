package com.example.allergy;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OffProduct {
    @SerializedName("code")
    private String code;

    @SerializedName("product_name")
    private String productName;

    @SerializedName("ingredients_text")
    private String ingredientsText;

    @SerializedName("allergens_tags")
    private List<String> allergensTags;

    @SerializedName("categories_tags")
    private List<String> categoriesTags;

    @SerializedName("image_front_url")
    private String imageUrl;

    @SerializedName("countries_tags")
    private List<String> countriesTags;

    @SerializedName("countries")
    private String countries;

    @SerializedName("states_tags")
    private List<String> statesTags;

    public String getCode() { return code; }
    public String getProductName() { return productName != null ? productName : "Nieznany produkt"; }
    public String getIngredientsText() { return ingredientsText != null ? ingredientsText : "Brak danych o składzie"; }
    public List<String> getAllergensTags() { return allergensTags; }
    public List<String> getCategoriesTags() { return categoriesTags; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getCountriesTags() {
        return countriesTags;
    }
    public String getCountries() {
        return countries;
    }
    public List<String> getStatesTags() { return statesTags; }

    public boolean isSoldInPoland() {
        // Sprawdzenie po tagach krajów (np. "en:poland", "poland")
        if (countriesTags != null) {
            for (String tag : countriesTags) {
                if (tag.equalsIgnoreCase("en:poland") ||
                        tag.equalsIgnoreCase("poland") ||
                        tag.equalsIgnoreCase("polska")) {
                    return true;
                }
            }
        }

        // Zapasowe sprawdzenie ciągu tekstowego
        if (countries != null && !countries.isEmpty()) {
            String lower = countries.toLowerCase();
            return lower.contains("poland") || lower.contains("polska") || lower.contains("pl");
        }

        return false;
    }
}