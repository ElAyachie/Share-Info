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

public class TwitterData {
        public static void GetTwitterDataForStock(Context context, String stockSymbol, String stockName) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    StringBuilder twitterSearchData = null;
                    StringBuilder twitterSearchData2 = null;
                    try {
                        // GET Twitter News information from API using the stock symobol.
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
                        String url2 = "https://api.twitter.com/2/tweets/search/recent?query=" + stockName + rules;
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

                    // Save the JSON information to the file storage for specifc stock.
                    try {
                        // Saving Twitter data in a JSON file.
                        String filePath = context.getFilesDir().getAbsolutePath() + "/stock_information/" + stockSymbol;
                        String twitterJSONFileName = "TwitterSearchData_" + stockSymbol + ".json";
                        File twitterSearchDataFile = new File(filePath, twitterJSONFileName);
                        FileOutputStream stream = new FileOutputStream(twitterSearchDataFile);
                        assert twitterSearchData != null;
                        stream.write(twitterSearchData.toString().getBytes());
                        stream.write("\n".getBytes());
                        stream.close();
                        // Saving Twitter data in a JSON file.
                        String twitterJSONFileName2 = "TwitterSearchData_" + stockSymbol + "2.json";
                        twitterSearchDataFile = new File(filePath, twitterJSONFileName2);
                        stream = new FileOutputStream(twitterSearchDataFile);
                        assert twitterSearchData2 != null;
                        stream.write(twitterSearchData2.toString().getBytes());
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
