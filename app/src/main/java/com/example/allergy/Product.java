package com.example.allergy;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class Product {
    @PrimaryKey
    @NonNull
    private String barcode; // Unique barcode ID
    private String name;    // Product name
    private String ingredients; // Full list of ingredients
    private String allergensTagsJson; // API tags stored as text for simple persistence
    private String categoriesTagsJson; // Category tags from API
    private long lastUpdated;         // Timestamp of the last scan/fetch
    private boolean isAllergic;       // Result of allergen analysis
    private boolean isNewAlert;       // Flag indicating a retroactive alert
    private String detectedAllergens; // User-friendly names of detected allergens
    private String imageUrl; // URL to the product image

    public Product(@NonNull String barcode, String name, String ingredients,
                   String allergensTagsJson, String categoriesTagsJson, long lastUpdated,
                   boolean isAllergic, boolean isNewAlert, String detectedAllergens, String imageUrl) {
        this.barcode = barcode;
        this.name = name;
        this.ingredients = ingredients;
        this.allergensTagsJson = allergensTagsJson;
        this.categoriesTagsJson = categoriesTagsJson;
        this.lastUpdated = lastUpdated;
        this.isAllergic = isAllergic;
        this.isNewAlert = isNewAlert;
        this.detectedAllergens = detectedAllergens;
        this.imageUrl = imageUrl;
    }

    // Getters and Setters
    @NonNull public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public String getIngredients() { return ingredients; }
    public String getAllergensTagsJson() { return allergensTagsJson; }
    public String getCategoriesTagsJson() { return categoriesTagsJson; }
    public long getLastUpdated() { return lastUpdated; }
    public boolean isAllergic() { return isAllergic; }
    public void setAllergic(boolean allergic) { isAllergic = allergic; }
    public boolean isNewAlert() { return isNewAlert; }
    public void setNewAlert(boolean newAlert) { isNewAlert = newAlert; }
    public String getDetectedAllergens() { return detectedAllergens; }
    public void setDetectedAllergens(String detectedAllergens) { this.detectedAllergens = detectedAllergens; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}