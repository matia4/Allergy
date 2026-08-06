package com.example.allergy;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface AllergyDAO {
    @Query("SELECT * FROM allergies")
    List<Allergy> getAllAllergies();

    @Query("SELECT * FROM allergies WHERE isActive = 1")
    List<Allergy> getActiveAllergies();

    @Insert
    void insertAll(List<Allergy> allergies);

    @Update
    void update(Allergy allergy);

    @Query("SELECT COUNT(*) FROM allergies")
    int getCount();
}
