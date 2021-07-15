package com.example.shareinfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.FileUtils;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Setup {
    public static void CreateMainStockFolder(Context context) {
        String filePath = context.getFilesDir().getAbsolutePath();
        String folderName = "/stock_information";
        File file = new File(filePath + folderName);
        File folder = new File(filePath, folderName);
        boolean result = file.delete();
        boolean result2 = folder.mkdir();
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
        for (int i = 0; i < trendingSymbolsList.size(); i++) {
            CreateStockFolderLocation(context, trendingSymbolsList.get(i));
        }
    }

    public static void getAllStockData (Context context) {
        CreateMainStockFolder(context);
        CreateStockFolderLocations(context);
        String filePath = context.getFilesDir().getAbsolutePath();
        StockData.GetGeneralStockData(context);
        StockData.StoreTrendingStocks(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String trendingSymbols = preferences.getString("stockTrendingSymbols", "");
        String trendingNames = preferences.getString("stockTrendingNames", "");
        List<String> trendingSymbolsList = Arrays.asList(trendingSymbols.split(","));
        List<String> trendingNamesList = Arrays.asList(trendingNames.split(","));
        for (int i = 0; i < trendingSymbolsList.size(); i++) {
            String stockSymbol = trendingSymbolsList.get(i);
            String stockName = trendingNamesList.get(i);
            StockData.GetStockData(context, trendingSymbolsList.get(i));
            TwitterData.GetTwitterDataForStock(context, stockSymbol, stockName);
            NewsData.GetNewsDataForStock(context, stockSymbol, stockName);
            RedditData.GetRedditDataForStock(context, stockSymbol, stockName);
            String folderName = "/stock_information/" + trendingSymbolsList.get(i);
            File file = new File(filePath + folderName);
            boolean result = file.mkdir();
        }
    }
}
