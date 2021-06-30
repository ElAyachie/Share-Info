package com.example.shareinfo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = getBaseContext();
        Button getInfoButton = findViewById(R.id.getInfoButton);


        getInfoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String stockSymbol = "TSLA";
                StockData.GetGeneralStockData(context);
                StockData.GetStockData(context, stockSymbol);
                GoogleNewsData.GetGoogleNewsDataForStock(context, stockSymbol);
                TwitterData.GetTwitterDataForStock(context, stockSymbol);
                RedditData.GetRedditDataForStock(context, stockSymbol);
            }
        });
    }


}