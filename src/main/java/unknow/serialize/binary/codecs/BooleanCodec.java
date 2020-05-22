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
public class BooleanCodec implements Codec {
	private final int id;

	/**
	 * create new ByteCodec
	 * 
	 * @param id the codecId
	 */
	public BooleanCodec(int id) {
		this.id = id;
	}

	@Override
	public void write(Object o, OutputStream out) throws IOException {
		if (o == null)
			out.write(0);
		if (!(o instanceof java.lang.Boolean))
			throw new IOException("not a Boolean");
		IoUtils.write(out, id);
		if ((Boolean) o)
			out.write(1);
		else
			out.write(0);
	}

	@Override
	public Object read(InputStream in) throws IOException {
		return IoUtils.read(in) != 0;
	}

	/**
	 * boolean[] codec
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
			if (!(o instanceof boolean[]))
				throw new IOException("not a boolean array");
			IoUtils.write(out, id);
			boolean[] a = (boolean[]) o;
			IoUtils.write(out, a.length);
			int i = 0;
			int len = a.length;
			int off = 0;
			int v = 0;
			while (off < len) {
				if (a[off++])
					v |= 1 << i;
				if (++i == 8) {
					i = 0;
					out.write(v);
					v = 0;
				}
			}
			if (i > 0)
				out.write(v);
		}

		@Override
		public Object read(InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			boolean[] a = new boolean[len];
			int i = 0;
			while (i + 8 < len) {
				int v = IoUtils.read(in);
				a[i++] = (v & 0b0000_0001) != 0;
				a[i++] = (v & 0b0000_0010) != 0;
				a[i++] = (v & 0b0000_0100) != 0;
				a[i++] = (v & 0b0000_1000) != 0;
				a[i++] = (v & 0b0001_0000) != 0;
				a[i++] = (v & 0b0010_0000) != 0;
				a[i++] = (v & 0b0100_0000) != 0;
				a[i++] = (v & 0b1000_0000) != 0;
			}
			if (i < len) {
				int v = IoUtils.read(in);
				int m = 1;
				while (i < len) {
					a[i++] = (v & m) != 0;
					m = m << 1;
				}
			}
			return a;
		}
	}

	/**
	 * Boolean[] codec
	 * 
	 * @author unknow
	 */
	public static class ArrayBoolean implements Codec {
		private final int id;

		/**
		 * create new ArrayBoolean
		 * 
		 * @param id the codecId
		 */
		public ArrayBoolean(int id) {
			this.id = id;
		}

		@Override
		public void write(Object o, OutputStream out) throws IOException {
			if (o == null)
				out.write(0);
			if (!(o instanceof Boolean[]))
				throw new IOException("not a Boolean array");
			IoUtils.write(out, id);
			Boolean[] a = (Boolean[]) o;
			IoUtils.write(out, a.length);
			int i = 0;
			int len = a.length;
			int off = 0;
			int v = 0;
			while (off < len) {
				Boolean b = a[off++];
				if (b != null) {
					v |= 1 << i + 1;
					if (b)
						v |= 1 << i;
				}
				i += 2;
				if (i == 8) {
					i = 0;
					out.write(v);
					v = 0;
				}
			}
			if (i > 0)
				out.write(v);
		}

		private static Boolean read(int i) throws IOException {
			if (i == 0b00)
				return null;
			if (i == 0b10)
				return Boolean.FALSE;
			if (i == 0b11)
				return Boolean.TRUE;
			throw new IOException("corruped stream");
		}

		@Override
		public Object read(InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			Boolean[] a = new Boolean[len];
			int i = 0;
			while (i + 4 < len) {
				int v = IoUtils.read(in);
				a[i++] = read(v & 0b11);
				a[i++] = read((v >>> 2) & 0b11);
				a[i++] = read((v >>> 4) & 0b11);
				a[i++] = read((v >>> 6) & 0b11);
			}
			if (i < len) {
				int v = IoUtils.read(in);
				while (i < len) {
					a[i++] = read(v & 0b11);
					v = v >>> 2;
				}
			}
			return a;
		}
	}
}