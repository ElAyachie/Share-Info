package com.example.shareinfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

    public class RedditData {

        public static void GetRedditDataForStock(Context context, String searchString) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String redditSearchData = "";
                    try {
                        // GET Reddit information from API
                        String url = "https://socialgrep.p.rapidapi.com/search/posts?query=" + searchString +" %2C%2Fr%2Fwallstreetbets";
                        URL stockTrendingInfoUrl = new URL(url);
                        HttpURLConnection stockTrendingInfoConnection = (HttpURLConnection) stockTrendingInfoUrl.openConnection();
                        stockTrendingInfoConnection.setRequestProperty("x-rapidapi-host", "socialgrep.p.rapidapi.com");
                        stockTrendingInfoConnection.setRequestProperty("x-rapidapi-key", "132a0cb470mshf7b87dbc92557f1p1cf34bjsna1ca8152da1a");
                        stockTrendingInfoConnection.setRequestMethod("GET");
                        InputStream inputStream = stockTrendingInfoConnection.getInputStream();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        redditSearchData = bufferedReader.readLine();
                        Log.d("Reddit JSON Data", redditSearchData);

                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Save the JSON information to the file storage for a specific stock.
                    try {
                        // Saving Google News data in a JSON file
                        String filePath = context.getFilesDir().getAbsolutePath();
                        String redditJSONFileName = "RedditSearchData_" + searchString + ".json";
                        File redditSearchDataFile = new File(filePath, redditJSONFileName);
                        FileOutputStream stream = new FileOutputStream(redditSearchDataFile);
                        stream.write(redditSearchData.getBytes());
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
