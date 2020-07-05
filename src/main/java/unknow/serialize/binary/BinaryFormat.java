package unknow.serialize.binary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Binary serialization for Object. Only registered class can be serialized
 * 
 * @author unknow
 */
public interface BinaryFormat {
	/**
	 * @return the hash from all registered class
	 */
	byte[] hash();

	/**
	 * write the object
	 * 
	 * @param o   the object to write
	 * @param out the output
	 * @throws IOException on IOException
	 */
	void write(Object o, OutputStream out) throws IOException;

	/**
	 * read an object
	 * 
	 * @param in the input
	 * @return the object
	 * @throws IOException on IOException
	 */
	Object read(InputStream in) throws IOException;
}
