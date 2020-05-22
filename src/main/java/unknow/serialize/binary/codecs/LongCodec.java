package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Long codec
 * 
 * @author unknow
 */
public class LongCodec implements Codec {
	private final int id;

	/**
	 * create new LongCodec
	 * 
	 * @param id the codecId
	 */
	public LongCodec(int id) {
		this.id = id;
	}

	@Override
	public void write(Object o, OutputStream out) throws IOException {
		if (o == null)
			out.write(0);
		if (!(o instanceof Long))
			throw new IOException("not a Long");
		IoUtils.write(out, id);
		IoUtils.write(out, (Long) o);
	}

	@Override
	public Object read(InputStream in) throws IOException {
		return IoUtils.readLong(in);
	}

	/**
	 * long[] codec
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
			if (!(o instanceof long[]))
				throw new IOException("not a long array");
			IoUtils.write(out, id);
			long[] b = (long[]) o;
			IoUtils.write(out, b.length);
			for (int i = 0; i < b.length; i++)
				IoUtils.write(out, b[i]);
		}

		@Override
		public Object read(InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			long[] b = new long[len];
			for (int i = 0; i < len; i++)
				b[i] = IoUtils.readLong(in);
			return b;
		}
	}
}