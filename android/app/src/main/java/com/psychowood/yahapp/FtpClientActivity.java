package com.psychowood.yahapp;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.psychowood.yahapp.vpk.VpkFile;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class FtpClientActivity extends TextStatusActivityBase {
    private static final int PUSH_VPK_FILE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final String TAG = "YAHFtpClientActivity";

    private String filePathToPush = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //chooseFile();
        StringBuffer buf = new StringBuffer();

        buf.append("\n");
        buf.append("\n");

        textView.setText(buf.toString());

    }

    @Override
    public void onStart ()
    {
        Log.d (TAG, "+ onStart");
        super.onStart ();

        final Intent intent = getIntent ();

        if (intent != null)
        {
            Log.d (TAG, "> Got intent : " + intent);

            VpkFile vpkFile = null;
            String filePath = null;
            InputStream inputStream = null;
            String fileName = null;

            final String action = intent.getAction();
            if (Intent.ACTION_SEND.equalsIgnoreCase(action)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    final ClipData clipData = intent.getClipData();
                    if (clipData != null) {
                        final int itemCount = clipData.getItemCount();
                        if (itemCount == 1) {
                            final ClipData.Item item = clipData.getItemAt(0);
                            final Uri uri = item.getUri();
                            if (uri.getScheme().equalsIgnoreCase("content")) {
                                try {
                                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                                    try {
                                        if (cursor != null && cursor.moveToFirst()) {
                                            fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                        }
                                    } finally {
                                        cursor.close();
                                    }

                                    inputStream = getContentResolver().openInputStream(uri);

                                } catch (FileNotFoundException e) {
                                    Log.e(TAG,"Cannot read shared uri " + uri,e);
                                }
                            } else if (uri.getScheme().equalsIgnoreCase("file")) {
                                filePath = uri.getEncodedPath();
                            }
                        }
                    }
                }

            } else if (Intent.ACTION_VIEW.equalsIgnoreCase(action)) {

                final Uri data = intent.getData();

                if (data != null) {
                    Log.d(TAG, "> Got data   : " + data);

                    if (data.getScheme().equalsIgnoreCase("file")) {
                        filePath = data.getEncodedPath();
                    } else {
                        filePath = data.toString();
                    }


                    Log.d(TAG, "> Open file  : " + filePath);

                } // if
            }
            if (vpkFile == null) {
                if (filePath != null) {
                    // Check if we have read or write permission
                    int readPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

                    if (readPermission != PackageManager.PERMISSION_GRANTED) {
                        // We don't have permission so prompt the user
                        filePathToPush = filePath;
                        ActivityCompat.requestPermissions(
                                this,
                                PERMISSIONS_STORAGE,
                                PUSH_VPK_FILE
                        );
                    } else {
                        pushVpkFile(filePath);
                    }

                } else if (inputStream != null) {
                    pushVpkFile(inputStream,fileName);
                }
            }

        } // if
        Log.d (TAG, "- onStart");
        return;
    } // onStart

    private void pushVpkFile(String filePath) {
        _pushVpkFile(filePath,null,null);
    }

    private void pushVpkFile(InputStream inputStream, String fileName) {
        _pushVpkFile(null,inputStream,fileName);
    }

    private void _pushVpkFile(final String filePath, InputStream inputStream, String fileName) {
        VpkFile vpkFile = null;
        File cacheFile = null;
        try {
            if (filePath != null) {
                vpkFile = new VpkFile(filePath);
            } else if (inputStream != null) {

                if (fileName != null) {
                    cacheFile = new File(me.getCacheDir(), fileName);
                } else {
                    cacheFile = File.createTempFile("vpk","",me.getCacheDir());
                }
                FileOutputStream outputStream = new FileOutputStream(cacheFile);

                int bytesRead = -1;
                byte[] buffer = new byte[65536];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                vpkFile = new VpkFile(new FileInputStream(cacheFile),fileName);
                if (fileName == null) {
                    cacheFile.renameTo(new File(me.getCacheDir(), vpkFile.getTitle()));
                }
                vpkFile.setFilePath(cacheFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(TAG, getString(R.string.error_vpk_cant_read) + filePath, e);

            final String message = getString(R.string.error_vpk_cant_read) + (e != null ? e.getLocalizedMessage() : e);
            showError(message);
            if (cacheFile != null) {
                cacheFile.delete();
            }
        }
        if (vpkFile != null) {
            try {
                final Bitmap icon = vpkFile.getIcon();
                final VpkFile vpk = vpkFile;
                final EditText txtUrl = new EditText(this);
                txtUrl.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

                final SharedPreferences settings = getPrefs();
                final String psVitaIP = settings.getString(App.PREF_PSVITAIP, "");

                txtUrl.setText(psVitaIP);

                final AlertDialog alert = new AlertDialog.Builder(me)
                        .setTitle(getString(R.string.menu_installvdk_enter_psvita_ip))
                        .setView(txtUrl)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();

                alert.setOnShowListener(new DialogInterface.OnShowListener() {

                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button b = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                        b.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {
                                String connectTo = txtUrl.getText().toString();
                                try {
                                    if (connectTo == null || connectTo.length() < 1) {
                                        throw new Exception();
                                    }
                                    InetAddress.getByName(connectTo);
                                } catch (Exception e) {
                                    Toast.makeText(getApplicationContext(), R.string.error_invalid_address, Toast.LENGTH_LONG).show();
                                    return;
                                }

                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString(App.PREF_PSVITAIP,connectTo);
                                editor.commit();

                                if (icon != null){
                                    imageView.setImageBitmap(icon);
                                    imageView.setVisibility(View.VISIBLE);
                                }
                                messageView.setText("Uploading " + vpk.getTitle());
                                messageView.setVisibility(View.VISIBLE);

                                UploadTask async=new UploadTask();

                                connectTo = connectTo + ":1337";
                                textView.append("Connecting to " + connectTo  + "\n");
                                async.execute(connectTo,vpk.getFilePath());
                                alert.dismiss();
                            }
                        });
                    }
                });

                alert.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PUSH_VPK_FILE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String filePath = filePathToPush;
                filePathToPush = null;
                pushVpkFile(filePath);
            } else {
                Toast.makeText(getApplicationContext(), R.string.error_permission_storage_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    class UploadTask extends AsyncTask<String, Void, String> {

        PrintWriter pw;
        BufferedReader input;
        String destFilePath;
        final String DEST_DIR = "/ux0:/yahapp/";

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            //prg = new ProgressDialog(this);
            //prg.setMessage("Uploading...");
            //prg.show();
        }
        @Override
        protected String doInBackground(String... params) {
            Socket socket = null;
            Socket uploadSocket = null;

            try {
                final String[] address = params[0].split(":");
                final InetAddress addr = InetAddress.getByName(address[0]);
                int port = 21;
                if (address.length > 1) {
                    port = Integer.parseInt(address[1]);
                }
                String filePath = params[1];

                socket = new Socket();
                InetSocketAddress socketAddress = new InetSocketAddress(addr, port);
                socket.setSoTimeout(10000);
                socket.connect(socketAddress);

                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pw = new PrintWriter(socket.getOutputStream());

                expect(read(),"220");

                writeAndExpect("USER yahapp","331"); //331

                writeAndExpect("PASS yahapp","230");//230

                writeAndExpect("CWD /ux0:/","250");//250

                writeAndRead("MKD yahapp"); //226 ok - 550 ko - ignoring error in case of already existing dir

                writeAndExpect("CWD " + DEST_DIR,"250"); //250

                writeAndExpect("TYPE I","200"); //200

                String pasvResp = writeAndRead("PASV"); //227
                expect(pasvResp,"227");

                pasvResp = pasvResp.replaceAll("^.*\\((.*)\\).*$","$1");

                String fileName = filePath.replaceAll("^.*/([^/]*)$","$1");

                write("STOR " + fileName);

                destFilePath = DEST_DIR + fileName;

                String[] pasvParts = pasvResp.split(",");

                InetAddress pasvIp = Inet4Address.getByName(pasvParts[0] + "." + pasvParts[1] + "." + pasvParts[2] + "." + pasvParts[3]);
                int pasvPort = Integer.parseInt(pasvParts[4]) * 256 + Integer.parseInt(pasvParts[5]);

                uploadSocket = new Socket(pasvIp, pasvPort);

                BufferedOutputStream fileOs = new BufferedOutputStream(uploadSocket.getOutputStream());

                expect(read(),"150");
                File file = new File(filePath);
                long size = file.length();
                InputStream fileIs = new FileInputStream(file);
                byte[] buf = new byte[8192];
                int read = 0;
                long sent = 0;
                while ((read = fileIs.read(buf)) != -1) {
                    fileOs.write(buf, 0, read);
                    sent += read;
                    long progress = (sent * 100)/ size;
                    showMessage("Sent " + progress + "%");
                }
                fileOs.flush();
                fileOs.close();
                fileIs.close();

                expect(read(),"226");

                return new String("OK");
            } catch (IOException e){
                String t="Failure : " + e.getLocalizedMessage();
                return t;
            } finally {
                if (socket != null && ! socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                    }
                }
                if (uploadSocket != null && ! uploadSocket.isClosed()) {
                    try {
                        uploadSocket.close();
                    } catch (IOException e) {
                    }
                }

            }
        }

        private void write(String command) throws IOException {
            pw.println(command);
            pw.flush();
            log("SENT: " + command);
        }

        private String read() throws IOException {
            final String response = input.readLine();
            log("RECV: " + response);
            return response;
        }

        private void expect(String response, String expectedCode) throws IOException {
            if (!response.startsWith(expectedCode)) {
                throw new IOException("Expected " + expectedCode + " got " + response);
            }
        }

        private String writeAndRead(String command) throws IOException {
            write(command);
            return read();
        }

        private void writeAndExpect(String command, String expectedCode) throws IOException {
            expect(writeAndRead(command),expectedCode);
        }

        @Override
        protected void onPostExecute(String str) {
            if (!"OK".equalsIgnoreCase(str)) {
                messageView.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.INVISIBLE);
                showError(str);
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("\n");
                        textView.append(getString(R.string.menu_installvdk_completed));
                        textView.append("\n");

                        new AlertDialog.Builder(me)
                                .setTitle(getString(R.string.enjoy))
                                .setMessage(getString(R.string.menu_installvdk_completed) + destFilePath)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        onPause();
                                        finish();
                                    }
                                })
                                .show();
                    }
                });
            }
        }
    }
}