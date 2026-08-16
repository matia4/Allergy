package com.example.allergy;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface API {
    //Fetch product details from Open Food Facts by barcode
    @GET("api/v0/product/{barcode}.json")
    Call<OffResponse> getProductByBarcode(@Path("barcode") String barcode);

    //Search products by category or phrase
    @GET("cgi/search.pl?action=process&json=1&page_size=30")
    Call<OffSearch> searchProductsByCategory(
            @Query("tagtype_0") String tagType,          // e.g., "categories"
            @Query("tag_contains_0") String tagContains, // e.g., "contains"
            @Query("tag_0") String categoryTag,          // e.g., "en:biscuits"
            @Query("tagtype_1") String tagType1,         // "countries"
            @Query("tag_contains_1") String tagContains1, // "contains"
            @Query("tag_1") String country               // "en:poland" or "poland"
    );
}