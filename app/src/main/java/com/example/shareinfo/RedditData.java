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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

    public class RedditData {

        public static void GetRedditDataForStock(Context context, String stockSymbol, String stockName) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String redditSearchData = "";
                    String redditSearchData2 = "";
                    try {
                        // GET Reddit information from API using stock symbol.
                        String subReddit = "wallstreetbets";
                        String afterDate = "2020-07-09";
                        String score = "4";
                        //"
                        String url = "https://socialgrep.p.rapidapi.com/search/comments?query=%2Fr%2F"+ subReddit + "%2C"+ stockSymbol + "%2Cscore%3A"+ score + "%2Cafter%3A" + afterDate;
                        URL redditSearchUrl = new URL(url);
                        HttpURLConnection redditSearchConnection = (HttpURLConnection) redditSearchUrl.openConnection();
                        redditSearchConnection.setRequestProperty("x-rapidapi-host", "socialgrep.p.rapidapi.com");
                        redditSearchConnection.setRequestProperty("x-rapidapi-key", "132a0cb470mshf7b87dbc92557f1p1cf34bjsna1ca8152da1a");
                        redditSearchConnection.setRequestMethod("GET");
                        InputStream inputStream = redditSearchConnection.getInputStream();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        redditSearchData = bufferedReader.readLine();

                        // GET Reddit information from API using stock name.
                        String url2 = "https://socialgrep.p.rapidapi.com/search/posts?query=%2Fr%2F"+ subReddit +"%2C"+ stockName +"%2Cscore%3A8%2Cafter%3A" + afterDate;
                        URL redditSearchUrl2 = new URL(url2);
                        HttpURLConnection redditSearchConnection2 = (HttpURLConnection) redditSearchUrl2.openConnection();
                        redditSearchConnection2.setRequestProperty("x-rapidapi-host", "socialgrep.p.rapidapi.com");
                        redditSearchConnection2.setRequestProperty("x-rapidapi-key", "132a0cb470mshf7b87dbc92557f1p1cf34bjsna1ca8152da1a");
                        redditSearchConnection2.setRequestMethod("GET");
                        inputStream = redditSearchConnection2.getInputStream();
                        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        redditSearchData2 = bufferedReader.readLine();
                        Log.d("Reddit Data", "Reddit data received.");

                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Save the JSON information to the file storage for a specific stock.
                    try {
                        // Saving Reddit data in a JSON file for stock symbol query.
                        String filePath = context.getFilesDir().getAbsolutePath() + "/stock_information";
                        String redditJSONFileName = "RedditSearchData_" + stockSymbol + ".json";
                        File redditSearchDataFile = new File(filePath, redditJSONFileName);
                        FileOutputStream stream = new FileOutputStream(redditSearchDataFile);
                        stream.write(redditSearchData.getBytes());
                        stream.write("\n".getBytes());
                        stream.close();

                        // Saving Reddit data in a JSON file for stock name query.
                        String redditJSONFileName2 = "RedditSearchData_" + stockSymbol + "2.json";
                        File redditSearchDataFile2 = new File(filePath, redditJSONFileName2);
                        stream = new FileOutputStream(redditSearchDataFile2);
                        stream.write(redditSearchData2.getBytes());
                        stream.write("\n".getBytes());
                        stream.close();
                        Log.d("Reddit Data", "Reddit data saved.");

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
