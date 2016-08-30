package com.psychowood.yahapp;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.psychowood.yahapp.storage.AssetsProxy;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.OkHttpConnector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UpdateCheckerActivity extends TextStatusActivityBase {
    private static final String TAG = "YAHFtpClientActivity";

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onStart ()
    {
        Log.d (TAG, "+ onStart");
        super.onStart ();

        new UpdateTask().execute();

        Log.d (TAG, "- onStart");
        return;
    } // onStart

    @Override
    protected void onPause() {
        super.onPause();
    }

    class UpdateTask extends AsyncTask<String, Void, Boolean> {

        private OkHttpClient httpClient;

        public UpdateTask() {
            File cacheDirectory = new File(me.getCacheDir(),"httpcache");
            Cache cache = new Cache(cacheDirectory, 16 * 1024 * 1024); // 32MB cache
            this.httpClient = new OkHttpClient().setCache(cache);
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }
        @Override
        protected Boolean doInBackground(String... params) {
            String fileToCheck = null;
            if (params.length > 0) {
                fileToCheck = params[0];
            }
            boolean hasUpdates = false;
            final GitHub gh;
            try {
                log("Connecting to GitHub...");

                GitHub gitHub = GitHubBuilder.fromEnvironment()
                        .withConnector(new OkHttpConnector(new OkUrlFactory(httpClient)))
                        .build();

                final SharedPreferences prefsUpdates = getPrefs("updates");
                final String henkakuSettings = "updates-cache";
                final SharedPreferences henkakuUpdates = getPrefs(henkakuSettings+"-henkaku");

                final String cacheDir = AssetsProxy.UPDATES_PREFIX + AssetsProxy.HENKAKU_ASSETS_PREFIX;
                File cacheDirectory = new File(me.getCacheDir(), cacheDir);
                if (!cacheDirectory.exists()) {
                    log(cacheDir + " not cached.");
                    cacheDirectory.mkdirs();
                }

                log("Checking GitHub API validity");
                gitHub.checkApiUrlValidity();
                final GHRateLimit rateLimit = gitHub.getRateLimit();
                log("GitHub remaining " + rateLimit.remaining + " of " + rateLimit.limit + " until " + rateLimit.getResetDate());

                if (rateLimit.remaining < 3) {
                    log("GitHub limit reached, please retry after " + rateLimit.getResetDate());
                    return null;
                };

                final String henkakuRepoName = "henkaku/henkaku";
                log("Getting repo " + henkakuRepoName);
                final GHRepository henkakuRepo = gitHub.getRepository(henkakuRepoName);

                final GHBranch branch = henkakuRepo.getBranch("offline-hosting");
                final GHTree tree = henkakuRepo.getTreeRecursive(branch.getSHA1(), 1);

                final List<GHTreeEntry> contents = tree.getTree();

                Set<String> files = new HashSet<>(Arrays.asList(new String[]{"loader.rop.bin","exploit.rop.bin"}));
                String rawUrl = "https://raw.githubusercontent.com/henkaku/henkaku/offline-hosting/";
                for (GHTreeEntry content : contents) {
                    final String path = content.getPath();
                    if (content.getType().equalsIgnoreCase("blob") && ( path.startsWith("host/pkg/") || files.contains(path) ) ) {
                        hasUpdates = (downloadFileIfUpdated(henkakuUpdates, cacheDirectory, content, AssetsProxy.HENKAKU_ASSETS_PREFIX, rawUrl)) || hasUpdates;
                    }
                }

            } catch (IOException e) {
                showError(getString(R.string.error_github_not_accessible));
            }

            return hasUpdates;
        }

        private boolean downloadFileIfUpdated(SharedPreferences trackingPref, File cacheDirectory, GHTreeEntry content, String assetRoot, String baseDownloadUrl) throws IOException {
            String fileName = content.getPath();
            String sha = content.getSha();
            boolean updateSha = false;
            boolean updatedFile = false;
            String lastSha = trackingPref.getString(fileName, null);

            if (lastSha == null) {
                try {
                    lastSha = getSha1(getAssets().open(assetRoot + fileName));
                    updateSha = true;
                } catch (FileNotFoundException e) {
                    //File not found, new asset
                }
            }

            File saveToFile = new File(cacheDirectory, fileName);
            if (!saveToFile.exists() || lastSha == null || !lastSha.equalsIgnoreCase(sha)) {
                log("Downloading " + fileName + " ...");

                //String downloadUrl = content.getUrl().toString(); //Wrong, gets json contents
                downloadFile(baseDownloadUrl + fileName,saveToFile);

                updatedFile = true;
            } else {
                updatedFile = false;
            }
            if (updatedFile || updateSha) {
                SharedPreferences.Editor edit = trackingPref.edit();
                edit.putString(fileName, sha);
                edit.commit();
            }

            return updatedFile;
        }

        private void downloadFile(String url, File saveTo) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            InputStream in = null;
            OutputStream out = null;

            if (!saveTo.exists()) {
                saveTo.getParentFile().mkdirs();
                saveTo.createNewFile();
            }

            try {
                in = response.body().byteStream();
                out = new FileOutputStream(saveTo);
                byte[] buf = new byte[65536];
                int len;
                while((len=in.read(buf))>0){
                    out.write(buf,0,len);
                }
            } finally {
                if (out != null) out.close();
                if (in != null) in.close();
            }

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result != null) {
                String message;
                if (result.booleanValue()) {
                    message = "Updated!";
                } else {
                    message = "Already up-to-date, nothing to do.";
                }
                log(message);
                new AlertDialog.Builder(me)
                        .setTitle(getString(R.string.enjoy))
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                onPause();
                                finish();
                            }
                        })
                        .show();
            } else {
                showError("Something went wrong.");
            }
        }
    }

    private String getSha1(InputStream is) throws IOException {
        DigestInputStream shaStream = null;
        try {
            shaStream = new DigestInputStream(
                    is, MessageDigest.getInstance("SHA-1"));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 algorithm unavailable",e);
        }
        byte[] bytes = shaStream.getMessageDigest().digest();
        final char[] hexArray = "0123456789ABCDEF".toCharArray();

        char[] hexChars = new char[ bytes.length * 2 ];
        for( int j = 0; j < bytes.length; j++ )
        {
            int v = bytes[ j ] & 0xFF;
            hexChars[ j * 2 ] = hexArray[ v >>> 4 ];
            hexChars[ j * 2 + 1 ] = hexArray[ v & 0x0F ];
        }
        String sha = null;
        try {
            sha = new String(hexChars);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return sha;

    }

}