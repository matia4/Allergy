package com.example.allergy;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * API response for product search
 */
public class OffSearch {
    @SerializedName("count")
    private int count; // Total number of products matching the search

    @SerializedName("products")
    private List<OffProduct> products; // List of search results

    // Getters
    public int getCount() { return count; }
    public List<OffProduct> getProducts() { return products; }
}