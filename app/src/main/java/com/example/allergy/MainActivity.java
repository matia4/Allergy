package com.example.allergy;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.allergy.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // 1. Ustawienie domyślnego ekranu przy uruchomieniu (Skaner)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ScannerFragment())
                    .commit();
        }

        // 2. Obsługa kliknięć w dolny pasek menu
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }

            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int id = item.getItemId();

                if (id == R.id.nav_scanner) {
                    selectedFragment = new ScannerFragment();
                } else if (id == R.id.nav_history) {
                    // W przyszłości: selectedFragment = new HistoryFragment();
                    // Na potrzeby testu stworzymy pusty tymczasowy fragment
                    selectedFragment = DummyFragment.newInstance("Ekran Historii");
                } else if (id == R.id.nav_allergies) {
                    // W przyszłości: selectedFragment = new AllergiesFragment();
                    selectedFragment = DummyFragment.newInstance("Ekran Zarzadzania Alergiami");
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, selectedFragment)
                            .commit();
                    return true;
                }
                return false;
            }
        });
    }
}