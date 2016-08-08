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

public class HENprocess {

    //static final String TAG = HENprocess.class.getCanonicalName();
    private static boolean debug = true;

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

        try {
            if (operation.equalsIgnoreCase("write_pkg_url")) {
                final String srcFile = argv[1];
                final String url = argv[2];
                writePkgUrl(srcFile, url);
            } else if (operation.equalsIgnoreCase("preprocess")) {
                final String uropFile = argv[1];
                final String outputFile = argv[2];
                preprocess(uropFile, outputFile);
            } else {
                throw new Exception("Unsupported operation: " + operation);
            }
        } catch (Exception e) {
            logErr(e);
            System.exit(-2);
        }

    }

    private static void preprocess(String uropFile, String filename) throws Exception {
        byte[] urop = read(uropFile);

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
            final String err = "csize % 4 != 0???";
            logErr(err);
            throw new Exception(err);
        }

        int reloc_offset = header_size + dsize + csize;
        int symtab = reloc_offset + reloc_size;
        //int strtab = symtab + symtab_size;

        int symtab_n = (int) Math.floor( ((double) symtab_size) / 8 );

        Map<Integer,String> reloc_map = new HashMap();

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
        Map<SimpleEntry<String, Integer>,Integer> reloc_type_map = new HashMap();

        reloc_type_map.put(new SimpleEntry<String, Integer>("rop.data",0),1);       // dest += rop_data_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceWebKit",0),2);      // dest += SceWebKit_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceLibKernel",0),3);   // dest += SceLibKernel_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceLibc",0),4);        // dest += SceLibc_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceLibHttp",0),5);     // dest += SceLibHttp_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceNet",0),6);         // dest += SceNet_base
        reloc_type_map.put(new SimpleEntry<String, Integer>("SceAppMgr",0),7);      // dest += SceAppMgr_base

        // we don't need symtab/strtab/relocs
        int want_len = 0x40 + dsize + csize;
        byte[] relocs = getArrayOfByte((byte)0,(int) Math.floor( ((double) want_len) / 4 ));

        int reloc_n = (int) Math.floor( ((double) reloc_size) / 8 );

        for (int x=0; x < reloc_n; x++) {
            int reloc_type = u16(urop, reloc_offset + 8 * x);
            int sym_id = u16(urop, reloc_offset + 8 * x + 2);
            int offset = u32(urop, reloc_offset + 8 * x + 4);
            String err = null;
            log("type " + reloc_type + " sym " + reloc_map.get(sym_id) + " offset " + offset);
            if (offset % 4 != 0) {
                err = "offset % 4 != 0???";
            }
            int relocsIndex = (int) Math.floor( ((double) offset) / 4 );
            if (relocsIndex != 0) {
                 err = "symbol relocated twice, not supported";
            }
            Integer wk_reloc_type = reloc_type_map.get(new SimpleEntry(reloc_map.get(sym_id),reloc_type));
            if (wk_reloc_type == null) {
                err = "unsupported relocation type";
            }
            if (err != null) {
                logErr("type " + reloc_type + " sym " + reloc_map.get(sym_id) + " offset " + offset);
                throw new Exception(err);
            }
            relocs[relocsIndex] = wk_reloc_type.byteValue();
        }

        int[] urop_js = new int[want_len];
        for (int x=0; x < want_len; x = x+4) {
            urop_js[x] = u32(urop, x);
        }

        String tpl_js = "payload = [{}];\n" +
                "relocs = [{}];";


        OutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(filename));
            StringBuffer buffer;
            if (filename.endsWith(".js")) {
                buffer = new StringBuffer();
                for (int x=0; x < urop_js.length; x++) {
                    buffer.append(urop_js[x]);
                    if (x!= urop_js.length-1) {
                        buffer.append(",");
                    }
                }
                tpl_js = tpl_js.replaceFirst("\\{\\}",buffer.toString());

                buffer = new StringBuffer();
                for (int x=0; x < relocs.length; x++) {
                    buffer.append(relocs[x]);
                    if (x!= relocs.length-1) {
                        buffer.append(",");
                    }
                }
                tpl_js = tpl_js.replaceFirst("\\{\\}",buffer.toString());

                output.write(tpl_js.getBytes());
            } else {
                throw new Exception(".js only");
            }
        }
        finally {
            output.close();
        }


        /*

	filename = argv[2]
	if filename.endswith(".js"):
		with open(filename, "w") as fout:
			tpl = tpl_js
			fout.write(tpl.format(",".join(str(x) for x in urop_js), ",".join(str(x) for x in relocs)))
	else:
		with open(filename, "wb") as fout:
			fout.write((want_len // 4).to_bytes(4, "little"))
			for word in urop_js:
				fout.write(word.to_bytes(4, "little"))
			for reloc in relocs:
				fout.write(reloc.to_bytes(1, "little"))
			while fout.tell() % 4 != 0:
				fout.write(b"\x00")


    def main():
	if len(argv) != 3:
		print("Usage: preprocess.py urop.bin output-payload.js")
		return -1
	with open(argv[1], "rb") as fin:
		urop = fin.read()

	while len(urop) % 4 != 0:
		urop += b"\x00"

	header_size = 0x40
	dsize = u32(urop, 0x10)
	csize = u32(urop, 0x20)
	reloc_size = u32(urop, 0x30)
	symtab_size = u32(urop, 0x38)

	if csize % 4 != 0:
		print("csize % 4 != 0???")
		return -2

	reloc_offset = header_size + dsize + csize
	symtab = reloc_offset + reloc_size
	strtab = symtab + symtab_size

	symtab_n = symtab_size // 8
	reloc_map = dict()
	for x in range(symtab_n):
		sym_id = u32(urop, symtab + 8 * x)
		str_offset = u32(urop, symtab + 8 * x + 4)
		begin = str_offset
		end = str_offset
		while urop[end] != 0:
			end += 1
		name = urop[begin:end].decode("ascii")
		reloc_map[sym_id] = name

	# mapping between roptool (symbol_name, reloc_type) and exploit hardcoded types
	# note that in exploit type=0 means no relocation
	reloc_type_map = {
		("rop.data", 0): 1,       # dest += rop_data_base
		("SceWebKit", 0): 2,      # dest += SceWebKit_base
		("SceLibKernel", 0): 3,   # dest += SceLibKernel_base
		("SceLibc", 0): 4,        # dest += SceLibc_base
		("SceLibHttp", 0): 5,     # dest += SceLibHttp_base
		("SceNet", 0): 6,         # dest += SceNet_base
		("SceAppMgr", 0): 7,      # dest += SceAppMgr_base
	}

	# we don't need symtab/strtab/relocs
	want_len = 0x40 + dsize + csize
	relocs = [0] * (want_len // 4)

	reloc_n = reloc_size // 8
	for x in range(reloc_n):
		reloc_type = u16(urop, reloc_offset + 8 * x)
		sym_id = u16(urop, reloc_offset + 8 * x + 2)
		offset = u32(urop, reloc_offset + 8 * x + 4)
		print_dbg = lambda: print("type {} sym {} offset {}".format(reloc_type, reloc_map[sym_id], offset))
		# print_dbg()
		if offset % 4 != 0:
			print_dbg()
			print("offset % 4 != 0???")
			return -2
		if relocs[offset // 4] != 0:
			print_dbg()
			print("symbol relocated twice, not supported")
			return -2
		wk_reloc_type = reloc_type_map.get((reloc_map[sym_id], reloc_type))
		if wk_reloc_type is None:
			print_dbg()
			print("unsupported relocation type")
			return -2
		relocs[offset // 4] = wk_reloc_type

	urop_js = [u32(urop, x) for x in range(0, want_len, 4)]

	filename = argv[2]
	if filename.endswith(".js"):
		with open(filename, "w") as fout:
			tpl = tpl_js
			fout.write(tpl.format(",".join(str(x) for x in urop_js), ",".join(str(x) for x in relocs)))
	else:
		with open(filename, "wb") as fout:
			fout.write((want_len // 4).to_bytes(4, "little"))
			for word in urop_js:
				fout.write(word.to_bytes(4, "little"))
			for reloc in relocs:
				fout.write(reloc.to_bytes(1, "little"))
			while fout.tell() % 4 != 0:
				fout.write(b"\x00")

     */
    }

    private static int u32(byte[] data,int offset) {
        /*
        def u32(data, offset):
        return struct.unpack(u"<I", data[offset:offset+4])[0]
        */

        byte[] fourBytes = Arrays.copyOfRange(data,offset,offset+4);
        final int anInt = ByteBuffer.wrap(data)
                .order(ByteOrder.LITTLE_ENDIAN).getInt();
        log("u32: offset " + offset + " - output " + anInt);
        return anInt;
    }

    private static int u16(byte[] data, int offset) {
        /*
        def u16(data, offset):
            return struct.unpack(u"<H", data[offset:offset+2])[0]
        */

        byte[] fourBytes = Arrays.copyOfRange(data,offset,offset+2);
        final int anInt = ByteBuffer.wrap(data)
                .order(ByteOrder.LITTLE_ENDIAN).getInt();
        log("u16: offset " + offset + " - output " + anInt);
        return anInt;
    }

    /*



    def to_bytes(n, length, endianess='big'):
    h = '%x' % n
            s = ('0'*(len(h) % 2) + h).zfill(length*2).decode('hex')
    return s if endianess == 'big' else s[::-1]
    */

    private static void writePkgUrl(String srcFile, String url) {
        if (url.length() >= 255) {
            log("url must be at most 255 characters!");
            System.exit(-2);
        }

        byte[] contents = read(srcFile) ;
        Integer pos = bytesIndexOf(contents,URL_PLACEHOLDER,0);

        if (pos < 0) {
            log("URL placeholder not found!");
            System.exit(-2);
        }

        log("Writing binary file...");
        try {
            OutputStream output = null;
            try {
                output = new BufferedOutputStream(new FileOutputStream(srcFile));
                output.write(Arrays.copyOfRange(contents,0,pos));
                output.write(url.getBytes());

                byte[] chars = new byte[256 - url.length()];
                Arrays.fill(chars, (byte) 0);
                output.write(chars);

                output.write(Arrays.copyOfRange(contents,pos+256,contents.length));
            }
            finally {
                output.close();
            }
        }
        catch(FileNotFoundException ex){
            log("File not found.");
        }
        catch(IOException ex){
            log(ex);
        }
    }

    /** Read the given binary file, and return its contents as a byte array.*/
    private static byte[] readPlainImpl(String aInputFileName){
        log("Reading in binary file named : " + aInputFileName);
        File file = new File(aInputFileName);
        log("File size: " + file.length());
        byte[] result = new byte[(int)file.length()];
        try {
            InputStream input = null;
            try {
                int totalBytesRead = 0;
                input = new BufferedInputStream(new FileInputStream(file));
                while(totalBytesRead < result.length){
                    int bytesRemaining = result.length - totalBytesRead;
                    //input.read() returns -1, 0, or more :
                    int bytesRead = input.read(result, totalBytesRead, bytesRemaining);
                    if (bytesRead > 0){
                        totalBytesRead = totalBytesRead + bytesRead;
                    }
                }
        /*
         the above style is a bit tricky: it places bytes into the 'result' array;
         'result' is an output parameter;
         the while loop usually has a single iteration only.
        */
                log("Num bytes read: " + totalBytesRead);
            }
            finally {
                log("Closing input stream.");
                input.close();
            }
        }
        catch (FileNotFoundException ex) {
            log("File not found.");
        }
        catch (IOException ex) {
            log(ex);
        }
        return result;
    }

    /** Read the given binary file, and return its contents as a byte array.*/
    static byte[] read(String aInputFileName){
        log("Reading in binary file named : " + aInputFileName);
        File file = new File(aInputFileName);
        log("File size: " + file.length());
        byte[] result = null;
        try {
            InputStream input =  new BufferedInputStream(new FileInputStream(file));
            result = readAndClose(input);
        }
        catch (FileNotFoundException ex){
            log(ex);
        }
        return result;
    }

    /**
     Read an input stream, and return it as a byte array.
     Sometimes the source of bytes is an input stream instead of a file.
     This implementation closes aInput after it's read.
     */
    static byte[] readAndClose(InputStream aInput){
        //carries the data from input to output :
        byte[] bucket = new byte[32*1024];
        ByteArrayOutputStream result = null;
        try  {
            try {
                //Use buffering? No. Buffering avoids costly access to disk or network;
                //buffering to an in-memory stream makes no sense.
                result = new ByteArrayOutputStream(bucket.length);
                int bytesRead = 0;
                while(bytesRead != -1){
                    //aInput.read() returns -1, 0, or more :
                    bytesRead = aInput.read(bucket);
                    if(bytesRead > 0){
                        result.write(bucket, 0, bytesRead);
                    }
                }
            }
            finally {
                aInput.close();
                //result.close(); this is a no-operation for ByteArrayOutputStream
            }
        }
        catch (IOException ex){
            log(ex);
        }
        return result.toByteArray();
    }

    /**
     Write a byte array to the given file.
     Writing binary data is significantly simpler than reading it.
     */
    void write(byte[] aInput, String aOutputFileName){
        log("Writing binary file...");
        try {
            OutputStream output = null;
            try {
                output = new BufferedOutputStream(new FileOutputStream(aOutputFileName));
                output.write(aInput);
            }
            finally {
                output.close();
            }
        }
        catch(FileNotFoundException ex){
            log("File not found.");
        }
        catch(IOException ex){
            log(ex);
        }
    }

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
