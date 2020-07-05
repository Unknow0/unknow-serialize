package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.BinaryFormat;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Long codec
 * 
 * @author unknow
 */
public class LongCodec implements Codec {
	@Override
	public void write(BinaryFormat format, Object o, OutputStream out) throws IOException {
		IoUtils.write(out, (Long) o);
	}

	@Override
	public Object read(BinaryFormat format, InputStream in) throws IOException {
		return IoUtils.readLong(in);
	}

	/**
	 * long[] codec
	 * 
	 * @author unknow
	 */
	public static class Array implements Codec {
		@Override
		public void write(BinaryFormat format, Object o, OutputStream out) throws IOException {
			long[] b = (long[]) o;
			IoUtils.write(out, b.length);
			for (int i = 0; i < b.length; i++)
				IoUtils.write(out, b[i]);
		}

		@Override
		public Object read(BinaryFormat format, InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			long[] b = new long[len];
			for (int i = 0; i < len; i++)
				b[i] = IoUtils.readLong(in);
			return b;
		}
	}
}