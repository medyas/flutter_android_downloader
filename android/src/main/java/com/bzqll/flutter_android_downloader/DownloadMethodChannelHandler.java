package com.bzqll.flutter_android_downloader;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class DownloadMethodChannelHandler implements MethodChannel.MethodCallHandler {
    private final Context context;
    private final DownloadManager manager;
    private final Activity activity;

    static final int PERMISSION_CODE = 1000;

    DownloadMethodChannelHandler(Context context, DownloadManager manager, Activity activity) {
        this.context = context;
        this.manager = manager;
        this.activity = activity;
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull MethodChannel.Result result) {

        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + Build.VERSION.RELEASE);
                break;
            case "getPermission":
                boolean p = requestPermissions();
                result.success(p);
                break;
            case "download":
                String url = call.argument("url");
                String fileName = call.argument("fileName");
                String directory = call.argument("directory");
                String originName = call.argument("originName");
                Map<String, String> headers = call.argument("headers");

               boolean permission = requestPermissions();
                if (permission) {
                    Long downloadId = startDownload(url, fileName, directory, originName, headers);
                    result.success(downloadId);
                    return;
                }
                result.success(null);

                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private boolean requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(activity, permissions, PERMISSION_CODE);
                return false;
            }
        }
        return true;
    }

    private long startDownload(String url, String fileName, String directory, String originName, Map<String, String> headers) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        if (headers != null) {
            for (String key : headers.keySet()) {
                request.addRequestHeader(key, headers.get(key));
            }
        }
        request.allowScanningByMediaScanner();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        } else {
            request.setDestinationUri(Uri.fromFile(new File(directory + "/" + fileName)));
        }
        request.setTitle(fileName);
        request.setAllowedOverRoaming(true);
        request.setDescription(originName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        return manager.enqueue(request);
    }
}
