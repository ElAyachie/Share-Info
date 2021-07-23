package com.example.shareinfo;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.robinhood.spark.SparkView;



public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
        //I added this if statement to keep the selected fragment when rotating the device
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainerView,
                    new Trending()).commit();
        }
    }
    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                switch (item.getItemId()) {
                    case R.id.trending:
                        selectedFragment = new Trending();
                        break;
                    case R.id.personalized:
                        selectedFragment = new Personalized();
                        break;
                    case R.id.search:
                        selectedFragment = new Search();
                        break;
                    case R.id.profile:
                        selectedFragment = new Profile();
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainerView,
                        selectedFragment).commit();
                return true;
            };
}