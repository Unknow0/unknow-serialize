package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import unknow.serialize.Codec;
import unknow.serialize.binary.IoUtils;

/**
 * javaµ.lang.String codec
 * 
 * @author unknow
 */
public class StringCodec implements Codec {
	private final int id;

	/**
	 * create new StringCodec
	 * 
	 * @param id the codecId
	 */
	public StringCodec(int id) {
		this.id = id;
	}

	@Override
	public void write(Object o, OutputStream out) throws IOException {
		if (o == null)
			out.write(0);
		if (!(o instanceof String))
			throw new IOException("not a String");
		IoUtils.write(out, id);
		String value = (String) o;
		int charCount = value.length();
		if (charCount == 0) {
			out.write(0x00);
			return;
		}
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		IoUtils.write(out, bytes.length);
		out.write(bytes);
	}

	@Override
	public Object read(InputStream in) throws IOException {
		int len = IoUtils.readInt(in);
		byte[] b = new byte[len];
		IoUtils.fill(in, b);
		return new String(b, StandardCharsets.UTF_8);
	}
}
