package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Short codec
 * 
 * @author unknow
 */
public class ShortCodec implements Codec {
	private final int id;

	/**
	 * create new ShortCodec
	 * 
	 * @param id the codecId
	 */
	public ShortCodec(int id) {
		this.id = id;
	}

	@Override
	public void write(Object o, OutputStream out) throws IOException {
		if (o == null)
			out.write(0);
		if (!(o instanceof Short))
			throw new IOException("not a Short");
		IoUtils.write(out, id);
		IoUtils.write(out, (Short) o);
	}

	@Override
	public Object read(InputStream in) throws IOException {
		return (short) IoUtils.readInt(in);
	}

	/**
	 * short[] codec
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
			if (!(o instanceof short[]))
				throw new IOException("not a short array");
			IoUtils.write(out, id);
			short[] b = (short[]) o;
			IoUtils.write(out, b.length);
			for (int i = 0; i < b.length; i++)
				IoUtils.write(out, b[i]);
		}

		@Override
		public Object read(InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			short[] b = new short[len];
			for (int i = 0; i < len; i++)
				b[i] = (short) IoUtils.readInt(in);
			return b;
		}
	}
}