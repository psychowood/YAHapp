package com.psychowood.yahapp.vpk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ch.timesplinter.sfo4j.common.SFODataValue;
import ch.timesplinter.sfo4j.reader.SFOReader;

/**
 * Created by gg on 25/08/2016.
 */

public class VpkFile {
    final static String TAG = "VpkFile";

    private String fileName;
    private String filePath;
    private Map<String, SFODataValue> paramSfo;
    private Bitmap icon;

    private static final String PARAM_SFO_FILE = "sce_sys/param.sfo";
    private static final String ICON0_FILE = "sce_sys/icon0.png";
    private static final String EBOOT_BIN_FILE = "eboot.bin";

    private static final String PARAM_PSP2_DISP_VER = "";
    private static final String PARAM_TITLE = "TITLE";

    public VpkFile(InputStream is, String fileName) throws IOException {
        this.fileName = fileName;
        init(is);
    }

    public VpkFile(String filePath) throws IOException {

        this.filePath = filePath;
        try {
            filePath = URLDecoder.decode(filePath,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG,"Error",e);
        }
        int cut = filePath.lastIndexOf('/');
        if (cut != -1) {
            this.fileName = filePath.substring(cut + 1);
        }
        init(new FileInputStream(filePath));
    }

    private void init(InputStream is) throws IOException {
        ZipInputStream zis;

        String filename;
        zis = new ZipInputStream(new BufferedInputStream(is));
        ZipEntry ze;
        byte[] buffer = new byte[8192];
        int count;

        boolean ebootBinFound = false;
        boolean isZipFile = false;
        while ((ze = zis.getNextEntry()) != null)
        {
            isZipFile = true;
            filename = ze.getName();

            if (ze.isDirectory()) {
                continue;
            }

            if (filename.equalsIgnoreCase(PARAM_SFO_FILE)) {
                SFOReader sfoReader = new SFOReader(zis);
                paramSfo = sfoReader.getKeyValueMap();
            } else if (filename.equalsIgnoreCase(ICON0_FILE)) {
                icon = BitmapFactory.decodeStream(zis);
            } else if (filename.equalsIgnoreCase(EBOOT_BIN_FILE)){
                ebootBinFound = true;
            }

            zis.closeEntry();

            if (paramSfo != null && icon != null && ebootBinFound) {
                // Stopping because we are not interested in other files
                break;
            }
        }

        if (!isZipFile) {
            throw new IOException(this.fileName + " is not a zip file");
        }

        final String missingPrefix = "Zip file is missing ";
        if (paramSfo == null) {
            throw new IOException(missingPrefix + PARAM_SFO_FILE);
        }

        if (icon == null) {
            //throw new IOException(missingPrefix + ICON0_FILE); // Not mandatory

        }


        if (!ebootBinFound) {
            throw new IOException(missingPrefix + EBOOT_BIN_FILE);
        }

        zis.close();

    }

    public Bitmap getIcon() {
        return icon;
    }

    public Map<String, SFODataValue> getParamSfo() {
        return paramSfo;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTitle() {
        if (paramSfo.get("TITLE") != null) {
            return paramSfo.get("TITLE").toString();
        } else if (paramSfo.get("STITLE") != null) {
            return paramSfo.get("STITLE").toString();
        } else {
            return "undefined";
        }
    }
}
