package ch.timesplinter.sfo4j.common;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;


public class SFOUtilities {
	/**
	 * Converts any byte[]-Array to a string with the specified encoding.
	 *
	 * @param byteArray
	 * @param encoding
	 * @return String
	 */
	public static String byteArrayToString(byte[] byteArray, String encoding) {
		String converted;
		Byte nullByte = 0;

		try {
			int i, j;
			for (i = j = 0; j < byteArray.length; ++j)
			  if (!nullByte.equals(byteArray[j])) byteArray[i++] = byteArray[j];
			byteArray = Arrays.copyOf(byteArray, i);


			converted = new String(byteArray, encoding);
		} catch (UnsupportedEncodingException e) {
			converted = byteArrayToString(byteArray);
		}

		return converted;
	}

	/**
	 * Converts any byte[]-Array to a string by casting the byte to a char
	 *
	 * @param byteArray
	 * @return String
	 */
	public static String byteArrayToString(byte[] byteArray) {
		StringBuilder sb = new StringBuilder();
		byte nullByte = 0;

		for(int i = 0; i < byteArray.length; i++) {
			if(byteArray[i] != nullByte)
				sb.append((char)byteArray[i]);
		}

		return sb.toString();
	}

	/**
	 * Reverse any byte[]-Array and converts it then to an int
	 *
	 * @param b
	 * @return Integer
	 */
	public static int byteArrayReverseToInt(byte[] b) {
		byte[] bTemp = byteArrayReverse(b);

		return byteArrayToInt(bTemp);
	}

	public static byte[] intToByteArrayReverse(int value) {
		byte[] newValue = intToByteArray(value);
		newValue = byteArrayReverse(newValue);

		return newValue;
	}

	/**
	 * The method to reverse a byte[]-Array
	 *
	 * @param b
	 * @return byte[]
	 */
	public static byte[] byteArrayReverse(byte[] b) {
		byte[] bTemp = new byte[b.length];

		for(int i = b.length-1, j = 0; i >= 0; i--, j++) {
			bTemp[j] = b[i];
		}

		return bTemp;
	}

	/**
	 * Returns any byte[]-Array as an int
	 *
	 * @param b
	 * @return Integer
	 */
	public static int byteArrayToInt(byte[] b) {
        return byteArrayToInt(b, 0);
    }

	/**
	 * Returns any byte[]-Array as an int from the given offset
	 *
	 * @param b
	 * @param offset
	 * @return Integer
	 */
	public static int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    public static byte[] intToByteArray(int value) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }

	public static byte[] shortToByteArrayReverse(int value) {
		byte[] newValue = shortToByteArray(value);
		newValue = byteArrayReverse(newValue);

		return newValue;
	}

	public static short byteArrayReverseToShort(byte[] b) {
		byte[] bTemp = byteArrayReverse(b);

		return byteArrayToShort(bTemp);
	}

	public static short byteArrayToShort(byte[] b) {
		return byteArrayToShort(b, 0);
	}

	public static short byteArrayToShort(byte[] b, int offset) {
        short value = 0;
        for (int i = 0; i < 2; i++) {
            int shift = (2 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

	public static byte[] shortToByteArray(int value) {
        byte[] b = new byte[2];
        for (int i = 0; i < 2; i++) {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }

    /**
	 * Replace bytes from newBytes with that one on source at the specified offset
	 *
	 * @param source
	 * @param newbytes
	 * @param offset
	 * @return byte[]
	 */
	public static byte[] replaceBytesInByteArray(byte[] source, byte[] newbytes, int offset) {
		byte[] newsource = source;

		for(int i = offset, j = 0; i < offset+newbytes.length; i++, j++) {
			newsource[i] = newbytes[j];
		}

		return newsource;
	}

	public static byte[] replaceByteInByteArray(byte[] source, byte newbyte, int offset) {
		byte[] newsource = source;

		newsource[offset] = newbyte;

		return newsource;
	}
}
