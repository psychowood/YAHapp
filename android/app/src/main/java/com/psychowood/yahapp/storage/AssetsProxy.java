package com.psychowood.yahapp.storage;

import android.content.Context;
import android.widget.Toast;

import com.psychowood.yahapp.R;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.OkHttpConnector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by gg on 28/08/2016.
 */

public class AssetsProxy {

    public static final String UPDATES_PREFIX = "updates/";
    public static final String HENKAKU_ASSETS_PREFIX = "henkaku/";

    private Context context;
    private File cacheDir;
    private GitHub gitHub = null;

    public AssetsProxy(Context context) {
        this.context = context;
        this.cacheDir = new File(context.getCacheDir(),UPDATES_PREFIX);
    }

    public InputStream openAsset(String assetRoot, String assetRelativePath) throws IOException {
        final String assetPath = assetRoot + assetRelativePath;
        File cachedFile = new File(cacheDir, assetPath);
        if (cachedFile.exists()) {
            return new FileInputStream(cachedFile);
        } else {
            return context.getAssets().open(assetPath);
        }
    }

    public File getCacheDir() {
        return cacheDir;
    }
}
