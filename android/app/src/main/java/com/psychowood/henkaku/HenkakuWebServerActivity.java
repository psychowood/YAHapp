package com.psychowood.henkaku;

import android.app.Activity;
import android.content.res.AssetManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fi.iki.elonen.NanoHTTPD;

public class HenkakuWebServerActivity extends Activity {
    private static final String TAG = "HENdroid";
    private static final int PORT = 8357;
    private TextView hello;
    private TextView textIpaddr;
    private MyHTTPD server;
    private Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        hello = (TextView) findViewById(R.id.hello);
        textIpaddr = (TextView) findViewById(R.id.ipaddr);

        Button buttonOne = (Button) findViewById(R.id.goodbye);
        buttonOne.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                onPause();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        List<InetAddress> addresses = getLocalIpV4Addressese();

        String welcomeText;

        if (addresses == null || addresses.size() == 0) {
            welcomeText = getString(R.string.noNetworkAccess);
        } else if (addresses.size() > 1){
            welcomeText = getString(R.string.connectAtTheseAddresses);
        } else {
            welcomeText = getString(R.string.connectAtThisAddress);
        }

        for (InetAddress inetAddress : addresses) {
            welcomeText = welcomeText + "http://"
                    //+ String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff))
                    + inetAddress.getHostAddress()
                    + ":" + PORT + "\n";
        }

        textIpaddr.setText(welcomeText);

        try {
            server = new MyHTTPD();
            server.start();
        } catch (IOException e) {
            textIpaddr.append(getString(R.string.errorStartingServer) + "\n");
            textIpaddr.append(Log.getStackTraceString(e));
        }
    }

    public List<InetAddress> getLocalIpV4Addressese() {
        List<InetAddress> addresses = new ArrayList<>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        Log.d(TAG,"inetAddress " + inetAddress.getHostAddress());
                        addresses.add(inetAddress);
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG,"Exception getting ip", ex);
        }
        return addresses;
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (server != null)
            server.stop();
    }

    private static final String EXPLOIT_HTML = "exploit.html";
    private static final String PAYLOAD_JS = "payload.js";
    private static final String LOADER_BIN = "loader.rop.bin";
    private static final String EXPLOIT_BIN = "exploit.rop.bin";

    private static final String STAGE2_PATH = "stage2/";
    private static final String PKG_REFIX_PATH = "pkg";

    private class MyHTTPD extends NanoHTTPD {
        public MyHTTPD() throws IOException {
            super(PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {
            Log.d(TAG,"Request: " + session.getUri());
            final StringBuilder buf = new StringBuilder();
            Map<String, String> header = session.getHeaders();
            for (Entry<String, String> kv : header.entrySet())
                buf.append(kv.getKey() + " : " + kv.getValue() + "\n");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    textIpaddr.append(buf);
                }
            });
            InputStream resInputStream = null;
            String resPath = EXPLOIT_HTML;
            String resMimeType = MIME_HTML;
            try {
                AssetManager assetManager = getAssets();

                if (session.getUri().length() > 1) {
                    resPath = session.getUri().substring(1);
                    resMimeType = NanoHTTPD.getMimeTypeForFile(resPath);
                }


                if (!resPath.endsWith(STAGE2_PATH)) {
                    //Stage1

                    if (resPath.equalsIgnoreCase(PAYLOAD_JS)) {
                        String stage2Url = "http://192.168.1.30:4000/";
                        //http://" + session.getHeaders().get("host") + "/" + STAGE2_PATH;
                        /*
                            Stage1
                            PAYLOAD_JS is generated as
                            - LOADER_BIN
                            - Write exploit URL
                            - Preprocess as JS and serve to client
                         */
                        InputStream loaderInputStream = assetManager.open(LOADER_BIN);

                        ByteArrayOutputStream stage1ToProcess = new ByteArrayOutputStream();
                        HENprocess.writePkgUrl(
                                loaderInputStream,
                                stage1ToProcess,
                                stage2Url
                        );

                        ByteArrayOutputStream stage1Processed = new ByteArrayOutputStream();
                        HENprocess.preprocess(
                                new ByteArrayInputStream(stage1ToProcess.toByteArray()),
                                stage1Processed,
                                true
                        );

                        resInputStream = new ByteArrayInputStream(stage1Processed.toByteArray());

                    } else {
                        // EXPLOIT_HTML and everything else will be served as is
                        resInputStream = assetManager.open(resPath);
                    }
                } else {

                    //Stage2
                    int num_args = 7;

                    long[] args = new long[num_args+1];
                    args[0] = 0;

                    final Map<String, List<String>> params = session.getParameters();
                    for (int i = 1; i <= num_args; ++i) {
                        final String param = "a" + i;
                        final List<String> val = params.get(param);
                        if (val == null || val.size() == 0) {
                            throw new Exception("missing param" + param);
                        }

                        args[i] = Long.parseLong(val.get(0), 16);
                    }

                    InputStream exploitInputStream = assetManager.open(EXPLOIT_BIN);
                    ByteArrayOutputStream stage2ToProcess = new ByteArrayOutputStream();
                    HENprocess.preprocess(
                            exploitInputStream,
                            stage2ToProcess,
                            false
                    );

                    String packageUrl = "http://" + session.getHeaders().get("host") + "/" + PKG_REFIX_PATH;
                    ByteArrayOutputStream stage2Processed = new ByteArrayOutputStream();
                    HENprocess.writePkgUrl(
                            new ByteArrayInputStream(stage2ToProcess.toByteArray()),
                            stage2Processed,
                            packageUrl
                    );

                    byte[] payload = stage2Processed.toByteArray();


                    //payload = HENprocess.readAndClose(assetManager.open("stage2-go.bin"));
                    long[] source = HENprocess.getLongs(payload,payload.length);
                    byte[] relocs = Arrays.copyOf(payload,payload.length);

                    long size_words = source[0];

                    long dsize = source[1 + 0x10/4];
                    long csize = source[1 + 0x20/4];

                    long code_base = args[1];
                    long data_base = args[1] + csize;

                    for (int i=1; i<size_words; ++i) {
                        long add = 0;
                        byte x = relocs[(int)size_words * 4 + 4 + i - 1];

                        if (x == 1) {
                            add = data_base;
                        } else if (x != 0) {
                            //if (!isset($rgs, $x))
                            //    die("broken reloc");
                            add = args[x];
                        }

                        source[i] += add;
                    }

                    long[] data = Arrays.copyOfRange(source, 1 + 0x40 / 4, (int)dsize / 4);
                    long[] code = Arrays.copyOfRange(source, 1 + (0x40 + (int)dsize) / 4, (int)csize / 4);

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    for (int x=0; x < data.length; x++) {
                        byte[] outData = ByteBuffer.allocate(4)
                                .putInt((int)data[x])
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .array();
                        //for(int j = outData.length; j > 0; j--) {
                        //    out.write(outData[j-1]);
                        //}
                        out.write(outData);
                    }

                    for (int x=0; x < code.length; x++) {
                        byte[] outData = ByteBuffer.allocate(4)
                                .putInt((int)code[x])
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .array();
                        //for(int j = outData.length; j > 0; j--) {
                        //    out.write(outData[j-1]);
                        //}
                        out.write(outData);
                    }

                    resInputStream = new ByteArrayInputStream(out.toByteArray());
                }

            } catch (IOException ioe) {
                resInputStream = null;
                Log.e(TAG,"Error serving files",ioe);
            } catch (Exception ioe) {
                resInputStream = null;
                Log.e(TAG,"Error processing files",ioe);
            }

            Log.d(TAG,"Should serve " + resPath);
            if (resInputStream != null) {
                return NanoHTTPD.newChunkedResponse(Response.Status.OK, resMimeType, resInputStream);
            } else {
                final String html = "<html><head><head><body><h1>Sorry, something went wrong</h1><p></p></body></html>";
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML, html);
            }

        }
    }
}