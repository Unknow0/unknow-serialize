package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.BinaryFormat;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Byte codec
 * 
 * @author unknow
 */
public class ByteCodec implements Codec {
	@Override
	public void write(BinaryFormat format, Object o, OutputStream out) throws IOException {
		out.write((Byte) o);
	}

	@Override
	public Object read(BinaryFormat format, InputStream in) throws IOException {
		return (byte) in.read();
	}

	/**
	 * byte[] codec
	 * 
	 * @author unknow
	 */
	public static class Array implements Codec {
		@Override
		public void write(BinaryFormat format, Object o, OutputStream out) throws IOException {
			byte[] b = (byte[]) o;
			IoUtils.write(out, b.length);
			out.write(b);
		}

		@Override
		public Object read(BinaryFormat format, InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			byte[] b = new byte[len];
			IoUtils.fill(in, b);
			return b;
		}
	}
}