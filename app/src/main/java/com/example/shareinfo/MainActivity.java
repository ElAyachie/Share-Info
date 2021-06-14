package com.example.shareinfo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = getBaseContext();
        Button getInfoButton = findViewById(R.id.getInfoButton);

        getInfoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FetchData process = new FetchData(context);
                process.execute();
            }
        });
    }
}