package unknow.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unknow.serialize.binary.BinaryFormat;

/**
 * a codec for an Object
 * 
 * @author unknow
 */
public interface Codec {
	/**
	 * write the object
	 * 
	 * @param o   the object to write
	 * @param out the output
	 * @throws IOException on IOException
	 */
	void write(BinaryFormat format, Object o, OutputStream out) throws IOException;

	/**
	 * read an object
	 * 
	 * @param in the input
	 * @return the object
	 * @throws IOException on IOException
	 */
	Object read(BinaryFormat format, InputStream in) throws IOException;
}
