package unknow.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
