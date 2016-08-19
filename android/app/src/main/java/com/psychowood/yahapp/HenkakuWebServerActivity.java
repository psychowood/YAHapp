package com.psychowood.yahapp;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;

import com.psychowood.henkaku.HenkakuWebServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

public class HenkakuWebServerActivity extends TextStatusActivityBase {
    private static final String TAG = "HNKWebServerActivity";

    private static final String ASSETS_PREFIX      = "henkaku/";

    private HenkakuWebServer server;
    private Activity me;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        me = this;
    }

    @Override
    protected void onResume() {
        super.onResume();

        List<InetAddress> addresses = null;
        try {
            addresses = HenkakuWebServer.getLocalIpV4Addresses();
        } catch (SocketException e) {
            Log.e(TAG,"Error getting addresses",e);
            addresses = new ArrayList<>();
        }

        StringBuffer buf = new StringBuffer();
        if (addresses == null || addresses.size() == 0) {
            buf.append(getString(R.string.noNetworkAccess));
        } else if (addresses.size() > 1){
            buf.append(getString(R.string.connectAtTheseAddresses));
        } else {
            buf.append(getString(R.string.connectAtThisAddress));
        }
        buf.append("\n");
        buf.append("\n");

        try {
            server = new HenkakuWebServer(new HenkakuWebServer.WebServerHandler() {

                @Override
                public InputStream openResource(String resourceName) throws IOException {
                    return getAssets().open(ASSETS_PREFIX+resourceName);
                }

                @Override
                public void receivedRequest(final NanoHTTPD.IHTTPSession session) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            textView.append(session.getUri());
                            textView.append("\n");
                        }
                    });
                }

                @Override
                public void log(String tag, String s) {
                    Log.d(tag,s);
                }

                @Override
                public void log(String tag, String s, Exception ex) {
                    Log.e(tag,s,ex);
                }

                @Override
                public void done() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            textView.append("\n");
                            textView.append(getString(R.string.install_completed));
                            textView.append("\n");

                            new AlertDialog.Builder(me)
                                    .setTitle(getString(R.string.enjoy))
                                    .setMessage(getString(R.string.install_completed))
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
            });
            server.start();

            for (InetAddress inetAddress : addresses) {
                buf.append("http://"
                        + inetAddress.getHostAddress()
                        + ":" + server.getCurrentPort() + "\n");
            }

            buf.append("\n");
            buf.append(getString(R.string.serving));
            buf.append("\n");
            buf.append("\n");

            textView.setText(buf.toString());

        } catch (IOException e) {
            textView.append(getString(R.string.errorStartingServer) + "\n");
            textView.append(Log.getStackTraceString(e));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (server != null)
            server.stop();
    }
}