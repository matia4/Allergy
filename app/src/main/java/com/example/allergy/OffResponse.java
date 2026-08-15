package com.example.allergy;

import com.google.gson.annotations.SerializedName;

public class OffResponse {
    @SerializedName("status")
    private int status; // 1 = znaleziono, 0 = nie znaleziono produktu

    @SerializedName("product")
    private OffProduct product;

    public int getStatus() { return status; }
    public OffProduct getProduct() { return product; }
}