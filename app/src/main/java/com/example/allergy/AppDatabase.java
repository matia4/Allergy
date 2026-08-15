package com.example.allergy;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Allergy.class, Product.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase{
    public abstract AllergyDAO allergyDAO();
    public abstract ProductDAO productDAO();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "allergy_food_db")
                            .fallbackToDestructiveMigration(true)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

