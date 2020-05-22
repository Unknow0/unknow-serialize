package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Integer codec
 * 
 * @author unknow
 */
public class IntegerCodec implements Codec {
	private final int id;

	/**
	 * create new IntegerCodec
	 * 
	 * @param id the codecId
	 */
	public IntegerCodec(int id) {
		this.id = id;
	}

	@Override
	public void write(Object o, OutputStream out) throws IOException {
		if (o == null)
			out.write(0);
		if (!(o instanceof Integer))
			throw new IOException("not a Integer");
		IoUtils.write(out, id);
		IoUtils.write(out, (Integer) o);
	}

	@Override
	public Object read(InputStream in) throws IOException {
		return IoUtils.readInt(in);
	}

	/**
	 * int[] codec
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
			if (!(o instanceof int[]))
				throw new IOException("not a int array");
			IoUtils.write(out, id);
			int[] b = (int[]) o;
			IoUtils.write(out, b.length);
			for (int i = 0; i < b.length; i++)
				IoUtils.write(out, b[i]);
		}

		@Override
		public Object read(InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			int[] b = new int[len];
			for (int i = 0; i < len; i++)
				b[i] = IoUtils.readInt(in);
			return b;
		}
	}
}