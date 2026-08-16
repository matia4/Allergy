package com.example.allergy;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ProductDAO {
    //Retrieve all historical scans, sorted by most recent first
    @Query("SELECT * FROM products ORDER BY lastUpdated DESC")
    List<Product> getAllProducts();

    //Find a specific product in the cache by barcode
    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    Product getProductByBarcode(String barcode);

    //Cache a new product or update existing entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(Product product);

    //Update a single product's state
    @Update
    void update(Product product);

    //Remove a product from history
    @Delete
    void deleteProduct(Product product);
}