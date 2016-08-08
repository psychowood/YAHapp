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
        String root = "/Users/gg/dev/github/henkaku/"; //Set to repository root

        copyFile(root+"loader.rop.bin",root+"host/stage1.bin"); //Needed for Step 2

        String[] argv;

        argv = new String[] {"preprocess",root+"exploit.rop.bin",root+"host/stage2.bin"}; //Step 1
        HENprocess.main(argv);

        argv = new String[] {"write_pkg_url",root+"host/stage1.bin","http://site.domain:1234/"}; //Step 2
        HENprocess.main(argv);

        argv = new String[] {"write_pkg_url",root+"host/stage2.bin","http://othersite.mydomain:8000/pkg"}; //Step 3
        HENprocess.main(argv);

        argv = new String[] {"preprocess",root+"host/stage1.bin",root+"host/payload.js"}; //Step 4
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