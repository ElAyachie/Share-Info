package com.example.shareinfo;

import android.content.Context;

import java.io.File;

public class Setup {
    public static void CreateFolders(Context context) {
        String filePath = context.getFilesDir().getAbsolutePath();
        String folderName = "/stock_information";
        File file = new File(filePath + folderName);
        boolean exists = file.isDirectory();
        if (!exists) {
            File folder = new File(filePath, folderName);
            folder.mkdir();
        }
    }
}
