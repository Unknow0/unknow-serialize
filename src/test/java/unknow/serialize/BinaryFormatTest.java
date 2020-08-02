/**
 * 
 */
package unknow.serialize;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import unknow.serialize.binary.BinaryFormat;
import unknow.serialize.binary.BinaryFormatBuilder;

/**
 * @author unknow
 */
public class BinaryFormatTest {
	private static final Random rand = new Random();

	@SuppressWarnings("rawtypes")
	private static BinaryFormat format(Class... cl) throws ReflectiveOperationException {
//		BinaryFormat.Builder b=BinaryFormat.create();
		BinaryFormatBuilder b = new BinaryFormatBuilder();

		for (int i = 0; i < cl.length; i++)
			b.register(cl[i]);
		return b.build();
	}

	@Test
	public void testPrimitive() throws ReflectiveOperationException, IOException {
		BinaryFormat binary = format(Primitive.class);

		Primitive o = new Primitive();
		o.bool = rand.nextBoolean();
		o.b = (byte) rand.nextInt(256);
		o.c = (char) rand.nextInt(65536);
		o.s = (short) rand.nextInt(65536);
		o.i = rand.nextInt();
		o.l = rand.nextLong();
		o.f = rand.nextFloat();
		o.d = rand.nextDouble();
		assertReadWrite("Primitive inside an object", binary, o);
	}

	@Test
	public void testWrapper() throws ReflectiveOperationException, IOException {
		BinaryFormat binary = format(Wrapper.class, Boolean.class);

		assertReadWrite("Boolean", binary, rand.nextBoolean());
		assertReadWrite("Byte", binary, (byte) rand.nextInt(256));
		assertReadWrite("Character", binary, (char) rand.nextInt(65536));
		assertReadWrite("Short", binary, (short) rand.nextInt(65536));
		assertReadWrite("Integer", binary, rand.nextInt());
		assertReadWrite("Long", binary, rand.nextLong());
		assertReadWrite("Float", binary, rand.nextFloat());
		assertReadWrite("Double", binary, rand.nextDouble());

		Wrapper o = new Wrapper();
		o.bool = rand.nextBoolean() ? null : rand.nextBoolean();
		o.b = rand.nextBoolean() ? null : (byte) rand.nextInt(256);
		o.c = rand.nextBoolean() ? null : (char) rand.nextInt(65536);
		o.s = rand.nextBoolean() ? null : (short) rand.nextInt(65536);
		o.i = rand.nextBoolean() ? null : rand.nextInt();
		o.l = rand.nextBoolean() ? null : rand.nextLong();
		o.f = rand.nextBoolean() ? null : rand.nextFloat();
		o.d = rand.nextBoolean() ? null : rand.nextDouble();
		assertReadWrite("Wrapper inside an object", binary, o);
	}

	@Test
	public void testPrimitiveArray() throws ReflectiveOperationException, IOException {
		BinaryFormat binary = format(PrimitiveArray.class);

		PrimitiveArray o = new PrimitiveArray();
		o.bool = new boolean[rand.nextInt(256)];
		o.b = new byte[rand.nextInt(256)];
		o.c = new char[rand.nextInt(256)];
		o.s = new short[rand.nextInt(256)];
		o.i = new int[rand.nextInt(256)];
		o.l = new long[rand.nextInt(256)];
		o.f = new float[rand.nextInt(256)];
		o.d = new double[rand.nextInt(256)];

		assertReadWrite("boolean[]", binary, o.bool);
		assertReadWrite("byte[]", binary, o.b);
		assertReadWrite("char[]", binary, o.c);
		assertReadWrite("short[]", binary, o.s);
		assertReadWrite("int[]", binary, o.i);
		assertReadWrite("long[]", binary, o.l);
		assertReadWrite("float[]", binary, o.f);
		assertReadWrite("double[]", binary, o.d);
		assertReadWrite("Array inside an object", binary, o);
	}

	@Test
	public void testEnum() throws ReflectiveOperationException, IOException {
		BinaryFormat binary = format(EnumTest.class);

		assertReadWrite("Enum", binary, (Object) null);
		assertReadWrite("Enum", binary, EnumTest.V1);
	}

	@Test
	public void testArray() throws ReflectiveOperationException, IOException {
		BinaryFormat binary = format(Byte[].class);

		int len = rand.nextInt(256);
		Byte[] a = new Byte[len];
		for (int i = 0; i < len; i++)
			a[i] = rand.nextBoolean() ? null : (byte) rand.nextInt(256);
		assertReadWrite("Object[]", binary, a);
	}

	@Test
	public void testCollection() throws ReflectiveOperationException, IOException {
		BinaryFormat binary = format(ArrayList.class, HashSet.class, Byte.class);

		Collection<Byte> list = new ArrayList<>();
		int len = rand.nextInt(256);
		for (int i = 0; i < len; i++)
			list.add(rand.nextBoolean() ? null : (byte) rand.nextInt(256));
		assertReadWrite("ArrayList", binary, list);

		list = new HashSet<>();
		len = rand.nextInt(256);
		for (int i = 0; i < len; i++)
			list.add(rand.nextBoolean() ? null : (byte) rand.nextInt(256));
		assertReadWrite("HashSet", binary, list);
	}

	@Test
	public void testMap() throws ReflectiveOperationException, IOException {
		BinaryFormat binary = format(HashMap.class, Byte.class);

		Map<Byte, Byte> list = new HashMap<>();
		int len = rand.nextInt(256);
		for (int i = 0; i < len; i++) {
			Byte b = rand.nextBoolean() ? null : (byte) rand.nextInt(256);
			list.put(b, b);
		}
		assertReadWrite("ArrayList", binary, list);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testObject() throws ReflectiveOperationException, IOException {
		BinaryFormat binary = format(ArrayList.class, Integer.class, Pojo.class);

		int len = rand.nextInt(256);
		int[][] a = new int[len][];
		for (int i = 0; i < len; i++)
			a[i] = new int[rand.nextInt(256)];

		Pojo pojo = new Pojo();
		pojo.setMultiArray(a);
		pojo.setList(new ArrayList(Arrays.asList(1, 5, 6, 7)));

		pojo.addInner(new Pojo.Inner(4));
		pojo.addInner(new Pojo.Inner(7));

		assertReadWrite("Pojo", binary, pojo);

	}

	private static void assertReadWrite(String msg, BinaryFormat binary, Object o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		binary.write(o, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Object read = binary.read(in);
		assertEquals(msg, o, read);
	}

	private static void assertReadWrite(String msg, BinaryFormat binary, Object[] o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		binary.write(o, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Object read = binary.read(in);
		assertArrayEquals(msg, o, (Object[]) read);
	}

	private static void assertReadWrite(String msg, BinaryFormat binary, boolean[] o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		binary.write(o, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Object read = binary.read(in);
		assertArrayEquals(msg, o, (boolean[]) read);
	}

	private static void assertReadWrite(String msg, BinaryFormat binary, byte[] o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		binary.write(o, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Object read = binary.read(in);
		assertArrayEquals(msg, o, (byte[]) read);
	}

	private static void assertReadWrite(String msg, BinaryFormat binary, char[] o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		binary.write(o, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Object read = binary.read(in);
		assertArrayEquals(msg, o, (char[]) read);
	}

	private static void assertReadWrite(String msg, BinaryFormat binary, short[] o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		binary.write(o, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Object read = binary.read(in);
		assertArrayEquals(msg, o, (short[]) read);
	}

	private static void assertReadWrite(String msg, BinaryFormat binary, int[] o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		binary.write(o, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Object read = binary.read(in);
		assertArrayEquals(msg, o, (int[]) read);
	}

	private static void assertReadWrite(String msg, BinaryFormat binary, long[] o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		binary.write(o, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Object read = binary.read(in);
		assertArrayEquals(msg, o, (long[]) read);
	}

	private static void assertReadWrite(String msg, BinaryFormat binary, float[] o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		binary.write(o, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Object read = binary.read(in);
		assertArrayEquals(msg, o, (float[]) read, .0000001f);
	}

	private static void assertReadWrite(String msg, BinaryFormat binary, double[] o) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		binary.write(o, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Object read = binary.read(in);
		assertArrayEquals(msg, o, (double[]) read, .0000001);
	}
}
