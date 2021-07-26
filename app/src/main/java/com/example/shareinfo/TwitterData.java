package com.example.shareinfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TwitterData {
        public static void GetTwitterDataForStock(Context context, String stockSymbol, String stockName, String filePath) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    StringBuilder twitterSearchData = null;
                    StringBuilder twitterSearchData2 = null;
                    try {
                        // GET Twitter News information from API using the stock symbol.
                        String urlStockName = stockName.replaceAll(" ", "+");
                        String rules = "&max_results=100&tweet.fields=author_id,created_at,lang,referenced_tweets,public_metrics&expansions=author_id";
                        String url = "https://api.twitter.com/2/tweets/search/recent?query=" + stockSymbol + rules;
                        URL twitterStreamsUrl = new URL(url);
                        HttpURLConnection twitterSearchConnection = (HttpURLConnection) twitterStreamsUrl.openConnection();
                        String twitterToken = "AAAAAAAAAAAAAAAAAAAAANDdQgEAAAAAEZxD1p%2BpxRdfzYQBYAFnCfxgPhU%3D9uWLk6CFLfuZuFYXm4Bdzbd0IbOKFLclXueGwTEPPyT9N520iE";
                        twitterSearchConnection.setRequestProperty("Authorization", "Bearer " + twitterToken);
                        twitterSearchConnection.setRequestMethod("GET");
                        twitterSearchData = new StringBuilder();
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(twitterSearchConnection.getInputStream()))) {
                            String output;
                            while ((output = in.readLine()) != null) {
                                twitterSearchData.append(output);
                            }
                         twitterSearchConnection.disconnect();


                        } catch (Exception e) {
                            //throw new RuntimeException(e);
                        }

                        // GET Twitter News information from API using the stock name.
                        String url2 = "https://api.twitter.com/2/tweets/search/recent?query=" + urlStockName + rules;
                        URL twitterStreamsUrl2 = new URL(url2);
                        HttpURLConnection twitterSearchConnection2 = (HttpURLConnection) twitterStreamsUrl2.openConnection();
                        twitterSearchConnection2.setRequestProperty("Authorization", "Bearer " + twitterToken);
                        twitterSearchConnection2.setRequestMethod("GET");
                        twitterSearchData2 = new StringBuilder();
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(twitterSearchConnection2.getInputStream()))) {
                            String output;
                            while ((output = in.readLine()) != null) {
                                twitterSearchData2.append(output);
                            }
                        twitterSearchConnection.disconnect();
                        twitterSearchConnection2.disconnect();
                        } catch (Exception e) {
                            //throw new RuntimeException(e);
                        }
                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Save the JSON information to the file storage for specific stock.
                        // Saving Twitter data in a JSON file.
                        String extendedFilePath = filePath + "/" + stockSymbol + "/TwitterSearchData_" + stockSymbol + ".json";
                        assert twitterSearchData != null;
                        FileFunctions.CreateFile(context, extendedFilePath, twitterSearchData.toString());
                        // Saving Twitter data in a JSON file.
                        String extendedFilePath2 = filePath + "/" + stockSymbol + "/TwitterSearchData_" + stockSymbol + "2.json";
                        assert twitterSearchData2 != null;
                        FileFunctions.CreateFile(context, extendedFilePath2, twitterSearchData2.toString());

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

    // Will help us identify what author id is linked with which author name.
    public static String GetTwitterUsername(String id, JSONArray userJSONObject) {
        String username = "";
        for (int i = 0; i < userJSONObject.length(); i++) {
            try {
                String thisID = userJSONObject.getJSONObject(i).getString("id");
                if (thisID.equals(id)){
                    username = userJSONObject.getJSONObject(i).getString("username");
                    return username;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return username;
    }
}
