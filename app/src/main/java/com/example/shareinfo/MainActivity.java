package com.example.shareinfo;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.os.Bundle;


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.robinhood.spark.SparkView;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = getBaseContext();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
        // Make sure to leave this commented out unless you want to use up API calls (which are limited).
        //Setup.CreateFavoritesFolder(context);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = preferences.edit();
        prefEditor.putBoolean("trendingExists", false);
        prefEditor.putBoolean("personalizedExists", false);
        prefEditor.putBoolean("searchExists", false);
        prefEditor.putBoolean("profileExists", false);
        prefEditor.apply();

        // I added this if statement to keep the selected fragment when rotating the device
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainerView,
                    new Trending(), "trending").commit();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor prefEditor = preferences.edit();
                Fragment selectedFragment = null;
                switch (item.getItemId()) {
                    case R.id.trending:
                        selectedFragment = new Trending();
                        prefEditor.putBoolean("trendingExists", true);
                        prefEditor.apply();
                        break;
                    case R.id.personalized:
                        selectedFragment = new Personalized();
                        prefEditor.putBoolean("personalizedExists", true);
                        prefEditor.apply();
                        break;
                    case R.id.search:
                        selectedFragment = new Search();
                        prefEditor.putBoolean("searchExists", true);
                        prefEditor.apply();
                        break;
                    case R.id.profile:
                        selectedFragment = new Profile();
                        prefEditor.putBoolean("profileExists", true);
                        prefEditor.apply();
                        break;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainerView,
                        selectedFragment).commit();
                return true;
            };

    public static void RefreshTrending(FragmentManager fm) {
        fm.beginTransaction().replace(R.id.fragmentContainerView,
                new Trending(), "trending").commit();
    }

    public static void RefreshPersonalized(FragmentManager fm) {
        fm.beginTransaction().replace(R.id.fragmentContainerView,
                new Personalized(), "personalized").commit();
    }
}