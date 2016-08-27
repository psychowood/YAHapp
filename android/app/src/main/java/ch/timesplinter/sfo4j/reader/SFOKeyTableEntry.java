package ch.timesplinter.sfo4j.reader;

import java.io.IOException;
import java.io.InputStream;

public class SFOKeyTableEntry {
	public final static byte DELIMITER_BYTE = 0;
	private int keyTableLength;
	
	public SFOKeyTableEntry() {
		this.keyTableLength = 0;
	}
	
	/**
	 * Reads a key from the keyTable and return the value
	 * 
	 * @param fIn
	 * @return String
	 * @throws IOException
	 */
	public String readEntry(InputStream fIn) throws IOException {
		byte[] tempByteArray1 = new byte[1];
		StringBuilder sb = new StringBuilder();
		
		fIn.read(tempByteArray1, 0, 1);
		keyTableLength++;
		while(tempByteArray1[0] != DELIMITER_BYTE) {
			sb.append((char)tempByteArray1[0]);
			fIn.read(tempByteArray1, 0, 1);
			keyTableLength++;
		}
		
		return sb.toString();
	}
	
	/**
	 * Returns the keyTable-length in bytes
	 * @return Integer
	 */
	public int getKeyTableLength() {
		return this.keyTableLength;
	}
}
