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
        Setup.CreateFolders(context);


        getInfoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String stockSymbol = "TSLA";
                String stockName = "Tesla";
                //StockData.GetGeneralStockData(context);
                //StockData.GetStockData(context, stockSymbol);
                //GoogleNewsData.GetGoogleNewsDataForStock(context, stockSymbol, stockName);
                //StockData.StoreTrendingStocks(context);
                //TwitterData.GetTwitterDataForStock(context, stockSymbol, stockName);
                //RedditData.GetRedditDataForStock(context, stockSymbol, stockName);
                CombineData.CombineAndSaveData(context, stockSymbol);
            }
        });
    }


}