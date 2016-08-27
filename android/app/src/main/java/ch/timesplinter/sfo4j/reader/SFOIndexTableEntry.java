package ch.timesplinter.sfo4j.reader;

import java.io.IOException;
import java.io.InputStream;

import ch.timesplinter.sfo4j.common.SFOUtilities;


public class SFOIndexTableEntry {
	public final static int INDEX_TABLE_ENTRY_LENGTH = 16;
	
	private short offsetKeyNameInKeyTable;
	private byte dataAlignmentRequirements;
	private byte dataTypeValue;
	private int sizeValueData;
	private int sizeValueDataAndPadding;
	private int offsetDataValueInDataTable;
	
	/**
	 * Reads one entry of the indexTable and return it's values in a SFOIndexTableEntry-object
	 * @param fIn
	 * @return SFOIndexTableEntry
	 * @throws IOException
	 */
	public static SFOIndexTableEntry readEntry(InputStream fIn) throws IOException {
		SFOIndexTableEntry sfoIndexTableEntry = new SFOIndexTableEntry();
		
		byte[] tempByteArray1 = new byte[1];
		byte[] tempByteArray2 = new byte[2];
		byte[] tempByteArray4 = new byte[4];
		
		// read offsetKeyNameInKeyTable
		fIn.read(tempByteArray2,0,2);
		sfoIndexTableEntry.setOffsetKeyNameInKeyTable(SFOUtilities.byteArrayReverseToShort(tempByteArray2));
		
		// read dataAlignmentRequirements
		fIn.read(tempByteArray1,0,1);
		sfoIndexTableEntry.setDataAlignmentRequirements(tempByteArray1[0]);
		
		// read dataTypeValue
		fIn.read(tempByteArray1,0,1);
		sfoIndexTableEntry.setDataTypeValue(tempByteArray1[0]);
		
		
		// read sizeValueData
		fIn.read(tempByteArray4,0,4);
		sfoIndexTableEntry.setSizeValueData(SFOUtilities.byteArrayReverseToInt(tempByteArray4));
		
		// read sizeValueDataAndPadding
		fIn.read(tempByteArray4,0,4);
		sfoIndexTableEntry.setSizeValueDataAndPadding(SFOUtilities.byteArrayReverseToInt(tempByteArray4));
		
		// read offsetDataValueInDataTable
		fIn.read(tempByteArray4,0,4);
		sfoIndexTableEntry.setOffsetDataValueInDataTable(SFOUtilities.byteArrayReverseToInt(tempByteArray4));
		
		return sfoIndexTableEntry;
	}

	public short getOffsetKeyNameInKeyTable() {
		return offsetKeyNameInKeyTable;
	}

	public void setOffsetKeyNameInKeyTable(short offsetKeyNameInKeyTable) {
		this.offsetKeyNameInKeyTable = offsetKeyNameInKeyTable;
	}

	public byte getDataAlignmentRequirements() {
		return dataAlignmentRequirements;
	}

	public void setDataAlignmentRequirements(byte dataAlignmentRequirements) {
		this.dataAlignmentRequirements = dataAlignmentRequirements;
	}

	public byte getDataTypeValue() {
		return dataTypeValue;
	}

	public void setDataTypeValue(byte dataTypeValue) {
		this.dataTypeValue = dataTypeValue;
	}

	public int getSizeValueData() {
		return sizeValueData;
	}

	public void setSizeValueData(int sizeValueData) {
		this.sizeValueData = sizeValueData;
	}

	public int getSizeValueDataAndPadding() {
		return sizeValueDataAndPadding;
	}

	public void setSizeValueDataAndPadding(int sizeValueDataAndPadding) {
		this.sizeValueDataAndPadding = sizeValueDataAndPadding;
	}

	public int getOffsetDataValueInDataTable() {
		return offsetDataValueInDataTable;
	}

	public void setOffsetDataValueInDataTable(int offsetDataValueInDataTable) {
		this.offsetDataValueInDataTable = offsetDataValueInDataTable;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("== SFO IndexTable Entry ==\n")
		.append("offsetKeyNameInKeyTable:    ").append(offsetKeyNameInKeyTable).append("\n")
		.append("dataAlignmentRequirements:  ").append(dataAlignmentRequirements).append("\n")
		.append("dataTypeValue:              ").append(dataTypeValue).append("\n")
		.append("sizeValueData:              ").append(sizeValueData).append("\n")
		.append("sizeValueDataAndPadding:    ").append(sizeValueDataAndPadding).append("\n")
		.append("offsetDataValueInDataTable: ").append(offsetDataValueInDataTable).append("\n");
	
		return sb.toString();
	}
}
