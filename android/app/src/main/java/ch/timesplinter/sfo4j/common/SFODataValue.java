package ch.timesplinter.sfo4j.common;

public class SFODataValue {
	public static final byte DATATYPE_BINARY = 0;
	public static final byte DATATYPE_STRING = 2;
	public static final byte DATATYPE_NUMBER = 4;
	
	private byte dataType;
	private byte[] dataValue;
	
	public SFODataValue(byte[] dataValue, byte dataType) {
		this.dataValue = dataValue;
		this.dataType = dataType;
	}

	public byte getDataType() {
		return dataType;
	}

	public void setDataType(byte dataType) {
		this.dataType = dataType;
	}

	public byte[] getDataValue() {
		return dataValue;
	}

	public void setDataValue(byte[] dataValue) {
		this.dataValue = dataValue;
	}
	
	public byte[] toBytes() {
		return dataValue;
	}
	
	public String toString() {
		if(dataType != DATATYPE_STRING)
			return null;
		
		return SFOUtilities.byteArrayToString(dataValue,"UTF8");
	}
	
	public int toInt() {
		if(dataType != DATATYPE_NUMBER)
			return -1;
		
		return SFOUtilities.byteArrayReverseToInt(dataValue);
	}
}
