package unknow.serialize.binary.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import unknow.serialize.Codec;
import unknow.serialize.binary.BinaryFormat;
import unknow.serialize.binary.IoUtils;

/**
 * javaµ.lang.String codec
 * 
 * @author unknow
 */
public class StringCodec implements Codec {
	@Override
	public void write(BinaryFormat format, Object o, OutputStream out) throws IOException {
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
	public Object read(BinaryFormat format, InputStream in) throws IOException {
		int len = IoUtils.readInt(in);
		byte[] b = new byte[len];
		IoUtils.fill(in, b);
		return new String(b, StandardCharsets.UTF_8);
	}
}
