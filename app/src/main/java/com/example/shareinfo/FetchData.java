package com.example.shareinfo;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchData extends AsyncTask<Void, Void, Void> {
    String googleNewsData = "";
    String stockInfoData = "";
    private final Context context;

    public FetchData (Context context){
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        try {
            URL googleNewsUrl = new URL("https://google-news1.p.rapidapi.com/topic-headlines?topic=WORLD&country=US&lang=en");
            HttpURLConnection googleNewsConnection = (HttpURLConnection) googleNewsUrl.openConnection();
            googleNewsConnection.setRequestProperty("x-rapidapi-host", "google-news1.p.rapidapi.com");
            googleNewsConnection.setRequestProperty("x-rapidapi-key", "132a0cb470mshf7b87dbc92557f1p1cf34bjsna1ca8152da1a");
            googleNewsConnection.setRequestMethod("GET");
            InputStream inputStream = googleNewsConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            googleNewsData = bufferedReader.readLine();
            Log.d("Google News JSON Data", googleNewsData);
            URL stockInfoUrl = new URL("https://apidojo-yahoo-finance-v1.p.rapidapi.com/auto-complete?q=tesla&region=US");
            HttpURLConnection stockInfoConnection = (HttpURLConnection) stockInfoUrl.openConnection();
            stockInfoConnection.setRequestProperty("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com");
            stockInfoConnection.setRequestProperty("x-rapidapi-key", "132a0cb470mshf7b87dbc92557f1p1cf34bjsna1ca8152da1a");
            stockInfoConnection.setRequestMethod("GET");
            inputStream = stockInfoConnection.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            stockInfoData = bufferedReader.readLine();
            Log.d("Stock JSON Data", stockInfoData);

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        // Save the JSON information to the file storage
        try {
            // Saving Google News data in a JSON file
            String filePath = context.getFilesDir().getAbsolutePath();
            String googleNewsJSONFileName = "GoogleNewsData.json";
            File googleNewsDataFile = new File(filePath, googleNewsJSONFileName);
            FileOutputStream stream = new FileOutputStream(googleNewsDataFile);
            stream.write(googleNewsData.getBytes());
            stream.write("\n".getBytes());
            stream.close();

            // Saving Stock information data in a JSON file
            String stockInfoJSONFileName = "StockData.json";
            File stockInfoDatafile = new File(filePath, stockInfoJSONFileName);
            stream = new FileOutputStream(stockInfoDatafile);
            stream.write(stockInfoData.getBytes());
            stream.write("\n".getBytes());
            stream.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        return null;
    }
    @Override
    protected  void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}

