package unknow.serialize.binary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author unknow
 */
public class IoUtils {
	/**
	 * read one byte and throws IOException if noting to read
	 * 
	 * @param in the input
	 * @return the read value
	 * @throws IOException on IOException or end of stream reached
	 */
	public static int read(InputStream in) throws IOException {
		int i = in.read();
		if (i == -1)
			throw new IOException("end of stream reached");
		return i;
	}

	/**
	 * try to fill the buffer
	 * 
	 * @param in the input
	 * @param b  the buffer to fill
	 * @throws IOException on out.read error or end of stream reached
	 */
	public static void fill(InputStream in, byte[] b) throws IOException {
		int l = b.length;
		int i = 0;
		while (l > 0) {
			int s = in.read(b, i, l);
			if (s == -1)
				throw new IOException("end of stream reached");
			l -= s;
			i += s;
		}
	}

	/**
	 * Writes a 1-5 byte int.
	 * 
	 * @param out   the output
	 * @param value the value to write
	 * @throws IOException on out.write error
	 */
	public static void write(OutputStream out, int value) throws IOException {
		if (value >>> 7 == 0) {
			out.write(value);
		} else if (value >>> 14 == 0) {
			out.write((value) | 0x80);
			out.write(value >>> 7);
		} else if (value >>> 21 == 0) {
			out.write((value) | 0x80);
			out.write(value >>> 7 | 0x80);
			out.write(value >>> 14);
		} else if (value >>> 28 == 0) {
			out.write((value) | 0x80);
			out.write(value >>> 7 | 0x80);
			out.write(value >>> 14 | 0x80);
			out.write(value >>> 21);
		} else {
			out.write(value | 0x80);
			out.write(value >>> 7 | 0x80);
			out.write(value >>> 14 | 0x80);
			out.write(value >>> 21 | 0x80);
			out.write(value >>> 28);
		}
	}

	/**
	 * read 1-5 byte int
	 * 
	 * @param in the input
	 * @return the int value
	 * @throws IOException on IOException
	 */
	public static int readInt(InputStream in) throws IOException {
		int b = read(in);
		int result = b & 0x7F;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= (b & 0x7F) << 7;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= (b & 0x7F) << 14;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= (b & 0x7F) << 21;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= b << 28;
		return result;
	}

	/**
	 * Writes a 1-9 byte long.
	 * 
	 * @param out   the output
	 * @param value the value to write
	 * @throws IOException on out.write error
	 */
	public static void write(OutputStream out, long value) throws IOException {
		if (value >>> 7 == 0) {
			out.write((byte) value);
		} else if (value >>> 14 == 0) {
			out.write((byte) (value | 0x80));
			out.write((byte) (value >>> 7));
		} else if (value >>> 21 == 0) {
			out.write((byte) (value | 0x80));
			out.write((byte) (value >>> 7 | 0x80));
			out.write((byte) (value >>> 14));
		} else if (value >>> 28 == 0) {
			out.write((byte) (value | 0x80));
			out.write((byte) (value >>> 7 | 0x80));
			out.write((byte) (value >>> 14 | 0x80));
			out.write((byte) (value >>> 21));
		} else if (value >>> 35 == 0) {
			out.write((byte) (value | 0x80));
			out.write((byte) (value >>> 7 | 0x80));
			out.write((byte) (value >>> 14 | 0x80));
			out.write((byte) (value >>> 21 | 0x80));
			out.write((byte) (value >>> 28));
		} else if (value >>> 42 == 0) {
			out.write((byte) (value | 0x80));
			out.write((byte) (value >>> 7 | 0x80));
			out.write((byte) (value >>> 14 | 0x80));
			out.write((byte) (value >>> 21 | 0x80));
			out.write((byte) (value >>> 28 | 0x80));
			out.write((byte) (value >>> 35));
		} else if (value >>> 49 == 0) {
			out.write((byte) (value | 0x80));
			out.write((byte) (value >>> 7 | 0x80));
			out.write((byte) (value >>> 14 | 0x80));
			out.write((byte) (value >>> 21 | 0x80));
			out.write((byte) (value >>> 28 | 0x80));
			out.write((byte) (value >>> 35 | 0x80));
			out.write((byte) (value >>> 42));
		} else if (value >>> 56 == 0) {
			out.write((byte) (value | 0x80));
			out.write((byte) (value >>> 7 | 0x80));
			out.write((byte) (value >>> 14 | 0x80));
			out.write((byte) (value >>> 21 | 0x80));
			out.write((byte) (value >>> 28 | 0x80));
			out.write((byte) (value >>> 35 | 0x80));
			out.write((byte) (value >>> 42 | 0x80));
			out.write((byte) (value >>> 49));
		} else {
			out.write((byte) (value | 0x80));
			out.write((byte) (value >>> 7 | 0x80));
			out.write((byte) (value >>> 14 | 0x80));
			out.write((byte) (value >>> 21 | 0x80));
			out.write((byte) (value >>> 28 | 0x80));
			out.write((byte) (value >>> 35 | 0x80));
			out.write((byte) (value >>> 42 | 0x80));
			out.write((byte) (value >>> 49 | 0x80));
			out.write((byte) (value >>> 56));
		}
	}

	/**
	 * read a 1-9 byte long
	 * 
	 * @param in the input
	 * @return the value
	 * @throws IOException on IOException
	 */
	public static long readLong(InputStream in) throws IOException {
		long b = read(in);
		long result = b & 0x7F;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= (b & 0x7F) << 7;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= (b & 0x7F) << 14;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= (b & 0x7F) << 21;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= (b & 0x7F) << 28;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= (b & 0x7F) << 35;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= (b & 0x7F) << 42;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= (b & 0x7F) << 49;
		if ((b & 0x80) == 0)
			return result;
		b = read(in);
		result |= b << 56;
		return result;
	}

	/**
	 * write a float value
	 * 
	 * @param out the output
	 * @param s   the value
	 * @throws IOException on IOException
	 */
	public static void write(OutputStream out, float s) throws IOException {
		int value = Float.floatToIntBits(s);
		out.write(value >>> 24);
		out.write(value >>> 16);
		out.write(value >>> 8);
		out.write(value);
	}

	/**
	 * read a float value
	 * 
	 * @param in the input
	 * @return the value
	 * @throws IOException on IOException
	 */
	public static float readFloat(InputStream in) throws IOException {
		byte[] b = new byte[4];
		fill(in, b);
		return Float.intBitsToFloat(((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF));
	}

	/**
	 * write a double value
	 * 
	 * @param out the output
	 * @param s   the value
	 * @throws IOException on IOException
	 */
	public static void write(OutputStream out, double s) throws IOException {
		long value = Double.doubleToLongBits(s);
		out.write((byte) (value >>> 56));
		out.write((byte) (value >>> 48));
		out.write((byte) (value >>> 40));
		out.write((byte) (value >>> 32));
		out.write((byte) (value >>> 24));
		out.write((byte) (value >>> 16));
		out.write((byte) (value >>> 8));
		out.write((byte) value);
	}

	/**
	 * read a double value
	 * 
	 * @param in the input
	 * @return the value
	 * @throws IOException on IOException
	 */
	public static double readDouble(InputStream in) throws IOException {
		byte[] b = new byte[8];
		fill(in, b);
		long l = ((long) b[0] & 0xFF) << 56;
		l |= ((long) b[1] & 0xFF) << 48;
		l |= ((long) b[2] & 0xFF) << 40;
		l |= ((long) b[3] & 0xFF) << 32;
		l |= ((long) b[4] & 0xFF) << 24;
		l |= ((long) b[5] & 0xFF) << 16;
		l |= ((long) b[6] & 0xFF) << 8;
		l |= (long) b[7] & 0xFF;
		return Double.longBitsToDouble(l);
	}
}
