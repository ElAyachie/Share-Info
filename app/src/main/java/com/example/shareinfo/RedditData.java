package com.example.shareinfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

    public class RedditData {

        public static void GetRedditDataForStock(Context context, String stockSymbol, String stockName, String folderPath) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String redditSearchData = "";
                    try {
                        // GET Reddit information from API using stock name from comments.
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                        // Bring the date back one day.
                        Date date = new Date(System.currentTimeMillis()-24*60*60*1000);
                        String dateString = formatter.format(date);
                        String urlStockName = stockName.replaceAll(" ", "+");
                        String url = "https://socialgrep.p.rapidapi.com/search/comments?query=" + urlStockName + "%2Cafter%3A" + dateString;
                        URL redditSearchUrl = new URL(url);
                        HttpURLConnection redditSearchConnection = (HttpURLConnection) redditSearchUrl.openConnection();
                        redditSearchConnection.setRequestProperty("x-rapidapi-host", "socialgrep.p.rapidapi.com");
                        redditSearchConnection.setRequestProperty("x-rapidapi-key", "881aff6e0emsh3abb12509b21cabp16e33cjsn518f9f8df2b2");
                        redditSearchConnection.setRequestMethod("GET");
                        InputStream inputStream = redditSearchConnection.getInputStream();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        redditSearchData = bufferedReader.readLine();
                        inputStream.close();
                        redditSearchConnection.disconnect();

                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Save the JSON information to the file storage for a specific stock.
                    String extendedFilePath = folderPath + "/" + stockSymbol + "/RedditSearchData_" + stockSymbol + ".json";
                    FileFunctions.CreateFile(context, extendedFilePath, redditSearchData);

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
