package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Byte codec
 * 
 * @author unknow
 */
public class ByteCodec implements Codec {
	private final int id;

	/**
	 * create new ByteCodec
	 * 
	 * @param id the codecId
	 */
	public ByteCodec(int id) {
		this.id = id;
	}

	@Override
	public void write(Object o, OutputStream out) throws IOException {
		if (o == null)
			out.write(0);
		if (!(o instanceof java.lang.Byte))
			throw new IOException("not a Byte");
		IoUtils.write(out, id);
		out.write((Byte) o);
	}

	@Override
	public Object read(InputStream in) throws IOException {
		return (byte) in.read();
	}

	/**
	 * byte[] codec
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
			if (!(o instanceof byte[]))
				throw new IOException("not a byte array");
			IoUtils.write(out, id);
			byte[] b = (byte[]) o;
			IoUtils.write(out, b.length);
			out.write(b);
		}

		@Override
		public Object read(InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			byte[] b = new byte[len];
			IoUtils.fill(in, b);
			return b;
		}
	}
}