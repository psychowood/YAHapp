package com.psychowood.yahapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DownloadFileWithProgressAsyncTask extends AsyncTask<String, Void, File> {
    private static final String TAG = "YAHDownloadTask";

    ProgressDialog asyncDialog;
    DownloadHandler caller;
    Handler handler;
    Context context;

    public DownloadFileWithProgressAsyncTask(DownloadHandler caller) {
        super();
        this.caller = caller;
        this.context = caller.getContext();
        this.handler = caller.getHandler();
        asyncDialog = new ProgressDialog(context);
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected File doInBackground(String... params) {

        //don't touch dialog here it'll break the application
        //do some lengthy stuff like calling login webservice

        File cacheFile = null;

        String filePath = null;
        if (params.length > 0) {
            filePath = params[0];
        } else {
            Toast.makeText(context, R.string.error_missing_url, Toast.LENGTH_LONG).show();
            return null;
        }

        String userAgent = "YAHapp/";

        try {
            userAgent += App.getPackageInfo(context).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG,"Cannot get version info",e);
            userAgent += "unavailable";
        }
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(filePath)
                .addHeader("User-Agent", userAgent)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            final String fileName;
            String fileNameTmp = filePath.replaceAll("^.*/([^/]*)$","$1");

            InputStream is = response.body().byteStream();
            final Headers headers = response.headers();

            String contentDisposition = headers.get("Content-Disposition");
            if (contentDisposition != null) {
                final String searchStr = "filename=";
                int nameIndex = contentDisposition.indexOf(searchStr);
                if (nameIndex != -1) {
                    fileNameTmp = contentDisposition.substring(nameIndex+searchStr.length());
                }
            }

            if (fileNameTmp.contains("?")) {
                fileName = fileNameTmp.split("\\?")[0];
            } else {
                fileName = fileNameTmp;
            }

            final Integer contentLength;
            Integer contentLengthTmp;
            try {
                contentLengthTmp = Integer.parseInt(headers.get("Content-Length"));
            } catch(NumberFormatException e) {
                contentLengthTmp = -1;
            }
            contentLength = contentLengthTmp;


            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (contentLength > 0) {
                        asyncDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        asyncDialog.setIndeterminate(false);
                        asyncDialog.setMax(1000);
                        asyncDialog.setProgress(0);
                        asyncDialog.setProgressNumberFormat(null);
                    }
                    asyncDialog.setTitle(context.getString(R.string.download_downloading) + " " + fileName);
                    asyncDialog.setMessage(context.getString(R.string.download_starting));
                    asyncDialog.show();
                }
            });

            BufferedInputStream input = new BufferedInputStream(is);
            cacheFile = new File(context.getCacheDir(), fileName);

            OutputStream output = new FileOutputStream(cacheFile);

            byte[] data = new byte[8192];

            long total = 0;
            int count;

            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);

                final int progress = (int) (total/(contentLength/1000));
                final String totalKB = (int) (total/1024) + "KB";
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (contentLength > 0) {
                            asyncDialog.setProgress(progress);
                        }
                        asyncDialog.setMessage(context.getString(R.string.download_downloaded)  + " " + totalKB);
                    }
                });
            }

            output.flush();
            output.close();
            input.close();

        } catch (IOException e) {
            caller.showError(context.getString(R.string.error_invalid_address) + ": " + e!=null?e.getMessage():"null");
            return null;
        }

        return cacheFile;
    }

    @Override
    protected void onPostExecute(File tempFile) {
        asyncDialog.dismiss();
        super.onPostExecute(tempFile);
        caller.onDownloadCompleted(tempFile);
    }

    public interface DownloadHandler {
        public Context getContext();
        public Handler getHandler();
        public void onDownloadCompleted(File tempFile);
        public void showError(String message);
    }

}