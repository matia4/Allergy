package com.example.allergy;

import com.google.gson.annotations.SerializedName;

public class OffResponse {
    @SerializedName("status")
    private int status; // API status: 1 = found, 0 = product not found

    @SerializedName("product")
    private OffProduct product; // Mapped product data

    // Getters
    public int getStatus() { return status; }
    public OffProduct getProduct() { return product; }
}