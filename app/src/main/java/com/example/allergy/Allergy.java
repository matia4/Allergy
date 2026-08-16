package com.example.allergy;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing an allergy type
 */
@Entity(tableName = "allergies", indices = {@androidx.room.Index(value = {"offTag"}, unique = true)})
public class Allergy {
    @PrimaryKey(autoGenerate = true)
    public int id; // Unique ID for database

    public String displayName; // Human-readable name of the allergy
    public String offTag;      // Open Food Facts tag for this allergen
    public boolean isActive;   // Whether the user has this allergy enabled

    public Allergy(String displayName, String offTag, boolean isActive) {
        this.displayName = displayName;
        this.offTag = offTag;
        this.isActive = isActive;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDisplayName() { return displayName; }
    public String getOffTag() { return offTag; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}