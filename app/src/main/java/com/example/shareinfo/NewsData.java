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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsData {
    public static void GetNewsDataForStock(Context context, String stockSymbol, String stockName, String folderPath) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String newsData = "";
                try {
                    // GET News information from API using the stock symbol.
                    // Remove spaces in the name of the stock so that the api works.
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    Date date = new Date();
                    String dateString = formatter.format(date);
                    String urlStockName = stockName.replaceAll(" ", "+");
                    String newsAPIKey = "0c2e1e9d0c9fabaa36b80de7b59d84c5";
                    String newsURL = "http://api.mediastack.com/v1/news?access_key=" + newsAPIKey + "&keywords=" + urlStockName + "&languages=en&date=" + dateString + "&limit=50";
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
                    // Path to were to save the data file.
                    String extendedFilePath = folderPath + "/" + stockSymbol + "/NewsData_" + stockSymbol + ".json";
                    // Check if their is information from the API.
                    if (!newsData.equals("")) {
                        FileFunctions.CreateFile(context, extendedFilePath, newsData);
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
