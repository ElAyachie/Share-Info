package com.example.shareinfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleNewsData {
    public static void GetGoogleNewsDataForStock(Context context, String stockSymbol, String stockName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String googleNewsData = "";
                String googleNewsData2 = "";
                try {
                    // GET Google News information from API using the stock symbol.
                    String rules = "&country=US&lang=en";
                    String url = "https://google-news1.p.rapidapi.com/search?q=" + stockSymbol + rules;
                    URL googleNewsUrl = new URL(url);
                    HttpURLConnection googleNewsConnection = (HttpURLConnection) googleNewsUrl.openConnection();
                    googleNewsConnection.setRequestProperty("x-rapidapi-host", "google-news1.p.rapidapi.com");
                    googleNewsConnection.setRequestProperty("x-rapidapi-key", "132a0cb470mshf7b87dbc92557f1p1cf34bjsna1ca8152da1a");
                    googleNewsConnection.setRequestMethod("GET");
                    InputStream inputStream = googleNewsConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    googleNewsData = bufferedReader.readLine();
                    Log.d("Google News JSON Data", googleNewsData);

                    // GET Google News information from API using the stock name.
                    String url2 = "https://google-news1.p.rapidapi.com/search?q=" + stockName + rules;
                    URL googleNewsUrl2 = new URL(url2);
                    HttpURLConnection googleNewsConnection2 = (HttpURLConnection) googleNewsUrl2.openConnection();
                    googleNewsConnection2.setRequestProperty("x-rapidapi-host", "google-news1.p.rapidapi.com");
                    googleNewsConnection2.setRequestProperty("x-rapidapi-key", "132a0cb470mshf7b87dbc92557f1p1cf34bjsna1ca8152da1a");
                    googleNewsConnection2.setRequestMethod("GET");
                    inputStream = googleNewsConnection2.getInputStream();
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    googleNewsData2 = bufferedReader.readLine();
                    inputStream.close();

                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Save the JSON information to the file storage.
                try {
                    // Saving Google News data in a JSON file for stock symbol query.
                    String filePath = context.getFilesDir().getAbsolutePath() + "/stock_information";
                    String googleNewsJSONFileName = "GoogleNewsData_" + stockSymbol + ".json";
                    File googleNewsDataFile = new File(filePath, googleNewsJSONFileName);
                    FileOutputStream stream = new FileOutputStream(googleNewsDataFile);
                    stream.write(googleNewsData.getBytes());
                    stream.write("\n".getBytes());
                    stream.close();

                    // Saving Google News data in a JSON file for stock name query.
                    String googleNewsJSONFileName2 = "GoogleNewsData_" + stockSymbol + "2.json";
                    File googleNewsDataFile2 = new File(filePath, googleNewsJSONFileName2);
                    stream = new FileOutputStream(googleNewsDataFile2);
                    stream.write(googleNewsData2.getBytes());
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
                        // Update the social feed panel to show the stock information.
                    }
                });
            }
        });
    }
}
