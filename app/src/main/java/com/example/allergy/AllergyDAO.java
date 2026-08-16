package com.example.allergy;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * Data Access Object for allergies table
 */
@Dao
public interface AllergyDAO {
    //Retrieve all allergies from the database
    @Query("SELECT * FROM allergies")
    List<Allergy> getAllAllergies();

    //Retrieve only active (enabled) allergies
    @Query("SELECT * FROM allergies WHERE isActive = 1")
    List<Allergy> getActiveAllergies();

    //Bulk insert allergies into the database
    @Insert
    void insertAll(List<Allergy> allergies);

    // Insert a single allergy, ignoring if it already exists (based on PrimaryKey)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIgnore(Allergy allergy);

     //Update an existing allergy's status
    @Update
    void update(Allergy allergy);

    //Get the total count of allergy entries
    @Query("SELECT COUNT(*) FROM allergies")
    int getCount();

    // Find an allergy by its Open Food Facts tag
    @Query("SELECT * FROM allergies WHERE offTag = :offTag LIMIT 1")
    Allergy getAllergyByOffTag(String offTag);
}
