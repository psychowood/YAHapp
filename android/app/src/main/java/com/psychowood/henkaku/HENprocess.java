package com.psychowood.henkaku;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;


/***
 * Really quick'n'dirty java implementation for HENkaku build scripts preprocess.py and write_pkg_url.py
 * https://github.com/henkaku/henkaku
 * I mean, REALLY. Most of the stuff was based on a Python2 conversion from the Python3 scripts, then
 * more or less reverse-engineered for all the byte-stuff mangling that is going on in the scripts.
 * Seems to be giving the very same results (identical files) of the original scripts.
 *
 * @author: psychowood
 * @see <a href="https://github.com/psychowood/henkaku">https://github.com/psychowood/henkaku</a>
 */
public class HENprocess {

    private static boolean debug = false;

    private static final byte[] URL_PLACEHOLDER = getArrayOfByte((byte) 'x',256);

    private static byte[] getArrayOfByte(byte b,int length) {
        byte[] arr = new byte[length];
        for (int i=0;i<length; i++) {
            arr[i] = b;
        }
        return arr;
    }

    public HENprocess() {
        super();
    }

    public static void main(String[] argv)
    {
        if (argv.length < 3) {
            logErr("Usage for write_pkg_url: java HENprocess write_pkg_url filename url");
            logErr("Usage for preprocess: java HENprocess preprocess urop.bin output-payload.js");
            System.exit(-2);
        }

        debug = debug || (argv.length > 3);

        final String operation = argv[0];
        OutputStream output = null;

        try {
            if (operation.equalsIgnoreCase("write_pkg_url")) {
                final String srcFile = argv[1];
                final String url = argv[2];
                byte[] contents = read(srcFile) ;
                output = new BufferedOutputStream(new FileOutputStream(srcFile));
                writePkgUrl(contents, output, url);
            } else if (operation.equalsIgnoreCase("preprocess")) {
                final String uropFile = argv[1];
                final String outputFile = argv[2];
                byte[] urop = read(uropFile);
                output = new BufferedOutputStream(new FileOutputStream(outputFile));
                boolean isJs = outputFile.endsWith(".js");
                preprocess(urop, output, isJs);
            } else {
                throw new Exception("Unsupported operation: " + operation);
            }
        } catch (Exception e) {
            logErr(e);
            System.exit(-2);
        } finally {
            if (output != null) {
                try { output.close(); } catch(Exception e) {}
            }
        }

    }

    /***
     * preprocess.py script, does the HENkaku magic. Don't ask.
     * @param urop the file contents to be mangled
     * @param output write here the output
     * @param jsOutput true if it should output the payload.js file
     * @throws Exception
     */
    public static void preprocess(byte[] urop, OutputStream output, boolean jsOutput) throws Exception {
        while (urop.length % 4 != 0) {
            final int N = urop.length;
            urop = Arrays.copyOf(urop, N + 1);
            urop[N] = (byte) 0;
        }

        int header_size = 0x40;
        int dsize = u32(urop, 0x10);
        int csize = u32(urop, 0x20);
        int reloc_size = u32(urop, 0x30);
        int symtab_size = u32(urop, 0x38);

        if (csize % 4 != 0) {
            throw new Exception("csize % 4 != 0???");
        }

        int reloc_offset = header_size + dsize + csize;
        int symtab = reloc_offset + reloc_size;
        int strtab = symtab + symtab_size;

        int symtab_n = getFloor(symtab_size, 8);

        Map<Integer,String> reloc_map = new HashMap<Integer,String>();

        for (int x=0; x < symtab_n; x++) {
            int sym_id = u32(urop, symtab + 8 * x);
            int str_offset = u32(urop, symtab + 8 * x + 4);
            int begin = str_offset;
            int end = str_offset;
            while (urop[end] != 0) {
                end += 1;
            }
            String name = new String(Arrays.copyOfRange(urop, begin, end),"ascii");
            reloc_map.put(sym_id,name);
        }

        // mapping between roptool (symbol_name, reloc_type) and exploit hardcoded types
        // note that in exploit type=0 means no relocation
        Map<SimpleEntry<String, Integer>,Integer> reloc_type_map = new HashMap<SimpleEntry<String, Integer>,Integer>();

        reloc_type_map.put(new SimpleEntry<String, Integer>("rop.data",0),1);       // dest += rop_data_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceWebKit",0),2);      // dest += SceWebKit_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceLibKernel",0),3);   // dest += SceLibKernel_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceLibc",0),4);        // dest += SceLibc_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceLibHttp",0),5);     // dest += SceLibHttp_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceNet",0),6);         // dest += SceNet_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceAppMgr",0),7);      // dest += SceAppMgr_base

        // we don't need symtab/strtab/relocs
        int want_len = 0x40 + dsize + csize;
        byte[] relocs = getArrayOfByte((byte)0, getFloor(want_len, 4));

        int reloc_n = getFloor(reloc_size, 8);

        for (int x=0; x < reloc_n; x++) {
            int reloc_type = u16(urop, reloc_offset + 8 * x);
            int sym_id = u16(urop, reloc_offset + 8 * x + 2);
            int offset = u32(urop, reloc_offset + 8 * x + 4);
            String err = null;
            //log("type " + reloc_type + " sym " + reloc_map.get(sym_id) + " offset " + offset);
            if (offset % 4 != 0) {
                err = "offset % 4 != 0???";
            }
            int relocsIndex = getFloor(offset, 4);
            if (relocs[relocsIndex] != 0) {
                err = "symbol relocated twice, not supported";
            }
            Integer wk_reloc_type = reloc_type_map.get(new SimpleEntry<String, Integer>(reloc_map.get(sym_id),reloc_type));
            if (wk_reloc_type == null) {
                err = "unsupported relocation type";
            }
            if (err != null) {
                //logErr("type " + reloc_type + " sym " + reloc_map.get(sym_id) + " offset " + offset);
                throw new Exception(err);
            }
            relocs[relocsIndex] = wk_reloc_type.byteValue();
        }

        long[] urop_js = new long[want_len/4];
        for (int x=0, y=0; x < want_len; x = x+4, y++) {
            long val = (long) u32(urop, x);
            if (val < 0) {
                val = -1*((long) (Integer.MAX_VALUE+1)*2-val);
            }
            urop_js[y] = val;
        }

        if (jsOutput) {
            output.write('\n');
            output.write("payload = [".getBytes());

            for (int x=0; x < urop_js.length; x++) {
                output.write(Long.toString(urop_js[x]).getBytes());
                if (x!= urop_js.length-1) {
                    output.write(',');
                }
            }

            output.write("];\n".getBytes());
            output.write("relocs = [".getBytes());

            for (int x=0; x < relocs.length; x++) {
                output.write(Long.toString(relocs[x]).getBytes());
                if (x!= relocs.length-1) {
                    output.write(',');
                }
            }

            output.write("];\n".getBytes());
        } else {
            long pos = 0;
            byte[] data = ByteBuffer.allocate(4)
                    .putInt(getFloor(want_len, 4))
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .array();
            for(int j = data.length; j > 0; j--) {
                output.write(data[j-1]);
            }
            pos = pos + 4;

            for (int x=0; x < urop_js.length; x++) {
                data = ByteBuffer.allocate(4)
                        .putInt((int)urop_js[x])
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .array();
                for(int j = data.length; j > 0; j--) {
                    output.write(data[j-1]);
                }
                pos = pos + 4;
            }

            for (int x=0; x < relocs.length; x++) {
                data = ByteBuffer.allocate(1)
                        .put(relocs[x])
                        .array();
                output.write(data);
                pos = pos + 1;
            }
            while (pos % 4 != 0 ) {
                output.write('\0');
                pos = pos +1;
            }
        }

    }

    private static int getFloor(double value, int divider) {
        return (int) Math.floor( value / divider );
    }

    private static int u32(byte[] data,int offset) {
        byte[] fourBytes = Arrays.copyOfRange(data,offset,offset+4);
        final int anInt = (ByteBuffer.wrap(fourBytes)
                .order(ByteOrder.LITTLE_ENDIAN).getInt());
        return (int) anInt;
    }

    private static int u16(byte[] data, int offset) {
        byte[] fourBytes = Arrays.copyOfRange(data,offset,offset+2);
        final int anInt = (ByteBuffer.wrap(fourBytes)
                .order(ByteOrder.LITTLE_ENDIAN).getShort());
        return (int) anInt;
    }

    /***
     * Write the given URL (with lentgh below 255 chars) in the contents array, writing the result
     * in output
     * @param contents the byte[] to search for the placeholder
     * @param output where to write the byte[] with the injected url
     * @param url the URL to inject
     * @throws Exception in case of url.length > 255, of placeholder not found or if writing to the output stream fails
     */
    private static void writePkgUrl(byte[] contents, OutputStream output, String url) throws Exception {
        if (url.length() >= 255) {
            throw new Exception("url must be at most 255 characters!");
        }

        Integer pos = bytesIndexOf(contents,URL_PLACEHOLDER,0);

        if (pos < 0) {
            throw  new Exception("URL placeholder not found!");
        }

        try {
            output.write(Arrays.copyOfRange(contents,0,pos));
            output.write(url.getBytes());

            byte[] chars = new byte[256 - url.length()];
            Arrays.fill(chars, (byte) 0);
            output.write(chars);

            output.write(Arrays.copyOfRange(contents,pos+256,contents.length));
        }
        catch(IOException ex){
            throw  new Exception(ex);
        }
    }

    /**
     * Searchs for a subarray in a given array, starting from a specific index
     * @param Source the source array to search in
     * @param Search the array to look for in Source
     * @param fromIndex the starting index
     * @return The Search arrau position in Source, or -1 if not found
     */
    private static Integer bytesIndexOf(byte[] Source, byte[] Search, int fromIndex) {
        boolean Find = false;
        int i;
        for (i = fromIndex;i<Source.length-Search.length;i++){
            if(Source[i]==Search[0]){
                Find = true;
                for (int j = 0;j<Search.length;j++){
                    if (Source[i+j]!=Search[j]){
                        Find = false;
                    }
                }
            }
            if(Find){
                break;
            }
        }
        if(!Find){
            return new Integer(-1);
        }
        return  new Integer(i);
    }

    /***
     * Read the given binary file, and return its contents as a byte array.
     * @param aInputFileName The filename
     * @return A byte[] with the file contents
     * @throws IOException
     */
    static byte[] read(String aInputFileName) throws IOException{
        File file = new File(aInputFileName);
        byte[] result = null;
        InputStream input =  new BufferedInputStream(new FileInputStream(file));
        result = readAndClose(input);
        return result;
    }

    /***
     * Read an input stream, and return it as a byte array.
     * @param aInput The InputStream to read
     * @return A byte[] with the InputStream contents
     * @throws IOException
     */
    static byte[] readAndClose(InputStream aInput) throws IOException {
        byte[] bucket = new byte[32*1024];
        ByteArrayOutputStream result = null;
        try {
            result = new ByteArrayOutputStream(bucket.length);
            int bytesRead = 0;
            while(bytesRead != -1){
                bytesRead = aInput.read(bucket);
                if(bytesRead > 0){
                    result.write(bucket, 0, bytesRead);
                }
            }
        }
        finally {
            aInput.close();
        }
        return result.toByteArray();
    }

    private static void logErr(Object aThing){
        log(aThing,true);
    }

    private static void log(Object aThing){
        log(aThing,false);
    }

    private static void log(Object aThing, boolean force){
        if (debug || force)
            System.out.println(String.valueOf(aThing));
    }

}
