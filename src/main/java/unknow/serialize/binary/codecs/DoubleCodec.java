package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Double codec
 * 
 * @author unknow
 */
public class DoubleCodec implements Codec {
	private final int id;

	/**
	 * create new DoubleCodec
	 * 
	 * @param id the codecId
	 */
	public DoubleCodec(int id) {
		this.id = id;
	}

	@Override
	public void write(Object o, OutputStream out) throws IOException {
		if (o == null)
			out.write(0);
		if (!(o instanceof Double))
			throw new IOException("not a Double");
		IoUtils.write(out, id);
		IoUtils.write(out, (Double) o);
	}

	@Override
	public Object read(InputStream in) throws IOException {
		return IoUtils.readDouble(in);
	}

	/**
	 * double[] codec
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
			if (!(o instanceof double[]))
				throw new IOException("not a double array");
			IoUtils.write(out, id);
			double[] b = (double[]) o;
			IoUtils.write(out, b.length);
			for (int i = 0; i < b.length; i++)
				IoUtils.write(out, b[i]);
		}

		@Override
		public Object read(InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			double[] b = new double[len];
			for (int i = 0; i < len; i++)
				b[i] = IoUtils.readDouble(in);
			return b;
		}
	}
}
