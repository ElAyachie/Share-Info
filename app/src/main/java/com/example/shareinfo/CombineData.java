package com.example.shareinfo;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;

public class CombineData {
    public static void CombineAndSaveData(Context context, String stockSymbol) {
        try {
            // Creating the twitter json object from the json file.
            String twitterFilePath1 = context.getFilesDir().getAbsolutePath() + "/stock_information/TwitterSearchData_"+ stockSymbol +".json";
            FileInputStream twitterFile1 = new FileInputStream(twitterFilePath1);
            int twitterFileSize1 = twitterFile1.available();
            byte[] twitterRawData1 = new byte[twitterFileSize1];
            int result = twitterFile1.read(twitterRawData1);
            twitterFile1.close();
            JSONObject twitterJSONData1 = new JSONObject(new String(twitterRawData1, "UTF-8"));
            // Creating the twitter trending json object from the json file.
            String twitterFilePath2 = context.getFilesDir().getAbsolutePath() + "/stock_information/TwitterSearchData_"+ stockSymbol +"2.json";
            FileInputStream twitterFile2 = new FileInputStream(twitterFilePath2);
            int twitterFileSize2 = twitterFile2.available();
            byte[] twitterRawData2 = new byte[twitterFileSize2];
            result = twitterFile2.read(twitterRawData2);
            twitterFile2.close();
            JSONObject twitterJSONData2 = new JSONObject(new String(twitterRawData2, "UTF-8"));

            // Creating the news json object from the json file.
            String newsFilePath = context.getFilesDir().getAbsolutePath() + "/stock_information/NewsData_"+ stockSymbol +".json";
            FileInputStream newsFile = new FileInputStream(newsFilePath);
            int newsFileSize = newsFile.available();
            byte[] newsRawData = new byte[newsFileSize];
            result = newsFile.read(newsRawData);
            newsFile.close();
            JSONObject newsJSONData = new JSONObject(new String(newsRawData, "UTF-8"));

            // Creating the reddit json object from the json file.
            String redditFilePath = context.getFilesDir().getAbsolutePath() + "/stock_information/RedditSearchData_"+ stockSymbol +".json";
            FileInputStream redditSearchFile = new FileInputStream(redditFilePath);
            int redditSearchFileSize = redditSearchFile.available();
            byte[] redditSearchRawData = new byte[redditSearchFileSize];
            result = redditSearchFile.read(redditSearchRawData);
            redditSearchFile.close();
            JSONObject redditSearchJSONData = new JSONObject(new String(redditSearchRawData, "UTF-8"));

            // Creating the json object for combined news and social media.
            JSONObject combinedStockData = new JSONObject();
            JSONArray stockDataArray = new JSONArray();
            int id = 0;

            // Add all the Twitter information to the combined json file.
            JSONArray twitterJSONDataArray1 = twitterJSONData1.getJSONArray("data");
            JSONArray twitterJSONUserDataArray = twitterJSONData1.getJSONObject("includes").getJSONArray("users");
            for (int i = 0; i < twitterJSONDataArray1.length(); i++) {
                JSONObject data = new JSONObject();
                JSONObject jsonObject = twitterJSONDataArray1.getJSONObject(i);
                // Make sure that the tweet is not a retweet and is in english.
                if (!jsonObject.getString("text").contains("RT") && jsonObject.getString("lang").equals("en")) {
                    JSONObject userJSONObject = twitterJSONUserDataArray.getJSONObject(i);
                    JSONObject metricsJSON = jsonObject.getJSONObject("public_metrics");
                    data.put("id", id);
                    data.put("media_source", "twitter");
                    data.put("user_or_network", userJSONObject.getString("username"));
                    data.put("content", jsonObject.getString("text"));
                    data.put("interactions", metricsJSON.getInt("like_count"));
                    data.put("date_created", jsonObject.getString("created_at"));
                    data.put("link", "NA");
                    stockDataArray.put(data);
                    id += 1;
                }
            }

            JSONArray twitterJSONDataArray2 = twitterJSONData2.getJSONArray("data");
            twitterJSONUserDataArray = twitterJSONData2.getJSONObject("includes").getJSONArray("users");
            for (int i = 0; i < twitterJSONDataArray2.length(); i++) {
                JSONObject data = new JSONObject();
                JSONObject jsonObject = twitterJSONDataArray2.getJSONObject(i);
                // Make sure that the tweet is not a retweet and is in english.
                if (!jsonObject.getString("text").contains("RT") && jsonObject.getString("lang").equals("en")) {
                    JSONObject userJSONObject = twitterJSONUserDataArray.getJSONObject(i);
                    JSONObject metricsJSON = jsonObject.getJSONObject("public_metrics");
                    data.put("id", id);
                    data.put("media_source", "twitter");
                    data.put("user_or_network", userJSONObject.getString("username"));
                    data.put("content", jsonObject.getString("text"));
                    data.put("interactions", metricsJSON.getInt("like_count"));
                    data.put("date_created", jsonObject.getString("created_at"));
                    data.put("link", "NA");
                    stockDataArray.put(data);
                    id += 1;
                }
            }

            // Add all the Reddit information to the combined json array.
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
                    data.put("interactions", jsonObject.getDouble("sentiment"));
                    data.put("date_created", jsonObject.getString("created_utc"));
                    data.put("link", "NA");
                    stockDataArray.put(data);
                    id += 1;
                }
            }

            // Add all the Google News information to the combined json file.
            JSONArray newsJSONDataArray = newsJSONData.getJSONArray("data");

            for (int i = 0; i < newsJSONDataArray.length(); i++) {
                JSONObject data = new JSONObject();
                JSONObject jsonObject = newsJSONDataArray.getJSONObject(i);
                data.put("id", id);
                data.put("media_source", "General News - " + jsonObject.getString("source"));
                data.put("user_or_network", jsonObject.getString("title"));
                data.put("content", jsonObject.getString("description"));
                data.put("interactions", "NA");
                data.put("date_created", jsonObject.getString("published_at"));
                data.put("link", jsonObject.getString("url"));
                stockDataArray.put(data);
                id += 1;
            }

            combinedStockData.put("data", stockDataArray);
            String filePath = context.getFilesDir().getAbsolutePath() + "/stock_information" + stockSymbol;
            String combinedDataFileName = "CombinedData_"+ stockSymbol +".json";
            File combinedDataFile = new File(filePath, combinedDataFileName);
            FileOutputStream stream = new FileOutputStream(combinedDataFile);
            stream.write(combinedStockData.toString().getBytes());
            stream.write("\n".getBytes());
            stream.close();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
