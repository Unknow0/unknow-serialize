package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.BinaryFormat;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Float codec
 * 
 * @author unknow
 */
public class FloatCodec implements Codec {
	@Override
	public void write(BinaryFormat format, Object o, OutputStream out) throws IOException {
		IoUtils.write(out, (Float) o);
	}

	@Override
	public Object read(BinaryFormat format, InputStream in) throws IOException {
		return IoUtils.readFloat(in);
	}

	/**
	 * float[] codec
	 * 
	 * @author unknow
	 */
	public static class Array implements Codec {

		@Override
		public void write(BinaryFormat format, Object o, OutputStream out) throws IOException {
			float[] b = (float[]) o;
			IoUtils.write(out, b.length);
			for (int i = 0; i < b.length; i++)
				IoUtils.write(out, b[i]);
		}

		@Override
		public Object read(BinaryFormat format, InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			float[] b = new float[len];
			for (int i = 0; i < len; i++)
				b[i] = IoUtils.readFloat(in);
			return b;
		}
	}
}
