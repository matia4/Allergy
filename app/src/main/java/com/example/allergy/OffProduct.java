package com.example.allergy;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OffProduct {
    @SerializedName("code")
    private String code; // Product barcode

    @SerializedName("product_name")
    private String productName; // Product name

    @SerializedName("ingredients_text")
    private String ingredientsText; // Full ingredients list text

    @SerializedName("allergens_tags")
    private List<String> allergensTags; // List of allergen tags from OFF

    @SerializedName("categories_tags")
    private List<String> categoriesTags; // List of category tags from OFF

    @SerializedName("image_front_url")
    private String imageUrl; // URL to product image

    @SerializedName("countries_tags")
    private List<String> countriesTags; // List of country tags from OFF

    @SerializedName("countries")
    private String countries; // Raw countries string

    @SerializedName("states_tags")
    private List<String> statesTags; // Product completion states

    // Getters
    public String getCode() { return code; }
    public String getProductName() { return productName != null ? productName : "Unknown product"; }
    public String getIngredientsText() { return ingredientsText != null ? ingredientsText : "Composition data missing"; }
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

    //Checks if the product is available in Poland based on tags or country string
    public boolean isSoldInPoland() {
        // Check by country tags (e.g., "en:poland", "poland")
        if (countriesTags != null) {
            for (String tag : countriesTags) {
                if (tag.equalsIgnoreCase("en:poland") ||
                        tag.equalsIgnoreCase("poland") ||
                        tag.equalsIgnoreCase("polska")) {
                    return true;
                }
            }
        }

        // Fallback check of country string
        if (countries != null && !countries.isEmpty()) {
            String lower = countries.toLowerCase();
            return lower.contains("poland") || lower.contains("polska") || lower.contains("pl");
        }

        return false;
    }
}