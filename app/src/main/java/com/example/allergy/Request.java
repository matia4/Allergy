package com.example.allergy;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Request {
    @SerializedName("product_name")
    private String productName;

    @SerializedName("ingredients_text")
    private String ingredientsText;

    @SerializedName("allergens_tags")
    private List<String> allergensTags;

    @SerializedName("image_front_url")
    private String imageUrl;

    // Gettery
    public String getProductName() { return productName != null ? productName : "Nieznany produkt"; }
    public String getIngredientsText() { return ingredientsText != null ? ingredientsText : "Brak danych o składzie"; }
    public List<String> getAllergensTags() { return allergensTags; }
    public String getImageUrl() { return imageUrl; }
}