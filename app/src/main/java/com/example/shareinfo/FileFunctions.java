package com.example.shareinfo;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Objects;

public class FileFunctions {

    public static void CreateFile(Context context, String extendedFilePath, String content) {
        try {
            String filePath = context.getFilesDir().getAbsolutePath();
            File file = new File(filePath + extendedFilePath);
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(content.getBytes());
            stream.write("\n".getBytes());
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject LoadJSONFile(Context context, String extendedFilePath) {
        JSONObject jsonObject = null;
        try {
            String filePath = context.getFilesDir().getAbsolutePath() + extendedFilePath;
            FileInputStream file = new FileInputStream(filePath);
            int fileSize = file.available();
            byte[] rawData = new byte[fileSize];
            int result = file.read(rawData);
            file.close();
            if (!Arrays.toString(rawData).equals("")) {
                jsonObject = new JSONObject(new String(rawData, "UTF-8"));
            }


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static void DeleteFolder(Context context, String extendedFilePath){
        String filePath = context.getFilesDir().getAbsolutePath() + extendedFilePath;
        File fileOrDirectory = new File(filePath);
        if (fileOrDirectory.isDirectory()) {
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles())) {
                DeleteFolder(context, extendedFilePath + "/" + child.getName());
            }
        }
        boolean result = fileOrDirectory.delete();
    }
}
