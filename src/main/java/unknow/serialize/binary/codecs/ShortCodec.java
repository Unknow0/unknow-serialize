package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.BinaryFormat;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Short codec
 * 
 * @author unknow
 */
public class ShortCodec implements Codec {
	@Override
	public void write(BinaryFormat format, Object o, OutputStream out) throws IOException {
		IoUtils.write(out, (Short) o);
	}

	@Override
	public Object read(BinaryFormat format, InputStream in) throws IOException {
		return (short) IoUtils.readInt(in);
	}

	/**
	 * short[] codec
	 * 
	 * @author unknow
	 */
	public static class Array implements Codec {
		@Override
		public void write(BinaryFormat format, Object o, OutputStream out) throws IOException {
			short[] b = (short[]) o;
			IoUtils.write(out, b.length);
			for (int i = 0; i < b.length; i++)
				IoUtils.write(out, b[i]);
		}

		@Override
		public Object read(BinaryFormat format, InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			short[] b = new short[len];
			for (int i = 0; i < len; i++)
				b[i] = (short) IoUtils.readInt(in);
			return b;
		}
	}
}