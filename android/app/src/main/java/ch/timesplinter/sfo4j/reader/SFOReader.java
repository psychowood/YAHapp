package ch.timesplinter.sfo4j.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ch.timesplinter.sfo4j.common.SFODataValue;
import ch.timesplinter.sfo4j.common.SFOUtilities;

/**
 * This class reads a SFO file and return the file content as follows:
 *  - getKeyTableEntryList(): The titles of the dataValues
 *  - getValueTableEntryList(): The values of the SFO file
 *  - getKeyValueMap(): A HashMap with the title as key and the dataValue as value
 *  
 * This class is copyrighted by TiMESPLiNTER (timesplinter.ch) 2011. Feel free
 * to modify this class and use it in your projects for free.
 * Please contact me if you made an important change or fixed/detected any bug.
 * 
 * @author TiMESPLiNTER
 * @version 1.1
 *
 */
public class SFOReader {
	public static final int HEADER_SIZE = 20;

	private SFOHeader sfoHeader;
	private List<SFOIndexTableEntry> indexTableEntryList = new ArrayList<SFOIndexTableEntry>();
	private List<String> keyTableEntryList = new ArrayList<String>();
	private List<SFODataValue> valueTableEntryList = new ArrayList<SFODataValue>();
	private Map<String,SFODataValue> keyValueMap = new TreeMap<String,SFODataValue>();
	
	public SFOReader(String sfoFile) throws IOException {
		File inFile = new File(sfoFile);
		FileInputStream fIn;

		fIn = new FileInputStream(inFile);
		parse(fIn);
		fIn.close();
	}

	public SFOReader(InputStream fIn) throws IOException {
		parse(fIn);
	}
	
	/**
	 * parse the submitted inputStream and
	 * fills the different ArrayLists and HashMaps
	 * @throws IOException 
	 */
	private void parse(InputStream fIn) throws IOException {
		// sfoHeader lesen
					sfoHeader = SFOHeader.read(fIn);
		
		for(int i = 0; i < sfoHeader.getNumberDataItems(); i++) {
			SFOIndexTableEntry sfoIndexEntry = SFOIndexTableEntry.readEntry(fIn);
			indexTableEntryList.add(sfoIndexEntry);
		}
		
		// Zum KeyTable Anfang springen 
		// (offset der KeyTabelle - Header-Länge - Anzahl * IndexEntry Länge = restl. zu ignorierende Bytes)
		int skipBytesToKeyTable = sfoHeader.getOffsetKeyTable()-HEADER_SIZE-(sfoHeader.getNumberDataItems()*SFOIndexTableEntry.INDEX_TABLE_ENTRY_LENGTH);
		fIn.skip(skipBytesToKeyTable);
		
		// read KeyTable
		SFOKeyTableEntry sfoKeyTableEntry = new SFOKeyTableEntry();
		for(int i = 0; i < sfoHeader.getNumberDataItems(); i++) {
			keyTableEntryList.add(sfoKeyTableEntry.readEntry(fIn));
		}
		
		long skipBytesToValueTable = sfoHeader.getOffsetValueTable()-sfoHeader.getOffsetKeyTable()-sfoKeyTableEntry.getKeyTableLength();
		fIn.skip(skipBytesToValueTable);
		
		// read ValueTable
		SFOValueTableEntry sfoValueTableEntry = new SFOValueTableEntry();
		for(int i = 0; i < sfoHeader.getNumberDataItems(); i++) {
			valueTableEntryList.add(new SFODataValue(sfoValueTableEntry.readEntry(fIn,indexTableEntryList.get(i)),indexTableEntryList.get(i).getDataTypeValue()));
		}
		
		for(int i = 0; i < keyTableEntryList.size(); i++) {
			keyValueMap.put(keyTableEntryList.get(i), valueTableEntryList.get(i));
		}

	}

	/**
	 * Returns the keys found in the sfo-File
	 * 
	 * @return List<String>
	 */
	public List<String> getKeyTableEntryList() {
		return keyTableEntryList;
	}

	/**
	 * Returns the dataValues found in the sfo-File
	 * 
	 * @return List<String>
	 */
	public List<SFODataValue> getValueTableEntryList() {
		return valueTableEntryList;
	}
	
	/**
	 * Returns the title/dataValues found in the sfo-File.
	 * Key of the Map is the title and value of the Map is the dataValue
	 * that fits to the title.
	 * 
	 * @return Map<String, String>
	 */
	public Map<String, SFODataValue> getKeyValueMap() {
		return keyValueMap;
	}
	
	/**
	 * Returns the value as an utf8-encoded string
	 * 
	 * @param key
	 * @return String
	 */
	public String getValueAsString(String key) {
		return SFOUtilities.byteArrayToString(keyValueMap.get(key).getDataValue(),"UTF8");
	}
	
	/**
	 * Returns the value as an integer
	 * 
	 * @param key
	 * @return Integer
	 */
	public int getValueAsInt(String key) {
		return SFOUtilities.byteArrayReverseToInt(keyValueMap.get(key).getDataValue());
	}
	
	/**
	 * Returns the value as plain byte[]-Array
	 * 
	 * @param key
	 * @return byte[]
	 */
	public byte[] getValue(String key) {
		return keyValueMap.get(key).getDataValue();
	}
	
	/**
	 * Returns the data type of the data element at key
	 * @param key
	 * @return byte
	 */
	public byte getDataType(String key) {
		return keyValueMap.get(key).getDataType();
	}

	/*public static void main(String[] args) {
		SFOReader sfoReader = new SFOReader("C:/Data/PARAM1.SFO");
		
		for(Map.Entry<String, SFODataValue> entry : sfoReader.getKeyValueMap().entrySet()){
			System.out.print(entry.getKey() + " = ");
			if(entry.getValue().getDataType() == SFODataValue.DATATYPE_STRING) {
				System.out.println(entry.getValue().toString());
			} else if(entry.getValue().getDataType() == SFODataValue.DATATYPE_NUMBER) {
				System.out.println(entry.getValue().toInt());
			} else {
				System.out.println(entry.getValue().toBytes());
			}
		}
	}*/
}
