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
    @Query("SELECT * FROM products ORDER BY lastUpdated DESC")
    List<Product> getAllProducts();

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    Product getProductByBarcode(String barcode);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(Product product);

    @Update
    void updateAll(List<Product> products);

    @Update
    void update(Product product);

    @Delete
    void deleteProduct(Product product);

    // 2. Alternatywa: Usuwanie po kodzie kreskowym
    @Query("DELETE FROM products WHERE barcode = :barcode")
    void deleteByBarcode(String barcode);

    // 3. OPCJONALNIE: Wyczyszczenie całej historii
    @Query("DELETE FROM products")
    void deleteAllProducts();
}