package com.example.shareinfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TwitterData {
        public static void GetTwitterDataForStock(Context context, String searchString) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    StringBuilder twitterSearchData = null;
                    String socialTrendingInfoData;
                    try {
                        // GET Twitter News information from API
                        String rules = "&max_results=10&tweet.fields=author_id,created_at,lang,referenced_tweets,public_metrics&expansions=author_id";
                        String url = "https://api.twitter.com/2/tweets/search/recent?query=" + searchString + rules;
                        URL twitterStreamsUrl = new URL(url);
                        HttpURLConnection twitterSearchConnection = (HttpURLConnection) twitterStreamsUrl.openConnection();
                        String twitterToken = "AAAAAAAAAAAAAAAAAAAAANDdQgEAAAAAEZxD1p%2BpxRdfzYQBYAFnCfxgPhU%3D9uWLk6CFLfuZuFYXm4Bdzbd0IbOKFLclXueGwTEPPyT9N520iE";
                        twitterSearchConnection.setRequestProperty("Authorization", "Bearer " + twitterToken);
                        twitterSearchConnection.setRequestMethod("GET");
                        twitterSearchData = new StringBuilder();
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(twitterSearchConnection.getInputStream()))) {
                            String output;
                            while ((output = in.readLine()) != null) {
                                System.out.println(output);
                                twitterSearchData.append(output);
                            }
                        } catch (Exception e) {
                            //throw new RuntimeException(e);
                        }

                    /*
                    // Trying alternative
                    URL socialTrendingInfoUrl = new URL("https://socialsentiment-io.p.rapidapi.com/stocks/" + searchString + "/posts/");
                    HttpURLConnection socialTrendingInfoConnection = (HttpURLConnection) socialTrendingInfoUrl.openConnection();
                    socialTrendingInfoConnection.setRequestProperty("x-rapidapi-host", "socialsentiment-io.p.rapidapi.com");
                    socialTrendingInfoConnection.setRequestProperty("x-rapidapi-key", "132a0cb470mshf7b87dbc92557f1p1cf34bjsna1ca8152da1a");
                    socialTrendingInfoConnection.setRequestMethod("GET");
                    InputStream inputStream = socialTrendingInfoConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    socialTrendingInfoData = bufferedReader.readLine();
                    Log.d("Social Stock JSON Data", socialTrendingInfoData);
                    */
                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Save the JSON information to the file storage for specifc stock.
                    try {
                        // Saving Google News data in a JSON file
                        String filePath = context.getFilesDir().getAbsolutePath();
                        String twitterJSONFileName = "TwitterSearchData_" + searchString + ".json";
                        File twitterSearchDataFile = new File(filePath, twitterJSONFileName);
                        FileOutputStream stream = new FileOutputStream(twitterSearchDataFile);
                        assert twitterSearchData != null;
                        stream.write(twitterSearchData.toString().getBytes());
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
