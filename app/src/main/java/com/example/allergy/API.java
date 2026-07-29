package com.example.allergy;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface API {
    // Zapytanie do darmowego API v2 Open Food Facts
    @GET("api/v2/product/{barcode}.json")
    Call<Response> getProductByBarcode(@Path("barcode") String barcode);
}
