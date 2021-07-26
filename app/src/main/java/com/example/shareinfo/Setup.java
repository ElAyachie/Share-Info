package com.example.shareinfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Setup {
    public static void CreateMainStockFolder (Context context) {
        String filePath = context.getFilesDir().getAbsolutePath();
        String folderName = "/stock_information";
        File folder = new File(filePath, folderName);
        FileFunctions.DeleteFolder(context, folderName);
        boolean result = folder.mkdir();
    }

    public static void CreateFavoritesFolder (Context context) {
        String filePath = context.getFilesDir().getAbsolutePath();
        String folderName = "/favorites_stock_information";
        File folder = new File(filePath, folderName);
        if (!folder.exists()) {
            boolean result = folder.mkdir();
        }
    }

    public static void CreateStockFolderLocation (Context context, String stockSymbol) {
        String filePath = context.getFilesDir().getAbsolutePath();
        String folderName = "/stock_information/" + stockSymbol;
        File file = new File(filePath + folderName);
        boolean result = file.mkdir();
    }

    public static void CreateStockFolderLocations (Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String trendingSymbols = preferences.getString("stockTrendingSymbols", "");
        List<String> trendingSymbolsList = Arrays.asList(trendingSymbols.split(","));
        for (int i = 0; i < 5; i++) {
            CreateStockFolderLocation(context, trendingSymbolsList.get(i));
        }
    }

    // Gets all stock data from the APIs and stores them in the designated folders.
    public static void GetAllStockData (Context context) {
        CreateMainStockFolder(context);
        CreateStockFolderLocations(context);
        String filePath = context.getFilesDir().getAbsolutePath();
        StockData.GetGeneralStockData(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String trendingSymbols = preferences.getString("stockTrendingSymbols", "");
        String trendingNames = preferences.getString("stockTrendingNames", "");
        List<String> trendingSymbolsList = Arrays.asList(trendingSymbols.split(","));
        List<String> trendingNamesList = Arrays.asList(trendingNames.split(","));

        // Wait to receive the trending stocks list so that you can collect information for each stock.
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String folderPath = "/stock_information";
                for (int i = 0; i < 5; i++) {
                    String stockSymbol = trendingSymbolsList.get(i);
                    String stockName = trendingNamesList.get(i);
                    String folderName = "/stock_information/" + trendingSymbolsList.get(i);
                    File file = new File(filePath + folderName);
                    boolean result = file.mkdir();
                    StockData.GetStockData(context, stockSymbol, folderPath);
                    TwitterData.GetTwitterDataForStock(context, stockSymbol, stockName, folderPath);
                    NewsData.GetNewsDataForStock(context, stockSymbol, stockName, folderPath);
                    RedditData.GetRedditDataForStock(context, stockSymbol, stockName, folderPath);
                }
            }
        }, 100);

        // Wait for all the information from the API to be saved and then combine all the values into one json file.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String folderPath = "/stock_information";
                for (int i = 0; i < 5; i++) {
                    String stockSymbol = trendingSymbolsList.get(i);
                    CombineData.CombineAndSaveData(context, stockSymbol, folderPath);
                }
            }
        }, 8000);

    }

    // Gets all stock data from the APIs and stores them in the designated folders.
    public static void GetAllStockDataForFavorites (Context context) {
        String filePath = context.getFilesDir().getAbsolutePath();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String favoriteStocksSymbols = preferences.getString("favoriteStocksSymbols", "");
        String favoriteStocksNames = preferences.getString("favoriteStocksNames", "");
        List<String> favoriteSymbolsList = Arrays.asList(favoriteStocksSymbols.split(","));
        List<String> favoriteNamesList = Arrays.asList(favoriteStocksNames.split(","));

        // Wait to receive the trending stocks list so that you can collect information for each stock.
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String folderPath = "/favorite_stock_information";
                for (int i = 0; i < favoriteSymbolsList.size(); i++) {
                    String stockSymbol = favoriteSymbolsList.get(i);
                    String stockName = favoriteNamesList.get(i);
                    String folderName = "/favorite_stock_information/" + favoriteSymbolsList.get(i);
                    File file = new File(filePath + folderName);
                    boolean result = file.mkdir();
                    StockData.GetStockData(context, stockSymbol, folderPath);
                    TwitterData.GetTwitterDataForStock(context, stockSymbol, stockName, folderPath);
                    NewsData.GetNewsDataForStock(context, stockSymbol, stockName, folderPath);
                    RedditData.GetRedditDataForStock(context, stockSymbol, stockName, folderPath);
                }
            }
        }, 100);

        // Wait for all the information from the API to be saved and then combine all the values into one json file.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String folderPath = "/stock_information";
                for (int i = 0; i < favoriteSymbolsList.size(); i++) {
                    String stockSymbol = favoriteSymbolsList.get(i);
                    CombineData.CombineAndSaveData(context, stockSymbol, folderPath);
                }
            }
        }, 8000);
    }


    public static void AddToFavorites(Context context, String stockSymbol, String stockName) {
        // Check if the stock is not a duplicate.
        CreateFavoritesFolder(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = preferences.edit();
        String favoriteStocksSymbols = preferences.getString("favoriteStocksSymbols", "");
        String favoriteStocksNames = preferences.getString("favoriteStocksNames", "");
        if (favoriteStocksSymbols.contains(stockSymbol)) {
            Toast toast = Toast.makeText(context, "Stock already in favorites.", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        String filePath = context.getFilesDir().getAbsolutePath();
        String folderPath = "/favorites_stock_information/";
        String folderName = folderPath + stockSymbol;
        File file = new File(filePath + folderName);
        boolean result = file.mkdir();

        TwitterData.GetTwitterDataForStock(context, stockSymbol, stockName, folderPath);
        NewsData.GetNewsDataForStock(context, stockSymbol, stockName, folderPath);
        RedditData.GetRedditDataForStock(context, stockSymbol, stockName, folderPath);
        StockData.GetStockData(context, stockSymbol, folderPath);
        if (favoriteStocksSymbols.length() > 0) {
            favoriteStocksSymbols += ",";
            favoriteStocksNames += ",";
        }
        favoriteStocksSymbols += stockSymbol;
        favoriteStocksNames += stockName;
        prefEditor.putString("favoriteStocksSymbols", favoriteStocksSymbols);
        prefEditor.putString("favoriteStocksNames", favoriteStocksNames);
        prefEditor.apply();
        System.out.println("favorite stock names: " + favoriteStocksNames);
        System.out.println("favorite stock symbols: " + favoriteStocksSymbols);
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                CombineData.CombineAndSaveData(context, stockSymbol, folderPath);
                Toast toast = Toast.makeText(context, "Stock Saved to Favorites!", Toast.LENGTH_SHORT);
                toast.show();
            }
        }, 1000);
    }

    public static void RemoveFromFavorites(Context context, String stockSymbol, String stockName) {
        // Check if the stock is not a duplicate.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = preferences.edit();
        String favoriteStocksSymbols = preferences.getString("favoriteStocksSymbols", "");
        String favoriteStocksNames = preferences.getString("favoriteStocksNames", "");
        String filePath = context.getFilesDir().getAbsolutePath();
        String folderPath = "/favorites_stock_information/";
        String folderName = folderPath + stockSymbol;
        File file = new File(filePath + folderName);
        FileFunctions.DeleteFolder(context, folderPath + stockSymbol);
        favoriteStocksSymbols = favoriteStocksSymbols.replace("," + stockSymbol, "");
        favoriteStocksNames = favoriteStocksNames.replace("," + stockName, "");
        favoriteStocksNames = favoriteStocksNames.replace(stockName, "");
        favoriteStocksSymbols = favoriteStocksSymbols.replace(stockSymbol, "");
        prefEditor.putString("favoriteStocksSymbols", favoriteStocksSymbols);
        prefEditor.putString("favoriteStocksNames", favoriteStocksNames);
        prefEditor.apply();
        System.out.println("favorite stock names: " + favoriteStocksNames);
        System.out.println("favorite stock symbols: " + favoriteStocksSymbols);
        Toast toast = Toast.makeText(context, "Stock unfollowed.", Toast.LENGTH_SHORT);
        toast.show();
    }
}
