package unknow.serialize.binary;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.serialize.Codec;
import unknow.serialize.binary.codecs.BooleanCodec;
import unknow.serialize.binary.codecs.ByteCodec;
import unknow.serialize.binary.codecs.CharacterCodec;
import unknow.serialize.binary.codecs.DoubleCodec;
import unknow.serialize.binary.codecs.FloatCodec;
import unknow.serialize.binary.codecs.IntegerCodec;
import unknow.serialize.binary.codecs.LongCodec;
import unknow.serialize.binary.codecs.ShortCodec;
import unknow.serialize.binary.codecs.StringCodec;

/**
 * Binary serialization builder
 * 
 * @author unknow
 */
public class BinaryFormatBuilder {
	private static final Logger log = LoggerFactory.getLogger(BinaryFormatBuilder.class);

	private static final int MOD = Modifier.STATIC | Modifier.TRANSIENT;
	private static final String[] IOEXCEPTION = new String[] { "java/io/IOException" };
	private static final String[] CODEC = new String[] { Type.getInternalName(Codec.class) };
	private static final String[] BINARYFORMAT = new String[] { Type.getInternalName(BinaryFormat.class) };
	private static final Comparator<Class<?>> CLASS_CMP = (a, b) -> a.getName().compareTo(b.getName());

	private static final Class<?> INT_COLLECTION;
	static {
		Class<?> cl = null;
		try {
			cl = Class.forName("unknow.common.data.IntCollection");
		} catch (ClassNotFoundException e) {// OK
		}
		INT_COLLECTION = cl;
	}

	/** version of the format */
	private static final byte VERSION = 1;

	/** Codec classloader */
	private final Loader loader = new Loader();
	/** hash calculation */
	private final MessageDigest md;

	/** all builded codec */
	private final Map<Class<?>, Integer> sawClass = new HashMap<>();
	/** all CodecBuilder */
	private final Map<Class<?>, CodecBuilder> builders = new HashMap<>();

	/** actual id -> Codec class mapping */
	private final Map<Integer, Class<?>> codecs = new HashMap<>();

	/** id for the next registered classs (0 is reserved for null) */
	private int nextId = 1;

	/** local cache */
	private String clazz;
	private String clazzDescriptor;
	private String codecName;
	private List<Field> fields = new ArrayList<>();
	private List<Field> boolFields = new ArrayList<>();
	private List<Field> booleanFields = new ArrayList<>();
	private Set<Class<?>> required = new TreeSet<>(CLASS_CMP);
	private List<Class<?>> temp = new ArrayList<>();

	public BinaryFormatBuilder() {
		try {
			md = MessageDigest.getInstance("SHA-512");
			md.update(VERSION);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * register a custom codec
	 * 
	 * @param cl    class to register
	 * @param codec codec to handle this class
	 * @return this
	 */
	public BinaryFormatBuilder register(Class<?> cl, Class<? extends Codec> codec) {
		int id = nextId++;
		codecs.put(id, codec);
		sawClass.put(cl, id);
		md.update(cl.getName().getBytes(StandardCharsets.UTF_8));
		md.update(codec.getClass().getName().getBytes(StandardCharsets.UTF_8));
		return this;
	}

	/**
	 * add a builder for this class
	 * 
	 * @param cl      class to add
	 * @param builder the builder for the class
	 * @return this
	 */
	public BinaryFormatBuilder addBuilder(Class<?> cl, CodecBuilder builder) {
		builders.put(cl, builder);
		return this;
	}

	/**
	 * generate the codec for class Cl
	 * 
	 * @param cl class to register
	 * @return this
	 */
	public BinaryFormatBuilder register(Class<?> cl) {
		if (cl.isPrimitive() || cl == Object.class || sawClass.containsKey(cl))
			return this;
		required.clear();
		_register(cl);
		while (!required.isEmpty()) {
			temp.addAll(required);
			required.clear();
			temp.removeAll(sawClass.keySet());
			for (Class<?> c : temp)
				_register(c);
			temp.clear();
		}
		return this;
	}

	private void _register(Class<?> cl) {
		if (cl.isAnnotation() || ((cl.getModifiers() & Modifier.ABSTRACT) != 0 && !cl.isArray()) || cl.isInterface()) {
			log.info("skiped " + cl);
			return;
		}
		int id = nextId++;

		log.debug("registring new Codec {} {}", id, cl);
		md.update(cl.getName().getBytes(StandardCharsets.UTF_8));
		Class<? extends Codec> codec = null;
		if (cl == Boolean.class)
			codec = BooleanCodec.class;
		else if (cl == Byte.class)
			codec = ByteCodec.class;
		else if (cl == Character.class)
			codec = CharacterCodec.class;
		else if (cl == Short.class)
			codec = ShortCodec.class;
		else if (cl == Integer.class)
			codec = IntegerCodec.class;
		else if (cl == Long.class)
			codec = LongCodec.class;
		else if (cl == Float.class)
			codec = FloatCodec.class;
		else if (cl == Double.class)
			codec = DoubleCodec.class;
		else if (cl == String.class)
			codec = StringCodec.class;
		else if (cl == boolean[].class)
			codec = BooleanCodec.Array.class;
		else if (cl == Boolean[].class)
			codec = BooleanCodec.ArrayBoolean.class;
		else if (cl == byte[].class)
			codec = ByteCodec.Array.class;
		else if (cl == char[].class)
			codec = CharacterCodec.Array.class;
		else if (cl == short[].class)
			codec = ShortCodec.Array.class;
		else if (cl == int[].class)
			codec = IntegerCodec.Array.class;
		else if (cl == long[].class)
			codec = LongCodec.Array.class;
		else if (cl == float[].class)
			codec = FloatCodec.Array.class;
		else if (cl == double[].class)
			codec = DoubleCodec.Array.class;
		else {
			clazz = Type.getInternalName(cl);
			clazzDescriptor = Type.getDescriptor(cl);
			codecName = "unknow/serialize/binary/codecs/$" + Integer.toString(System.identityHashCode(this), 16) + "$" + Integer.toString(System.identityHashCode(cl), 16);
			if (!cl.isArray())
				codecName += "$" + cl.getSimpleName();
			byte[] bytes = generate(cl);
			codec = loader.define(codecName.replace('/', '.'), bytes);
		}
		sawClass.put(cl, id);
		codecs.put(id, codec);
	}

	/**
	 * Build it
	 * 
	 * @return the usable BinaryFormat
	 * @throws ReflectiveOperationException on Codec creation issues
	 */
	public BinaryFormat build() throws ReflectiveOperationException {
		String name = "unknow/serialize/binary/codecs/$" + Integer.toString(System.identityHashCode(this), 16);
		byte[] bytes = generateFormat(name);
		Class<BinaryFormat> cl = loader.define(name.replace('/', '.'), bytes);

		return cl.newInstance();
	}

	private byte[] generateFormat(String name) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, name, null, Type.getInternalName(Object.class), BINARYFORMAT);

		// all fields hash, $<id> codecs
		cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "hash", "[B", null, null);
		for (Map.Entry<Integer, Class<?>> e : codecs.entrySet())
			cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "$" + e.getKey(), "Lunknow/serialize/Codec;", null, null);

		// constructor, init hash & $<id> codecs instances
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		for (Map.Entry<Integer, Class<?>> e : codecs.entrySet()) {
			String internalName = Type.getInternalName(e.getValue());
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitTypeInsn(Opcodes.NEW, internalName);
			mv.visitInsn(Opcodes.DUP);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, internalName, "<init>", "()V", false);
			mv.visitFieldInsn(Opcodes.PUTFIELD, name, "$" + e.getKey(), "Lunknow/serialize/Codec;");
		}

		mv.visitVarInsn(Opcodes.ALOAD, 0); // this
		byte[] digest = md.digest();
		loadInt(mv, digest.length);
		mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
		for (int i = 0; i < digest.length; i++) {
			mv.visitInsn(Opcodes.DUP);
			loadInt(mv, i);
			loadInt(mv, digest[i]);
			mv.visitInsn(Opcodes.BASTORE);
		}
		mv.visitFieldInsn(Opcodes.PUTFIELD, name, "hash", "[B");
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(-1, -1);
		mv.visitEnd();

		// public int hash()
		mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "hash", "()[B", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, name, "hash", "[B");
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(-1, -1);
		mv.visitEnd();

		// public void write(Object, OutputStream) throws IOException
		mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", null, IOEXCEPTION);
		mv.visitCode();

		Label end = new Label();
		mv.visitVarInsn(Opcodes.ALOAD, 1); // o
		mv.visitJumpInsn(Opcodes.IFNONNULL, end);
		mv.visitVarInsn(Opcodes.ALOAD, 2); // out
		loadInt(mv, 0);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);

		mv.visitVarInsn(Opcodes.ALOAD, 1); // o
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
		mv.visitVarInsn(Opcodes.ASTORE, 3); // class
		for (Map.Entry<Class<?>, Integer> e : sawClass.entrySet()) {
			end = new Label();
			mv.visitVarInsn(Opcodes.ALOAD, 3); // class
			mv.visitLdcInsn(Type.getType(e.getKey()));
			mv.visitJumpInsn(Opcodes.IF_ACMPNE, end);
			mv.visitVarInsn(Opcodes.ALOAD, 2); // out
			loadInt(mv, e.getValue());
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

			mv.visitVarInsn(Opcodes.ALOAD, 0); // this
			mv.visitFieldInsn(Opcodes.GETFIELD, name, "$" + e.getValue(), "Lunknow/serialize/Codec;");
			mv.visitVarInsn(Opcodes.ALOAD, 0); // this
			mv.visitVarInsn(Opcodes.ALOAD, 1); // o
			mv.visitVarInsn(Opcodes.ALOAD, 2); // out
			mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/Codec", "write", "(Lunknow/serialize/binary/BinaryFormat;Ljava/lang/Object;Ljava/io/OutputStream;)V", true);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitLabel(end);
		}
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(-1, -1);
		mv.visitEnd();

		// public Object read(InputStream) throws IOException
		mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", null, IOEXCEPTION);
		mv.visitCode();

		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
		Label err = new Label();
		Label[] labels = new Label[sawClass.size() + 1];
		for (int i = 0; i < labels.length; i++)
			labels[i] = new Label();
		mv.visitTableSwitchInsn(0, labels.length - 1, err, labels);
		mv.visitLabel(labels[0]);
		mv.visitInsn(Opcodes.ACONST_NULL);
		mv.visitInsn(Opcodes.ARETURN);
		for (int i = 1; i < labels.length; i++) {
			mv.visitLabel(labels[i]);
			mv.visitVarInsn(Opcodes.ALOAD, 0); // this
			mv.visitFieldInsn(Opcodes.GETFIELD, name, "$" + i, "Lunknow/serialize/Codec;");
			mv.visitVarInsn(Opcodes.ALOAD, 0); // this
			mv.visitVarInsn(Opcodes.ALOAD, 1); // in
			mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/Codec", "read", "(Lunknow/serialize/binary/BinaryFormat;Ljava/io/InputStream;)Ljava/lang/Object;", true);
			mv.visitInsn(Opcodes.ARETURN);
		}
		mv.visitLabel(err);
		mv.visitTypeInsn(Opcodes.NEW, "java/io/IOException");
		mv.visitInsn(Opcodes.DUP);
		mv.visitLdcInsn("corrupt stream (invalid object id)");
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/IOException", "<init>", "(Ljava/lang/String;)V", false);
		mv.visitInsn(Opcodes.ATHROW);
		mv.visitMaxs(-1, -1);
		mv.visitEnd();

		cw.visitEnd();

		return cw.toByteArray();
	}

	/**
	 * generate a Codec for the class
	 * 
	 * @param cl class to generate for
	 * @param id
	 * @return
	 */
	private byte[] generate(Class<?> cl) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, codecName, null, Type.getInternalName(Object.class), CODEC);

		// default constructor
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(-1, -1);
		mv.visitEnd();

		// public void write(BinaryFormat, Object, OutputStream) throws IOException
		mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "write", "(Lunknow/serialize/binary/BinaryFormat;Ljava/lang/Object;Ljava/io/OutputStream;)V", null, IOEXCEPTION);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitTypeInsn(Opcodes.INSTANCEOF, clazz);
		Label label0 = new Label();
		mv.visitJumpInsn(Opcodes.IFNE, label0);
		mv.visitTypeInsn(Opcodes.NEW, "java/io/IOException");
		mv.visitInsn(Opcodes.DUP);
		mv.visitLdcInsn("object not a " + cl.getName());
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/IOException", "<init>", "(Ljava/lang/String;)V", false);
		mv.visitInsn(Opcodes.ATHROW);
		mv.visitLabel(label0);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitTypeInsn(Opcodes.CHECKCAST, clazz);
		mv.visitVarInsn(Opcodes.ALOAD, 3);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, codecName, "write", "(Lunknow/serialize/binary/BinaryFormat;" + clazzDescriptor + "Ljava/io/OutputStream;)V", false);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(-1, -1);
		mv.visitEnd();

		// public Object read(BinaryFormat, InputStream in)
		MethodVisitor read = cw.visitMethod(Opcodes.ACC_PUBLIC, "read", "(Lunknow/serialize/binary/BinaryFormat;Ljava/io/InputStream;)Ljava/lang/Object;", null, IOEXCEPTION);
		read.visitCode();

		// public void write(BinaryFormat, <clazz>, OutputStream) throws IOException
		MethodVisitor write = cw.visitMethod(Opcodes.ACC_PUBLIC, "write", "(Lunknow/serialize/binary/BinaryFormat;" + clazzDescriptor + "Ljava/io/OutputStream;)V", null, IOEXCEPTION);
		write.visitCode();

		CodecBuilder builder = builders.get(cl);

		if (builder == null) {
			if (cl.isEnum())
				builder = CodecBuilder.ENUM;
			else if (INT_COLLECTION != null && INT_COLLECTION.isAssignableFrom(cl))
				builder = CodecBuilder.INTCOLLECTION;
			else if (cl.isArray())
				builder = CodecBuilder.ARRAY;
			else if (Collection.class.isAssignableFrom(cl))
				builder = CodecBuilder.COLLECTION;
			else if (Map.class.isAssignableFrom(cl))
				builder = CodecBuilder.MAP;
		}
		if (builder == null)
			builder = objectBuilder;

		builder.generate(required, cl, write, read);

		// end read
		read.visitInsn(Opcodes.ARETURN);
		read.visitMaxs(-1, -1);
		read.visitEnd();

		// end write
		write.visitInsn(Opcodes.RETURN);
		write.visitMaxs(-1, -1);
		write.visitEnd();

		cw.visitEnd();

		return cw.toByteArray();
	}

	private final CodecBuilder objectBuilder = (required, cl, write, read) -> {
		fields.clear();
		boolFields.clear();
		booleanFields.clear();
		getFields(cl);

		// bundle boolean fields
		if (!boolFields.isEmpty() || !booleanFields.isEmpty()) {
			int i = 0;
			write.visitInsn(Opcodes.ICONST_0);
			write.visitVarInsn(Opcodes.ISTORE, 4); // bundling
			for (Field f : booleanFields) {
				if (i == 8) {
					write.visitVarInsn(Opcodes.ALOAD, 3); // out
					write.visitVarInsn(Opcodes.ILOAD, 4); // bundling
					write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/OutputStream", "write", "(I)V", false);

					write.visitInsn(Opcodes.ICONST_0);
					write.visitVarInsn(Opcodes.ISTORE, 4);
					i = 0;
				}
				write.visitVarInsn(Opcodes.ALOAD, 2); // o
				getValue(write, f);
				Label end = new Label();
				Label save = new Label();
				write.visitJumpInsn(Opcodes.IFNULL, end);

				loadInt(write, 1 << (i + 1));
				write.visitVarInsn(Opcodes.ILOAD, 4); // bundling
				write.visitInsn(Opcodes.IOR);

				write.visitVarInsn(Opcodes.ALOAD, 2); // o
				getValue(write, f);
				write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
				write.visitJumpInsn(Opcodes.IFEQ, save);
				loadInt(write, 1 << i);
				write.visitInsn(Opcodes.IOR);
				write.visitLabel(save);
				write.visitVarInsn(Opcodes.ISTORE, 4); // bundling
				write.visitLabel(end);
				i += 2;
			}
			for (Field f : boolFields) {
				if (i == 8) {
					write.visitVarInsn(Opcodes.ILOAD, 4); // bundling
					write.visitVarInsn(Opcodes.ALOAD, 3); // out
					write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/OutputStream", "write", "(I)V", false);

					write.visitInsn(Opcodes.ICONST_0);
					write.visitVarInsn(Opcodes.ISTORE, 4);
					i = 0;
				}
				write.visitVarInsn(Opcodes.ALOAD, 2); // o
				getValue(write, f);
				Label end = new Label();
				write.visitJumpInsn(Opcodes.IFEQ, end);
				loadInt(write, 1 << i);
				write.visitVarInsn(Opcodes.ILOAD, 4); // bundling
				write.visitInsn(Opcodes.IOR);
				write.visitVarInsn(Opcodes.ISTORE, 4); // bundling
				write.visitLabel(end);
				i++;
			}

			if (i > 0) {
				write.visitVarInsn(Opcodes.ALOAD, 3); // out
				write.visitVarInsn(Opcodes.ILOAD, 4); // bundling
				write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/OutputStream", "write", "(I)V", false);
			}
		}

		for (Field f : fields) {
			Class<?> type = f.getType();
			if (type.isPrimitive()) {
				write.visitVarInsn(Opcodes.ALOAD, 3); // out
				write.visitVarInsn(Opcodes.ALOAD, 2); // o
				getValue(write, f);
				if (type == double.class)
					write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;D)V", false);
				else if (type == float.class)
					write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;F)V", false);
				else if (type == long.class)
					write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;J)V", false);
				else if (type == byte.class)
					write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/OutputStream", "write", "(I)V", false);
				else
					write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);
			} else {
				write.visitVarInsn(Opcodes.ALOAD, 1); // FORMAT
				write.visitVarInsn(Opcodes.ALOAD, 2); // o
				getValue(write, f);
				write.visitVarInsn(Opcodes.ALOAD, 3); // out
				write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/binary/BinaryFormat", "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", true);
			}
		}

		read.visitTypeInsn(Opcodes.NEW, clazz);
		read.visitInsn(Opcodes.DUP);
		read.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz, "<init>", "()V", false);
		read.visitVarInsn(Opcodes.ASTORE, 3); // object

		// unbundle boolean
		if (!boolFields.isEmpty() || !booleanFields.isEmpty()) {
			int i = 0;
			read.visitVarInsn(Opcodes.ALOAD, 2); // in
			read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "read", "(Ljava/io/InputStream;)I", false);
			read.visitVarInsn(Opcodes.ISTORE, 4); // bundle
			for (Field f : booleanFields) {
				if (i == 8) {
					read.visitVarInsn(Opcodes.ALOAD, 2); // in
					read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "read", "(Ljava/io/InputStream;)I", false);
					read.visitVarInsn(Opcodes.ISTORE, 4); // bundle
					i = 0;
				}
				read.visitVarInsn(Opcodes.ALOAD, 3); // object
				read.visitVarInsn(Opcodes.ILOAD, 4); // bundle
				loadInt(read, i);
				read.visitInsn(Opcodes.IUSHR);
				loadInt(read, 0b11);
				read.visitInsn(Opcodes.IAND);
				Label end = new Label();
				Label error = new Label();
				Label rnull = new Label();
				Label rfalse = new Label();
				Label rtrue = new Label();
				read.visitTableSwitchInsn(0, 3, error, rnull, error, rfalse, rtrue);
				read.visitLabel(error);
				read.visitTypeInsn(Opcodes.NEW, "java/io/IOException");
				read.visitInsn(Opcodes.DUP);
				read.visitLdcInsn("corruped stream");
				read.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/IOException", "<init>", "(Ljava/lang/String;)V", false);
				read.visitInsn(Opcodes.ATHROW);
				read.visitLabel(rnull);
				read.visitInsn(Opcodes.ACONST_NULL);
				read.visitJumpInsn(Opcodes.GOTO, end);
				read.visitLabel(rfalse);
				read.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;");
				read.visitJumpInsn(Opcodes.GOTO, end);
				read.visitLabel(rtrue);
				read.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;");
				read.visitLabel(end);
				setValue(read, f);
				i += 2;
			}
			for (Field f : boolFields) {
				if (i == 8) {
					read.visitVarInsn(Opcodes.ALOAD, 2); // in
					read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "read", "(Ljava/io/InputStream;)I", false);
					read.visitVarInsn(Opcodes.ISTORE, 4); // bundle
					i = 0;
				}
				read.visitVarInsn(Opcodes.ALOAD, 3); // object

				read.visitVarInsn(Opcodes.ILOAD, 4); // bundle
				loadInt(read, i);
				read.visitInsn(Opcodes.IUSHR);
				loadInt(read, 0b1);
				read.visitInsn(Opcodes.IAND);
				setValue(read, f);
				i++;
			}
		}

		for (Field f : fields) {
			Class<?> type = f.getType();
			read.visitVarInsn(Opcodes.ALOAD, 3); // object
			if (type.isPrimitive()) {
				read.visitVarInsn(Opcodes.ALOAD, 2); // in
				if (type == double.class)
					read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readDouble", "(Ljava/io/InputStream;)D", false);
				else if (type == float.class)
					read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readFloat", "(Ljava/io/InputStream;)F", false);
				else if (type == long.class)
					read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readLong", "(Ljava/io/InputStream;)J", false);
				else if (type == byte.class)
					read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "read", "(Ljava/io/InputStream;)I", false);
				else
					read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
			} else {
				read.visitVarInsn(Opcodes.ALOAD, 1); // FORMAT
				read.visitVarInsn(Opcodes.ALOAD, 2); // in
				read.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/binary/BinaryFormat", "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", true);
				read.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
			}
			setValue(read, f);
		}
		read.visitVarInsn(Opcodes.ALOAD, 3); // object
	};

	private void setValue(MethodVisitor methodVisitor, Field f) {
		if ((f.getModifiers() & Modifier.PUBLIC) == 1) {
			methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, clazz, f.getName(), Type.getDescriptor(f.getType()));
			return;
		}
		String name = f.getName();
		Class<?> cl = f.getDeclaringClass();
		name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		Method m = getMethod(cl, "set" + name, f.getType());
		if (m == null)
			throw new RuntimeException("Field " + f + " not public and no setter found");
		methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, m.getName(), Type.getMethodDescriptor(m), false);
	}

	private void getValue(MethodVisitor methodVisitor, Field f) {
		if ((f.getModifiers() & Modifier.PUBLIC) == 1) {
			methodVisitor.visitFieldInsn(Opcodes.GETFIELD, clazz, f.getName(), Type.getDescriptor(f.getType()));
			return;
		}
		String name = f.getName();
		Class<?> cl = f.getDeclaringClass();
		Class<?> type = f.getType();
		name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		Method m = getMethod(cl, "get" + name);
		if (m == null && (type == boolean.class || type == Boolean.class))
			m = getMethod(cl, "is" + name);
		if (m == null)
			throw new RuntimeException("Field " + f + " not public and no getter found");
		methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, m.getName(), Type.getMethodDescriptor(m), false);
	}

	private static Method getMethod(Class<?> cl, String name, Class<?>... params) {
		loop: for (Method m : cl.getMethods()) {
			if (!name.equals(m.getName()))
				continue;
			Parameter[] p = m.getParameters();
			if (p.length != params.length)
				continue;
			for (int i = 0; i < p.length; i++) {
				if (!p[i].getType().equals(params[i]))
					continue loop;
			}
			return m;
		}
		return null;
	}

	/**
	 * get all fields in fields list add class in required
	 * 
	 * @param cl
	 */
	private void getFields(Class<?> cl) {
		if (cl == Object.class || cl == null)
			return;
		loop: for (Field f : cl.getDeclaredFields()) {
			if ((f.getModifiers() & MOD) != 0)
				continue;
			for (Field field : fields) {
				if (field.getName().equals(f.getName()))
					continue loop;
			}
			Class<?> type = f.getType();

			if (type == boolean.class)
				boolFields.add(f);
			else if (type == Boolean.class)
				booleanFields.add(f);
			else {
				if (!type.isPrimitive() && type != Object.class)
					required.add(type);
				fields.add(f);
			}
			md.update(f.getName().getBytes(StandardCharsets.UTF_8));
		}
		getFields(cl.getSuperclass());
	}

	/**
	 * load an interger on the stack
	 * 
	 * @param methodVisitor
	 * @param v             value to load
	 */
	private static void loadInt(MethodVisitor methodVisitor, int v) {
		if (v == 1)
			methodVisitor.visitInsn(Opcodes.ICONST_1);
		else if (v == 2)
			methodVisitor.visitInsn(Opcodes.ICONST_2);
		else if (v == 2)
			methodVisitor.visitInsn(Opcodes.ICONST_3);
		else if (v == 2)
			methodVisitor.visitInsn(Opcodes.ICONST_4);
		else if (v == 2)
			methodVisitor.visitInsn(Opcodes.ICONST_5);
		else if (v <= Byte.MAX_VALUE)
			methodVisitor.visitIntInsn(Opcodes.BIPUSH, v);
		else if (v <= Short.MAX_VALUE)
			methodVisitor.visitIntInsn(Opcodes.SIPUSH, v);
		else
			methodVisitor.visitLdcInsn(v);
	}

	private static class Loader extends ClassLoader {
		@SuppressWarnings("unchecked")
		<T> Class<T> define(String name, byte[] clazz) {
			return (Class<T>) defineClass(name, clazz, 0, clazz.length);
		}
	}
}
