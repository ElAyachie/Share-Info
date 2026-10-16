package com.example.shareinfo;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.shareinfo.ui.ThemePrefs;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePrefs.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);
        navController = navHost.getNavController();
        // The four tabs are top level: no back arrow, and each keeps its own back stack.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.trending, R.id.personalized, R.id.search, R.id.profile).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController((BottomNavigationView) findViewById(R.id.bottomNavigation), navController);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    /** Switches bottom navigation tabs from inside a screen. */
    public static void selectTab(Activity activity, int tabId) {
        BottomNavigationView bottomNavigation = activity.findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(tabId);
        }
    }
}
