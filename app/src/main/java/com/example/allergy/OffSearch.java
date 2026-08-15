package com.example.allergy;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OffSearch {
    @SerializedName("count")
    private int count;

    @SerializedName("products")
    private List<OffProduct> products;

    public int getCount() { return count; }
    public List<OffProduct> getProducts() { return products; }
}