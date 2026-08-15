package com.example.allergy;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class Product {
    @PrimaryKey
    @NonNull
    private String barcode;
    private String name;
    private String ingredients;
    private String allergensTagsJson; // Przechowujemy tagi z API jako ciąg tekstowy
    private String categoriesTagsJson; // Przechowujemy tagi kategorii z API
    private long lastUpdated;         // Timestamp w milisekundach
    private boolean isAllergic;       // Czy zawiera alergen użytkownika
    private boolean isNewAlert;       // Flaga retroaktywnego alertu
    private String detectedAllergens; // Sformatowana nazwa wykrytych alergii
    private String imageUrl;

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