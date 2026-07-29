package com.example.allergy;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "allergies")
public class Allergy {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String displayName;
    public String offTag;
    public boolean isActive;

    public Allergy(String displayName, String offTag, boolean isActive) {
        this.displayName = displayName;
        this.offTag = offTag;
        this.isActive = isActive;
    }
}