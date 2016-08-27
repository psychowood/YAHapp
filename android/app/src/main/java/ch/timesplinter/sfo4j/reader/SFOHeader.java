package ch.timesplinter.sfo4j.reader;

import java.io.IOException;
import java.io.InputStream;

import ch.timesplinter.sfo4j.common.SFOUtilities;


public class SFOHeader {
	private String fileType;
	private String sfoVersion;
	private int offsetKeyTable;
	private int offsetValueTable;
	private int numberDataItems;
	
	public static SFOHeader read(InputStream fIn) throws IOException {
		SFOHeader sfoHeader = new SFOHeader();
		
		byte[] tempByteArray = new byte[4];
		
		// read FileType
		fIn.read(tempByteArray,0,4);
		sfoHeader.setFileType(SFOUtilities.byteArrayToString(tempByteArray));
		
		// read sfoVerion
		fIn.read(tempByteArray,0,4);
		sfoHeader.setSfoVersion(SFOUtilities.byteArrayToString(tempByteArray));
		
		// read offsetKeyTable
		fIn.read(tempByteArray,0,4);
		sfoHeader.setOffsetKeyTable(SFOUtilities.byteArrayReverseToInt(tempByteArray));
		
		// read offsetValueTable
		fIn.read(tempByteArray,0,4);
		sfoHeader.setOffsetValueTable(SFOUtilities.byteArrayReverseToInt(tempByteArray));
		
		// read numberDataItem
		fIn.read(tempByteArray,0,4);
		sfoHeader.setNumberDataItems(SFOUtilities.byteArrayReverseToInt(tempByteArray));
	
		return sfoHeader;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getSfoVersion() {
		return sfoVersion;
	}

	public void setSfoVersion(String sfoVersion) {
		this.sfoVersion = sfoVersion;
	}

	public int getOffsetKeyTable() {
		return offsetKeyTable;
	}

	public void setOffsetKeyTable(int offsetKeyTable) {
		this.offsetKeyTable = offsetKeyTable;
	}

	public int getOffsetValueTable() {
		return offsetValueTable;
	}

	public void setOffsetValueTable(int offsetValueTable) {
		this.offsetValueTable = offsetValueTable;
	}

	public int getNumberDataItems() {
		return numberDataItems;
	}

	public void setNumberDataItems(int numberDataItems) {
		this.numberDataItems = numberDataItems;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("== SFO Header Data ==\n")
		.append("fileType:         ").append(fileType).append("\n")
		.append("sfoVersion:       ").append(sfoVersion).append("\n")
		.append("offsetKeyTable:   ").append(offsetKeyTable).append("\n")
		.append("offsetValueTable: ").append(offsetValueTable).append("\n")
		.append("numberDataItems:  ").append(numberDataItems).append("\n");
	
		return sb.toString();
	}
}
