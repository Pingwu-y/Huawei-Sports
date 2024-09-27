package me.w1049.hsports;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileManager {
    private Context context;

    public FileManager(Context context) {
        this.context = context;
    }

    public void writeToFile(String filename, String str) {
        File files = context.getExternalFilesDir("");
        if (files == null) {
            Log.e("HSportsFileManager", "无法获取外部存储目录");
            return;
        }
        String path = files.getAbsolutePath() + "/" + filename;
        File file = new File(path);
        Log.i("HSportsFileManager", "写入到：" + path);
        try (FileWriter fileWriter = new FileWriter(file, true)) {
            fileWriter.append(str);
        } catch (IOException e) {
            Log.e("HSportsFileManager", "写入文件失败", e);
        }
    }
}