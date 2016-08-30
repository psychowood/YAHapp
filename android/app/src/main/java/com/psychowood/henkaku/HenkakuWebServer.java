package com.psychowood.henkaku;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class HenkakuWebServer extends NanoHTTPD {

    private static final String TAG = "HNKWebServer";

    private static final String INDEX_HTML      = "index.html";

    private static final String STAGE1_PATH     = "stage1/";
    private static final String PAYLOAD_JS      = STAGE1_PATH + "payload.js";
    private static final String LOADER_BIN      = "loader.rop.bin";

    private static final String STAGE2_PATH     = "stage2/";
    private static final String EXPLOIT_BIN     = "exploit.rop.bin";

    private static final String PKG_PREFIX_PATH = "host/pkg";

    private static final String LAST_FILE = "pkg/sce_sys/livearea/contents/template.xml";

    private static int PORT = 8357;

    private WebServerHandler handler;

    public static void main(String[] argv) throws IOException {

        if (argv.length < 1) {
            System.out.println("Usage for serving files: java HenkakuWebServer path-to-asset-directory [port]");
            System.out.println(" with  path-to-asset-directory the directory containing all the needed files");
            System.exit(-2);
        }

        final String assetDir = argv[0];
        if (argv.length > 1) {
            try {
                PORT = Integer.parseInt(argv[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number " + argv[1]);
            }
        }

        WebServerHandler serverHandler = new HenkakuWebServer.WebServerHandler() {

            @Override
            public InputStream openResource(String resourceName) throws IOException {
                return new FileInputStream(assetDir + resourceName);
            }

            @Override
            public void receivedRequest(final NanoHTTPD.IHTTPSession session) {
                System.out.println(session.getUri());
            }

            @Override
            public void log(String tag, String s) {
                System.out.println(s);
            }

            @Override
            public void log(String tag, String s, Exception ex) {
                System.out.println(s);
                ex.printStackTrace();
            }

            @Override
            public void done() {
                System.out.println("======================");
                System.out.println("======= ENJOY! =======");
                System.out.println("======================");
            }
        };
        HenkakuWebServer server = new HenkakuWebServer(serverHandler);

        server.start(PORT,false);

        final List<InetAddress> addresses = getLocalIpV4Addresses();
        for (InetAddress inetAddress : addresses) {
            System.out.println("running at http://"+ inetAddress.getHostAddress()
                    + ":" + server.getCurrentPort());
        }


    }


        private HenkakuWebServer() {
        super(PORT);
    }

    public HenkakuWebServer(WebServerHandler handler) {
        super(PORT);
        this.handler = handler;
    }

    @Override
    public Response serve(IHTTPSession session) {
        handler.log(TAG, "Request: " + session.getUri());
        Map<String, String> headers = session.getHeaders();
        InputStream resInputStream = null;
        String resPath = INDEX_HTML;
        String resMimeType = MIME_HTML;
        try {

            handler.receivedRequest(session);

            if (session.getUri().length() > 1) {
                resPath = session.getUri().substring(1);
                resMimeType = NanoHTTPD.getMimeTypeForFile(resPath);
            }

            if (resPath.startsWith(STAGE1_PATH)) {
                //Stage1

                if (resPath.endsWith(PAYLOAD_JS)) {
                    String stage2Url = "http://" + headers.get("host") + "/" + STAGE2_PATH;

                        /*
                            Stage1
                            PAYLOAD_JS is generated as
                            - LOADER_BIN
                            - Write exploit URL
                            - Preprocess as JS and serve to client
                         */
                    InputStream loaderInputStream = handler.openResource(LOADER_BIN);

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
                    resInputStream = handler.openResource(resPath);
                }
            } else if (resPath.startsWith(STAGE2_PATH)){

                //Stage2
                int num_args = 7;

                Map<String,String> args = new HashMap<>();

                final Map<String, List<String>> params = session.getParameters();
                for (int i = 1; i <= num_args; ++i) {
                    final String param = "a" + i;
                    final List<String> val = params.get(param);
                    if (val != null || val.size() > 0) {
                        args.put(param,val.get(0));
                    }
                }

                InputStream exploitInputStream = handler.openResource(EXPLOIT_BIN);
                ByteArrayOutputStream stage2ToProcess = new ByteArrayOutputStream();
                HENprocess.preprocess(
                        exploitInputStream,
                        stage2ToProcess,
                        false
                );

                String packageUrl = "http://" + headers.get("host") + "/" + PKG_PREFIX_PATH;
                ByteArrayOutputStream stage2Processed = new ByteArrayOutputStream();
                HENprocess.writePkgUrl(
                        new ByteArrayInputStream(stage2ToProcess.toByteArray()),
                        stage2Processed,
                        packageUrl
                );

                byte[] payload = stage2Processed.toByteArray();

                byte[] out = HENprocess.patchExploit(payload,args);
                resInputStream = new ByteArrayInputStream(out);
            } else {
                //Static resources (site & pkg)
                resInputStream =  handler.openResource(resPath);
            }

        } catch (IOException ioe) {
            resInputStream = null;
            handler.log(TAG,"Error serving files",ioe);
        } catch (Exception ioe) {
            resInputStream = null;
            handler.log(TAG,"Error processing files",ioe);
        }

        handler.log(TAG,"Serving: " + resPath);
        if (resInputStream != null) {
            if (resPath.endsWith(LAST_FILE)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        handler.done();
                    }
                }).start();
            }
            return NanoHTTPD.newChunkedResponse(Response.Status.OK, resMimeType, resInputStream);
        } else {
            final String html = "<html><head><head><body><h1>Sorry, something went wrong</h1><p></p></body></html>";
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML, html);
        }

    }

    public static List<InetAddress> getLocalIpV4Addresses() throws SocketException {
        List<InetAddress> addresses = new ArrayList<>();
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    addresses.add(inetAddress);
                }
            }
        }
        return addresses;
    }

    public int getCurrentPort() {
        return PORT;
    }

    public interface WebServerHandler {
        public InputStream openResource(String resourceName) throws IOException;
        public void receivedRequest(IHTTPSession session);
        public void log(String tag, String s);
        public void log(String tag, String s, Exception ex);
        public void done();
    }
}