package com.psychowood.henkaku;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author psychowood
 */
public class HENprocessUnitTest {
    @Test
    public void runBuildScript() throws Exception {
        final String repositoryRoot = "/Users/gg/dev/github/henkaku/"; //Set to repository repositoryRoot

        final String stage1Url = "http://site.domain:1234/";
        final String stage2Url = "http://othersite.mydomain:8000/pkg";

        final String exploitBinPath = "exploit.rop.bin";
        final String stage2BinPath = "host/stage2.bin";
        final String stage1BinPath = "host/stage1.bin";
        final String payloadJsPath = "host/payload.js";

        String[] argv;

        copyFile(repositoryRoot+"loader.rop.bin",repositoryRoot+ stage1BinPath); //Needed for Step 2

        argv = new String[] {"preprocess",repositoryRoot+ exploitBinPath,repositoryRoot+ stage2BinPath}; //Step 1
        HENprocess.main(argv);

        argv = new String[] {"write_pkg_url",repositoryRoot+ stage1BinPath, stage1Url}; //Step 2
        HENprocess.main(argv);

        argv = new String[] {"write_pkg_url",repositoryRoot+ stage2BinPath, stage2Url}; //Step 3
        HENprocess.main(argv);

        argv = new String[] {"preprocess",repositoryRoot+ stage1BinPath,repositoryRoot+ payloadJsPath}; //Step 4
        HENprocess.main(argv);
    }

    private static void copyFile(String source, String dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
}