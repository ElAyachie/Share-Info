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

public class NewsData {
    public static void GetNewsDataForStock(Context context, String stockSymbol, String stockName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String newsData = "";
                try {
                    // GET News information from API using the stock symbol.
                    String newsAPIKey = "73cbab5c6c58002b78c8ec6dd2c4c688";
                    String newsURL = "http://api.mediastack.com/v1/news?access_key=" + newsAPIKey + "&keywords=" + stockName + "&languages=en&limit=50";
                    URL newsDataStreamsUrl = new URL(newsURL);
                    HttpURLConnection newsDataConnection = (HttpURLConnection) newsDataStreamsUrl.openConnection();
                    newsDataConnection.setRequestMethod("GET");
                    InputStream inputStream = newsDataConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    newsData = bufferedReader.readLine();
                    inputStream.close();
                    newsDataConnection.disconnect();

                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Save the JSON information to the file storage.
                try {
                    // Path to were to save the data file.
                    String filePath = context.getFilesDir().getAbsolutePath() + "/stock_information/" + stockSymbol;
                    // Check if their is information from the API.
                    assert newsData != null;
                    if (!newsData.toString().equals("")) {
                        // Saving Google News data in a JSON file for stock symbol query.
                        String newsJSONFileName = "NewsData_" + stockSymbol + ".json";
                        File newsDataFile = new File(filePath, newsJSONFileName);
                        FileOutputStream stream = new FileOutputStream(newsDataFile);
                        stream.write(newsData.toString().getBytes());
                        stream.write("\n".getBytes());
                        stream.close();
                    }

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
