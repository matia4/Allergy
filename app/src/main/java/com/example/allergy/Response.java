package com.example.allergy;

import com.google.gson.annotations.SerializedName;

public class Response {
    @SerializedName("status")
    private int status; // 1 = znaleziono, 0 = nie znaleziono produktu

    @SerializedName("product")
    private Request product;

    public int getStatus() { return status; }
    public Request getProduct() { return product; }
}