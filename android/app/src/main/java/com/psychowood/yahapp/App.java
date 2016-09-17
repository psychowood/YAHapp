package com.psychowood.yahapp;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.File;

/**
 * Created by gg on 28/08/2016.
 */

public class App {
    public static final String APP_PREFS = "YAPappPrefs";
    public static final String PREF_PSVITAIP = "PSVitaIP";

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {}
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    public static PackageInfo getPackageInfo(Context context) throws PackageManager.NameNotFoundException {
        final String packageName = context.getApplicationContext().getPackageName();
        return context.getPackageManager().getPackageInfo(packageName,0);
    }
}
