package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.Codec;
import unknow.serialize.binary.IoUtils;

/**
 * java.lang.Character codec
 * 
 * @author unknow
 */
public class CharacterCodec implements Codec {
	private final int id;

	/**
	 * create new CharacterCodec
	 * 
	 * @param id the codecId
	 */
	public CharacterCodec(int id) {
		this.id = id;
	}

	@Override
	public void write(Object o, OutputStream out) throws IOException {
		if (o == null)
			out.write(0);
		if (!(o instanceof Character))
			throw new IOException("not a Character");
		IoUtils.write(out, id);
		IoUtils.write(out, (Character) o);
	}

	@Override
	public Object read(InputStream in) throws IOException {
		return (char) IoUtils.readInt(in);
	}

	/**
	 * char[] codec
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
			if (!(o instanceof char[]))
				throw new IOException("not a char array");
			IoUtils.write(out, id);
			char[] b = (char[]) o;
			IoUtils.write(out, b.length);
			for (int i = 0; i < b.length; i++)
				IoUtils.write(out, b[i]);
		}

		@Override
		public Object read(InputStream in) throws IOException {
			int len = IoUtils.readInt(in);
			char[] b = new char[len];
			for (int i = 0; i < len; i++)
				b[i] = (char) IoUtils.readInt(in);
			return b;
		}
	}
}