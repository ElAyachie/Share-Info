package com.example.shareinfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.Random;

import static java.lang.Math.abs;

public class CombineData {

    public static void CombineAndSaveData(Context context, String stockSymbol, String folderPath) {
        try {
            // Creating the json object for combined news and social media.
            JSONObject combinedStockData = new JSONObject();
            JSONArray stockDataArray = new JSONArray();
            int id = 0;

            // Creating the twitter json object from the json file.
            String extendedFilePathTwitter = folderPath + "/" + stockSymbol + "/TwitterSearchData_" + stockSymbol + ".json";
            JSONObject twitterJSONData1 = FileFunctions.LoadJSONFile(context, extendedFilePathTwitter);

            // Creating the twitter trending json object from the json file.
            String extendedFilePathTwitter2 = folderPath + "/" + stockSymbol + "/TwitterSearchData_" + stockSymbol + "2.json";
            JSONObject twitterJSONData2 = FileFunctions.LoadJSONFile(context, extendedFilePathTwitter2);

            // Check that the file is not empty (always a possibility).
            if (twitterJSONData1 != null) {
                if (twitterJSONData1.getJSONObject("meta").getInt("result_count") != 0) {
                    // Add all the Twitter information to the combined json file.
                    JSONArray twitterJSONDataArray1 = twitterJSONData1.getJSONArray("data");
                    JSONArray twitterJSONUserDataArray1 = twitterJSONData1.getJSONObject("includes").getJSONArray("users");
                    for (int i = 0; i < twitterJSONDataArray1.length(); i++) {
                        JSONObject data = new JSONObject();
                        JSONObject jsonObject = twitterJSONDataArray1.getJSONObject(i);
                        JSONObject metricsJSON = jsonObject.getJSONObject("public_metrics");
                        // Make sure that the tweet is not a retweet and is in english.
                        boolean isValid = ((metricsJSON.getInt("retweet_count") > 0 || (metricsJSON.getInt("reply_count") > 0) || (metricsJSON.getInt("like_count") > 0) )) && jsonObject.getString("lang").equals("en") && !jsonObject.getString("text").contains("RT");
                        if (isValid) {
                            String authorID = jsonObject.getString("author_id");
                            String username = TwitterData.GetTwitterUsername(authorID, twitterJSONUserDataArray1);
                            data.put("id", authorID);
                            data.put("media_source", "twitter");
                            data.put("user_or_network", username);
                            data.put("content", jsonObject.getString("text"));
                            int interactions = metricsJSON.getInt("retweet_count") + metricsJSON.getInt("reply_count") + metricsJSON.getInt("like_count");
                            data.put("interactions", interactions);
                            double sentiment = CalculateSentiment(jsonObject.getString("text"));
                            data.put("sentiment", sentiment);
                            data.put("date_created", jsonObject.getString("created_at"));
                            data.put("link", "NA");
                            stockDataArray.put(data);
                            id += 1;
                        }
                    }
                }
            }

            if (twitterJSONData2 != null) {
                if (twitterJSONData2.getJSONObject("meta").getInt("result_count") != 0) {
                    JSONArray twitterJSONDataArray2 = twitterJSONData2.getJSONArray("data");
                    JSONArray twitterJSONUserDataArray2 = twitterJSONData2.getJSONObject("includes").getJSONArray("users");
                    for (int i = 0; i < twitterJSONDataArray2.length(); i++) {
                        JSONObject data = new JSONObject();
                        JSONObject jsonObject = twitterJSONDataArray2.getJSONObject(i);
                        JSONObject metricsJSON = jsonObject.getJSONObject("public_metrics");
                        // Make sure that the tweet is not a retweet and is in english.
                        boolean isValid = ((metricsJSON.getInt("retweet_count") > 0 || (metricsJSON.getInt("reply_count") > 0) || (metricsJSON.getInt("like_count") > 0) )) && jsonObject.getString("lang").equals("en") && !jsonObject.getString("text").contains("RT");
                        if (isValid) {
                            String authorID = jsonObject.getString("author_id");
                            String username = TwitterData.GetTwitterUsername(authorID, twitterJSONUserDataArray2);
                            data.put("id", id);
                            data.put("media_source", "twitter");
                            data.put("user_or_network", username);
                            data.put("content", jsonObject.getString("text"));
                            int interactions = metricsJSON.getInt("retweet_count") + metricsJSON.getInt("reply_count") + metricsJSON.getInt("like_count");
                            data.put("interactions", interactions);
                            double sentiment = CalculateSentiment(jsonObject.getString("text"));
                            data.put("sentiment", sentiment);
                            data.put("date_created", jsonObject.getString("created_at"));
                            data.put("link", "NA");
                            stockDataArray.put(data);
                            id += 1;
                        }
                    }
                }
            }

            // Creating the reddit json object from the json file.
            String extendedFilePathRedditData = folderPath + "/" + stockSymbol + "/RedditSearchData_" + stockSymbol + ".json";
            JSONObject redditSearchJSONData = FileFunctions.LoadJSONFile(context, extendedFilePathRedditData);

            // Add all the Reddit information to the combined json array.
            if (redditSearchJSONData != null) {
                JSONArray redditJSONDataArray = redditSearchJSONData.getJSONArray("data");
                for (int i = 0; i < redditJSONDataArray.length(); i++) {
                    JSONObject data = new JSONObject();
                    JSONObject jsonObject = redditJSONDataArray.getJSONObject(i);
                    // Make sure that the comment has some interactions
                    if (jsonObject.has("sentiment") && !(jsonObject.getDouble("sentiment") == 0)) {
                        data.put("id", id);
                        data.put("media_source", "reddit");
                        data.put("user_or_network", jsonObject.getString("id"));
                        data.put("content", jsonObject.getString("body"));
                        int interactions = (int) abs((jsonObject.getDouble("sentiment")) * 20);
                        data.put("interactions", interactions);
                        data.put("sentiment", jsonObject.getDouble("sentiment"));
                        data.put("date_created", jsonObject.getString("created_utc"));
                        data.put("link", jsonObject.getString("permalink"));
                        stockDataArray.put(data);
                        id += 1;
                    }
                }
            }

            // Creating the news json object from the json file.
            String extendedFilePathNewsData = folderPath + "/" + stockSymbol + "/NewsData_" + stockSymbol + ".json";
            JSONObject newsJSONData = FileFunctions.LoadJSONFile(context, extendedFilePathNewsData);

            if (newsJSONData != null) {
                // Add all the Google News information to the combined json file.
                JSONArray newsJSONDataArray = newsJSONData.getJSONArray("data");
                for (int i = 0; i < newsJSONDataArray.length(); i++) {
                    JSONObject data = new JSONObject();
                    JSONObject jsonObject = newsJSONDataArray.getJSONObject(i);
                    data.put("id", id);
                    data.put("media_source", "General News - " + jsonObject.getString("source"));
                    data.put("user_or_network", jsonObject.getString("title"));
                    data.put("content", jsonObject.getString("description"));
                    Random rand = new Random();
                    int min = 10;
                    int max = 30;
                    int interactions = rand.nextInt(max - min + 1) + min;
                    data.put("interactions", interactions);
                    double sentiment = CalculateSentiment(jsonObject.getString("description"));
                    data.put("sentiment", sentiment);
                    data.put("date_created", jsonObject.getString("published_at"));
                    data.put("link", jsonObject.getString("url"));
                    stockDataArray.put(data);
                    id += 1;
                }
            }

            String filePath = folderPath + "/" + stockSymbol + "/CombinedData_" + stockSymbol + ".json";
            combinedStockData.put("data", stockDataArray);
            FileFunctions.CreateFile(context, filePath, combinedStockData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject GetMediaInformationForStock(Context context, String stockSymbol) {
        String extendedFilePath = "/stock_information/"+ stockSymbol +"/CombinedData_" + stockSymbol + ".json";
        return FileFunctions.LoadJSONFile(context, extendedFilePath);
    }

    public static JSONObject GetMediaInformationForStockFavorites(Context context, String stockSymbol) {
        String extendedFilePath = "/favorites_stock_information/"+ stockSymbol +"/CombinedData_" + stockSymbol + ".json";
        return FileFunctions.LoadJSONFile(context, extendedFilePath);
    }

    public static double CalculateSentiment(String content) {
        // Created list of bad, good, and neutral words.
        String[] goodWords = {"good", "surged", "rally", "soar", "highest", "high"};
        String[] badWords = {"bad", "worse", "drop", "lowest", "bullish", "low"};
        double positiveSentiment = 0;
        double negativeSentiment = 0;
        double totalSentiments = 0;
        double polarity;
        for (int i = 0; i < 6; i++) {
            if (content.contains(goodWords[i])) {
                positiveSentiment++;
                totalSentiments++;
            }
            else if (content.contains(badWords[i])) {
                negativeSentiment++;
                totalSentiments++;
            }
        }
        // Determine the overall sentiment of the content.
        polarity = (((positiveSentiment - negativeSentiment) / totalSentiments));
        if (Double.isNaN(polarity)) {
            return 0;
        }
        return polarity;
    }
}
