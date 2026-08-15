package com.example.allergy;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface API {
    @GET("api/v0/product/{barcode}.json")
    Call<OffResponse> getProductByBarcode(@Path("barcode") String barcode);

    // Wyszukiwanie produktów po kategorii lub frazie
    @GET("cgi/search.pl?action=process&json=1&page_size=30")
    Call<OffSearch> searchProductsByCategory(
            @Query("tagtype_0") String tagType,       // np. "categories"
            @Query("tag_contains_0") String tagContains, // np. "contains"
            @Query("tag_0") String categoryTag  ,       // np. "en:biscuits"
            @Query("tagtype_1") String tagType1,     // "countries"
            @Query("tag_contains_1") String tagContains1, // "contains"
            @Query("tag_1") String country          // "en:poland" lub "poland"
    );
}