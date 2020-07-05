package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.BinaryFormat;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Integer codec
 * 
 * @author unknow
 */
public class IntegerCodec implements Codec {
	@Override
	public void write(BinaryFormat format, Object o, OutputStream out) throws IOException {
		IoUtils.write(out, (Integer) o);
	}

	@Override
	public Object read(BinaryFormat format, InputStream in) throws IOException {
		return IoUtils.readInt(in);
	}

	/**
	 * int[] codec
	 * 
	 * @author unknow
	 */
	public static class Array implements Codec {
		@Override
		public void write(BinaryFormat format, Object o, OutputStream out) throws IOException {
			int[] b = (int[]) o;
			IoUtils.write(out, b.length);
			for (int i = 0; i < b.length; i++)
				IoUtils.write(out, b[i]);
		}

		@Override
		public Object read(BinaryFormat format, InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			int[] b = new int[len];
			for (int i = 0; i < len; i++)
				b[i] = IoUtils.readInt(in);
			return b;
		}
	}
}