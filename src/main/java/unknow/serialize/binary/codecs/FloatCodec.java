package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Float codec
 * 
 * @author unknow
 */
public class FloatCodec implements Codec {
	private final int id;

	/**
	 * create new FloatCodec
	 * 
	 * @param id the codecId
	 */
	public FloatCodec(int id) {
		this.id = id;
	}

	@Override
	public void write(Object o, OutputStream out) throws IOException {
		if (o == null)
			out.write(0);
		if (!(o instanceof java.lang.Float))
			throw new IOException("not a Foat");
		IoUtils.write(out, id);
		IoUtils.write(out, (Float) o);
	}

	@Override
	public Object read(InputStream in) throws IOException {
		return IoUtils.readFloat(in);
	}

	/**
	 * float[] codec
	 * 
	 * @author unknow
	 */
	public static class Array implements Codec {
		private final int id;

		/**
		 * create new Array
		 * 
		 * @param id the codecId
		 */
		public Array(int id) {
			this.id = id;
		}

		@Override
		public void write(Object o, OutputStream out) throws IOException {
			if (o == null)
				out.write(0);
			if (!(o instanceof float[]))
				throw new IOException("not a float array");
			IoUtils.write(out, id);
			float[] b = (float[]) o;
			IoUtils.write(out, b.length);
			for (int i = 0; i < b.length; i++)
				IoUtils.write(out, b[i]);
		}

		@Override
		public Object read(InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			float[] b = new float[len];
			for (int i = 0; i < len; i++)
				b[i] = IoUtils.readFloat(in);
			return b;
		}
	}
}
