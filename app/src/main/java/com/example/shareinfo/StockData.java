package com.example.shareinfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StockData {
    public static void GetGeneralStockData(Context context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            String stockTrendingInfoData = "";
            @Override
            public void run() {

                try {
                    // GET Stock information from API
                    String url = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/market/get-trending-tickers?region=US";
                    URL stockTrendingInfoUrl = new URL(url);
                    HttpURLConnection stockTrendingInfoConnection = (HttpURLConnection) stockTrendingInfoUrl.openConnection();
                    stockTrendingInfoConnection.setRequestProperty("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com");
                    stockTrendingInfoConnection.setRequestProperty("x-rapidapi-key", "881aff6e0emsh3abb12509b21cabp16e33cjsn518f9f8df2b2");
                    stockTrendingInfoConnection.setRequestMethod("GET");
                    InputStream inputStream = stockTrendingInfoConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    stockTrendingInfoData = bufferedReader.readLine();
                    inputStream.close();
                    stockTrendingInfoConnection.disconnect();

                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }



                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // UI Thread work here
                        // Update the UI to know show the Symbols at the top.
                        // Update the UI to show the graph on the home page.
                        // Save the JSON information to the file storage
                        // Saving Stock information data in a JSON file
                        String extendedFilePath = "/stock_information/StockTrendingData.json";
                        FileFunctions.CreateFile(context, extendedFilePath, stockTrendingInfoData);
                        StoreTrendingStocks(context);
                        Setup.CreateStockFolderLocations(context);
                    }
                });
            }
        });
    }

    // Function requires context to load the stock information.
    // The function saves the specific stock information to a file.
    public static void GetStockData(Context context, String stockSymbol, String folderPath) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String stockInfoData = "";
                try {
                    // GET Stock information from API
                    String rules = "&region=US";
                    String url = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v2/get-summary?symbol=" + stockSymbol + rules;
                    URL stockInfoUrl = new URL(url);
                    HttpURLConnection stockInfoConnection = (HttpURLConnection) stockInfoUrl.openConnection();
                    stockInfoConnection.setRequestProperty("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com");
                    stockInfoConnection.setRequestProperty("x-rapidapi-key", "881aff6e0emsh3abb12509b21cabp16e33cjsn518f9f8df2b2");
                    stockInfoConnection.setRequestMethod("GET");
                    InputStream inputStream = stockInfoConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    stockInfoData = bufferedReader.readLine();
                    inputStream.close();
                    stockInfoConnection.disconnect();

                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Save the JSON information to the file storage
                try {
                    // Saving Stock information data in a JSON file
                    String filePath = context.getFilesDir().getAbsolutePath() + folderPath + "/" + stockSymbol;
                    String stockInfoJSONFileName = "StockData_" + stockSymbol + ".json";
                    File stockTrendingInfoDatafile = new File(filePath, stockInfoJSONFileName);
                    FileOutputStream stream = new FileOutputStream(stockTrendingInfoDatafile);
                    stream.write(stockInfoData.getBytes());
                    stream.write("\n".getBytes());
                    stream.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // UI Thread work here
                        // Update the UI to know show the Symbols at the top.
                        // Update the UI to show the graph on the home page.
                    }
                });
            }
        });
    }


    // Function requires context to load the stock information.
    // The function saves the trending stock information to shared preferences.
    public static void StoreTrendingStocks(Context context) {
        // Function will store the trending stocks names(short name and acronym for each stock) from the stock trending json file.
        try {
            // Creating the stock trending json object from the json file.
            String extendedFilePath = "/stock_information/StockTrendingData.json";
            JSONObject stockTrendingJson = FileFunctions.LoadJSONFile(context, extendedFilePath);
            // The stock trending file has an array that has 20 trending stocks, it is under the name "quotes".
            JSONArray stockTrendingJsonArray = stockTrendingJson.getJSONObject("finance").getJSONArray("result").getJSONObject(0).getJSONArray("quotes");
            // In order to store the stock trending names, shared preferences requires us to use a StringBuilder.
            StringBuilder stockTrendingNamesSB = new StringBuilder();
            StringBuilder stockTrendingSymbolsSB = new StringBuilder();
            // Loop through the json array and store each trend stock's short name and symbol(acronym).
            // Another array will store solely the trending stock's symbols(acronym).
            for (int i = 0; i < stockTrendingJsonArray.length(); i++) {
                JSONObject jsonObject = stockTrendingJsonArray.getJSONObject(i);
                String symbol = jsonObject.getString("symbol");
                String otherName = "";
                if (jsonObject.has("shortName")) {
                    otherName = jsonObject.getString("shortName");
                } else {
                    otherName = jsonObject.getString("longName");
                }
                stockTrendingNamesSB.append(otherName);
                stockTrendingSymbolsSB.append(symbol);
                // If there is another value after this add a comma.
                if (i + 1 < stockTrendingJsonArray.length()) {
                    if (stockTrendingNamesSB.charAt(stockTrendingNamesSB.length() - 1) != ',') {
                        stockTrendingNamesSB.append(",");
                    }
                    stockTrendingSymbolsSB.append(",");
                }
            }
            // Remove some inconsistencies.
            stockTrendingSymbolsSB = new StringBuilder(stockTrendingSymbolsSB.toString().replaceAll("\\^", ""));
            stockTrendingSymbolsSB = new StringBuilder(stockTrendingSymbolsSB.toString().replaceAll("-USD", ""));
            stockTrendingSymbolsSB = new StringBuilder(stockTrendingSymbolsSB.toString().replaceAll("-CAD", ""));
            // Make all of the names uppercase for simplicity.
            stockTrendingNamesSB = new StringBuilder(stockTrendingNamesSB.toString().toUpperCase());
            // Need to remove inconsistent names with the help of regex.
            stockTrendingNamesSB = new StringBuilder(stockTrendingNamesSB.toString().replaceAll(", ", ""));
            stockTrendingNamesSB = new StringBuilder(stockTrendingNamesSB.toString().replaceAll("\\.", ""));
            stockTrendingNamesSB = new StringBuilder(stockTrendingNamesSB.toString().replaceAll("INC", ""));
            stockTrendingNamesSB = new StringBuilder(stockTrendingNamesSB.toString().replaceAll(" INC\\.", ""));
            stockTrendingNamesSB = new StringBuilder(stockTrendingNamesSB.toString().replaceAll(" LTD\\.", ""));
            stockTrendingNamesSB = new StringBuilder(stockTrendingNamesSB.toString().replaceAll(" LTD", ""));
            stockTrendingNamesSB = new StringBuilder(stockTrendingNamesSB.toString().replaceAll(", IN", ""));
            stockTrendingNamesSB = new StringBuilder(stockTrendingNamesSB.toString().replaceAll(" USD", ""));
            stockTrendingNamesSB = new StringBuilder(stockTrendingNamesSB.toString().replaceAll(" CAD", ""));

            // SharedPreferences can not be stored as arrays, instead i am storing them in a StringBuilder which I will turn into an array.
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("stockTrendingNames", String.valueOf(stockTrendingNamesSB));
            editor.putString("stockTrendingSymbols", String.valueOf(stockTrendingSymbolsSB));
            editor.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

