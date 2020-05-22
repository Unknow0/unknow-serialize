package unknow.serialize.binary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * Binary serialization for Object. Only registered class can be serialized
 * 
 * @author unknow
 */
public class BinaryFormat {
	private static final Logger log = LoggerFactory.getLogger(BinaryFormat.class);
	private static final int MOD = Modifier.STATIC | Modifier.TRANSIENT;

	/** version of the format */
	private static final byte VERSION = 1;

	private final Map<Class<?>, Codec> codecs;
	private final Map<Integer, Codec> ids;
	private final byte[] hash;

	private BinaryFormat(Map<Class<?>, Codec> codecs, Map<Integer, Codec> ids, byte[] hash) {
		this.codecs = codecs;
		this.ids = ids;
		this.hash = hash;
	}

	/**
	 * @return the hash from all registered class
	 */
	public byte[] hash() {
		return hash;
	}

	/**
	 * read one object from the stream
	 * 
	 * @param in the input
	 * @return the read value
	 * @throws IOException on IOException
	 */
	public Object read(InputStream in) throws IOException {
		int id = IoUtils.readInt(in);
		if (id == 0)
			return null;
		Codec codec = ids.get(id);
		if (codec == null)
			throw new IOException("invalid object id '" + id + "'");
		return codec.read(in);
	}

	/**
	 * write Object to the stream
	 * 
	 * @param o   the object to write
	 * @param out the output
	 * @throws IOException on IOException
	 */
	public void write(Object o, OutputStream out) throws IOException {
		if (o == null) {
			IoUtils.write(out, 0);
			return;
		}
		Codec codec = codecs.get(o.getClass());
		if (codec == null)
			throw new IOException("class not registered " + o.getClass());
		codec.write(o, out);
	}

	private static class Loader extends ClassLoader {
		@SuppressWarnings("unchecked")
		Class<Codec> define(String name, byte[] clazz) {
			return (Class<Codec>) defineClass(name, clazz, 0, clazz.length);
		}
	}

	/**
	 * @return the Builder
	 */
	public static Builder create() {
		return new Builder();
	}

	private static class CodecWrapper implements Codec {
		private int id;
		private Codec codec;

		private CodecWrapper(int id, Codec codec) {
			this.id = id;
			this.codec = codec;
		}

		@Override
		public void write(Object o, OutputStream out) throws IOException {
			IoUtils.write(out, id);
			codec.write(o, out);
		}

		@Override
		public Object read(InputStream in) throws IOException {
			return codec.read(in);
		}

	}

	/**
	 * the BinaryFormat builder
	 * 
	 * @author unknow
	 */
	public static class Builder {
		private static final String[] IOEXCEPTION = new String[] { "java/io/IOException" };
		private static final String[] CODEC = new String[] { Type.getInternalName(Codec.class) };
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

		private final Loader loader = new Loader();
		private final MessageDigest md;

		private final Map<Class<?>, Codec> codecs = new HashMap<>();
		private final Map<Integer, Codec> ids = new HashMap<>();
		private final Map<Class<?>, Pending> pending = new HashMap<>();
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

		private Builder() {
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
		public Builder register(Class<?> cl, Codec codec) {
			codecs.put(cl, new CodecWrapper(nextId++, codec));
			md.update(cl.getName().getBytes(StandardCharsets.UTF_8));
			md.update(codec.getClass().getName().getBytes(StandardCharsets.UTF_8));
			return this;
		}

		/**
		 * generate the codec for class Cl
		 * 
		 * @param cl class to register
		 * @return this
		 */
		public Builder register(Class<?> cl) {
			if (cl.isPrimitive() || cl == Object.class || codecs.containsKey(cl) || pending.containsKey(cl))
				return this;
			required.clear();
			_register(cl);
			while (!required.isEmpty()) {
				temp.addAll(required);
				required.clear();
				temp.removeAll(codecs.keySet());
				temp.removeAll(pending.keySet());
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
			Codec codec = null;
			if (cl == Boolean.class)
				codec = new BooleanCodec(id);
			else if (cl == Byte.class)
				codec = new ByteCodec(id);
			else if (cl == Character.class)
				codec = new CharacterCodec(id);
			else if (cl == Short.class)
				codec = new ShortCodec(id);
			else if (cl == Integer.class)
				codec = new IntegerCodec(id);
			else if (cl == Long.class)
				codec = new LongCodec(id);
			else if (cl == Float.class)
				codec = new FloatCodec(id);
			else if (cl == Double.class)
				codec = new DoubleCodec(id);
			else if (cl == String.class)
				codec = new StringCodec(id);
			else if (cl == boolean[].class)
				codec = new BooleanCodec.Array(id);
			else if (cl == Boolean[].class)
				codec = new BooleanCodec.ArrayBoolean(id);
			else if (cl == byte[].class)
				codec = new ByteCodec.Array(id);
			else if (cl == char[].class)
				codec = new CharacterCodec.Array(id);
			else if (cl == short[].class)
				codec = new ShortCodec.Array(id);
			else if (cl == int[].class)
				codec = new IntegerCodec.Array(id);
			else if (cl == long[].class)
				codec = new LongCodec.Array(id);
			else if (cl == float[].class)
				codec = new FloatCodec.Array(id);
			else if (cl == double[].class)
				codec = new DoubleCodec.Array(id);
			else {
				clazz = Type.getInternalName(cl);
				clazzDescriptor = Type.getDescriptor(cl);
				codecName = "unknow/serialize/binary/codecs/$" + Integer.toString(System.identityHashCode(this), 16) + "$" + Integer.toString(System.identityHashCode(cl), 16);
				if (!cl.isArray())
					codecName += "$" + cl.getSimpleName();
				byte[] bytes = generate(cl, id);
				Class<Codec> define = loader.define(codecName.replace('/', '.'), bytes);
				pending.put(cl, new Pending(id, define));
			}
			if (codec != null) {
				codecs.put(cl, codec);
				ids.put(id, codec);
			}
		}

		private static class Pending {
			private int id;
			private Class<Codec> c;

			public Pending(int id, Class<Codec> c) {
				this.id = id;
				this.c = c;
			}
		}

		/**
		 * Build it
		 * 
		 * @return the usable BinaryFormat
		 * @throws ReflectiveOperationException on Codec creation issues
		 */
		public BinaryFormat build() throws ReflectiveOperationException {
			BinaryFormat binaryFormat = new BinaryFormat(codecs, ids, md.digest());
			for (Map.Entry<Class<?>, Pending> e : pending.entrySet()) {
				Class<?> cl = e.getKey();
				Pending p = e.getValue();
				Codec codec = p.c.getConstructor(BinaryFormat.class).newInstance(binaryFormat);
				codecs.put(cl, codec);
				ids.put(p.id, codec);
			}
			return binaryFormat;
		}

		private byte[] generate(Class<?> cl, int id) {
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
			cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, codecName, null, Type.getInternalName(Object.class), CODEC);
			// private final BinaryFormat FORMAT;
			cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "FORMAT", "Lunknow/serialize/binary/BinaryFormat;", null, null);

			// public <codecClass>(BinaryFormat)
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Lunknow/serialize/binary/BinaryFormat;)V", null, null);
			mv.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			mv.visitFieldInsn(Opcodes.PUTFIELD, codecName, "FORMAT", "Lunknow/serialize/binary/BinaryFormat;");
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(-1, -1);
			mv.visitEnd();

			// public void write(Object, OutputStream) throws IOException
			mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", null, IOEXCEPTION);
			mv.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 1);
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
			mv.visitTypeInsn(Opcodes.CHECKCAST, clazz);
			mv.visitVarInsn(Opcodes.ALOAD, 2);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, codecName, "write", "(" + clazzDescriptor + "Ljava/io/OutputStream;)V", false);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(-1, -1);
			mv.visitEnd();

			// public Object read(InputStream in)
			MethodVisitor read = cw.visitMethod(Opcodes.ACC_PUBLIC, "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", null, IOEXCEPTION);
			read.visitCode();

			// public void write(<clazz>, OutputStream) throws IOException
			MethodVisitor write = cw.visitMethod(Opcodes.ACC_PUBLIC, "write", "(" + clazzDescriptor + "Ljava/io/OutputStream;)V", null, IOEXCEPTION);
			write.visitCode();
			write.visitVarInsn(Opcodes.ALOAD, 2); // out
			loadInt(write, id);
			write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

			if (cl.isEnum())
				generateEnum(write, read);
			else if (INT_COLLECTION != null && INT_COLLECTION.isAssignableFrom(cl))
				generateIntCollection(write, read);
			else if (cl.isArray())
				generateArray(cl, write, read);
			else if (Collection.class.isAssignableFrom(cl))
				generateCollection(write, read);
			else if (Map.class.isAssignableFrom(cl))
				generateMap(write, read);
			else
				generateObject(cl, write, read);

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

		private void generateEnum(MethodVisitor write, MethodVisitor read) {
			write.visitVarInsn(Opcodes.ALOAD, 2); // out
			write.visitVarInsn(Opcodes.ALOAD, 1); // o
			write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "ordinal", "()I", false);
			write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

			read.visitMethodInsn(Opcodes.INVOKESTATIC, clazz, "values", "()[" + clazzDescriptor, false);
			read.visitVarInsn(Opcodes.ALOAD, 1); // in
			read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
			read.visitInsn(Opcodes.AALOAD);
		}

		private void generateArray(Class<?> cl, MethodVisitor write, MethodVisitor read) {
			cl = cl.getComponentType();
			required.add(cl);

			write.visitVarInsn(Opcodes.ALOAD, 0);
			write.visitFieldInsn(Opcodes.GETFIELD, codecName, "FORMAT", "Lunknow/serialize/binary/BinaryFormat;");
			write.visitVarInsn(Opcodes.ASTORE, 3); // FORMAT

			write.visitVarInsn(Opcodes.ALOAD, 2); // out
			write.visitVarInsn(Opcodes.ALOAD, 1); // o
			write.visitInsn(Opcodes.ARRAYLENGTH);
			write.visitInsn(Opcodes.DUP);
			write.visitVarInsn(Opcodes.ISTORE, 5); // len
			write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);
			write.visitInsn(Opcodes.ICONST_0);
			write.visitVarInsn(Opcodes.ISTORE, 4); // i
			Label end = new Label();
			Label start = new Label();
			write.visitLabel(start);
			write.visitVarInsn(Opcodes.ILOAD, 5); // len
			write.visitVarInsn(Opcodes.ILOAD, 4); // i
			write.visitJumpInsn(Opcodes.IF_ICMPEQ, end);
			// load array[i]
			write.visitVarInsn(Opcodes.ALOAD, 3); // FORMAT
			write.visitVarInsn(Opcodes.ALOAD, 1); // o
			write.visitVarInsn(Opcodes.ILOAD, 4); // i
			write.visitInsn(Opcodes.AALOAD);
			write.visitVarInsn(Opcodes.ALOAD, 2); // out
			write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "unknow/serialize/binary/BinaryFormat", "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", false);

			write.visitIincInsn(4, 1);
			write.visitJumpInsn(Opcodes.GOTO, start);
			write.visitLabel(end);

			read.visitVarInsn(Opcodes.ALOAD, 0);
			read.visitFieldInsn(Opcodes.GETFIELD, codecName, "FORMAT", "Lunknow/serialize/binary/BinaryFormat;");
			read.visitVarInsn(Opcodes.ASTORE, 2); // FORMAT

			read.visitVarInsn(Opcodes.ALOAD, 1); // in
			read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
			read.visitInsn(Opcodes.DUP);
			read.visitVarInsn(Opcodes.ISTORE, 3); // len
			read.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(cl));
			read.visitVarInsn(Opcodes.ASTORE, 4); // array
			read.visitInsn(Opcodes.ICONST_0);
			read.visitVarInsn(Opcodes.ISTORE, 5); // i
			end = new Label();
			start = new Label();
			read.visitLabel(start);
			read.visitVarInsn(Opcodes.ILOAD, 3); // len
			read.visitVarInsn(Opcodes.ILOAD, 5); // i
			read.visitJumpInsn(Opcodes.IF_ICMPEQ, end);
			// load array[i]
			read.visitVarInsn(Opcodes.ALOAD, 4); // array
			read.visitVarInsn(Opcodes.ILOAD, 5); // i

			read.visitVarInsn(Opcodes.ALOAD, 2); // FORMAT
			read.visitVarInsn(Opcodes.ALOAD, 1); // in
			read.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "unknow/serialize/binary/BinaryFormat", "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", false);
			read.visitInsn(Opcodes.AASTORE);

			read.visitIincInsn(5, 1); // i++
			read.visitJumpInsn(Opcodes.GOTO, start);
			read.visitLabel(end);
			read.visitVarInsn(Opcodes.ALOAD, 4); // array
		}

		private void generateIntCollection(MethodVisitor write, MethodVisitor read) {
			write.visitVarInsn(Opcodes.ALOAD, 2); // out
			write.visitVarInsn(Opcodes.ALOAD, 1); // o
			write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "size", "()I", false);
			write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

			write.visitVarInsn(Opcodes.ALOAD, 1);
			write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "iterator", "()Lunknow/common/data/IntIterator;", false);
			write.visitVarInsn(Opcodes.ASTORE, 3); // Iterator

			Label start = new Label();
			Label end = new Label();
			write.visitLabel(start);
			write.visitVarInsn(Opcodes.ALOAD, 3); // iterator
			write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/common/data/IntIterator", "hasNext", "()Z", true);
			write.visitJumpInsn(Opcodes.IFEQ, end);

			write.visitVarInsn(Opcodes.ALOAD, 2); // out
			write.visitVarInsn(Opcodes.ALOAD, 3); // iterator
			write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/common/data/IntIterator", "nextInt", "()I", true);
			write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

			write.visitJumpInsn(Opcodes.GOTO, start);
			write.visitLabel(end);

			read.visitTypeInsn(Opcodes.NEW, clazz);
			read.visitInsn(Opcodes.DUP);
			read.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz, "<init>", "()V", false);
			read.visitVarInsn(Opcodes.ASTORE, 2); // collection

			read.visitVarInsn(Opcodes.ALOAD, 1); // in
			read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
			read.visitVarInsn(Opcodes.ISTORE, 3); // len

			end = new Label();
			start = new Label();
			read.visitVarInsn(Opcodes.ILOAD, 3); // len
			read.visitJumpInsn(Opcodes.IFEQ, end);
			read.visitLabel(start);

			read.visitVarInsn(Opcodes.ALOAD, 2); // collection
			read.visitVarInsn(Opcodes.ALOAD, 1); // in
			read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
			read.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "add", "(I)Z", false);
			read.visitInsn(Opcodes.POP);

			read.visitIincInsn(3, -1); // len--
			read.visitVarInsn(Opcodes.ILOAD, 3); // len
			read.visitJumpInsn(Opcodes.IFNE, start);
			read.visitLabel(end);
			read.visitVarInsn(Opcodes.ALOAD, 2); // collection
		}

		private void generateCollection(MethodVisitor write, MethodVisitor read) {
			write.visitVarInsn(Opcodes.ALOAD, 0);
			write.visitFieldInsn(Opcodes.GETFIELD, codecName, "FORMAT", "Lunknow/serialize/binary/BinaryFormat;");
			write.visitVarInsn(Opcodes.ASTORE, 3); // FORMAT

			write.visitVarInsn(Opcodes.ALOAD, 2); // out
			write.visitVarInsn(Opcodes.ALOAD, 1); // o
			write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "size", "()I", false);
			write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

			write.visitVarInsn(Opcodes.ALOAD, 1);
			write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "iterator", "()Ljava/util/Iterator;", false);
			write.visitVarInsn(Opcodes.ASTORE, 4); // Iterator

			Label start = new Label();
			Label end = new Label();
			write.visitLabel(start);
			write.visitVarInsn(Opcodes.ALOAD, 4); // iterator
			write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
			write.visitJumpInsn(Opcodes.IFEQ, end);

			write.visitVarInsn(Opcodes.ALOAD, 3); // FORMAT
			write.visitVarInsn(Opcodes.ALOAD, 4); // iterator
			write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
			write.visitVarInsn(Opcodes.ALOAD, 2); // out
			write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "unknow/serialize/binary/BinaryFormat", "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", false);

			write.visitJumpInsn(Opcodes.GOTO, start);
			write.visitLabel(end);

			read.visitVarInsn(Opcodes.ALOAD, 0);
			read.visitFieldInsn(Opcodes.GETFIELD, codecName, "FORMAT", "Lunknow/serialize/binary/BinaryFormat;");
			read.visitVarInsn(Opcodes.ASTORE, 2); // FORMAT

			read.visitTypeInsn(Opcodes.NEW, clazz);
			read.visitInsn(Opcodes.DUP);
			read.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz, "<init>", "()V", false);
			read.visitVarInsn(Opcodes.ASTORE, 3); // collection

			read.visitVarInsn(Opcodes.ALOAD, 1); // in
			read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
			read.visitVarInsn(Opcodes.ISTORE, 4); // len

			read.visitInsn(Opcodes.ICONST_0);
			read.visitVarInsn(Opcodes.ISTORE, 5); // i
			end = new Label();
			start = new Label();
			read.visitLabel(start);
			read.visitVarInsn(Opcodes.ILOAD, 4); // len
			read.visitVarInsn(Opcodes.ILOAD, 5); // i
			read.visitJumpInsn(Opcodes.IF_ICMPEQ, end);

			read.visitVarInsn(Opcodes.ALOAD, 3); // collection
			read.visitVarInsn(Opcodes.ALOAD, 2); // FORMAT
			read.visitVarInsn(Opcodes.ALOAD, 1); // in
			read.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "unknow/serialize/binary/BinaryFormat", "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", false);
			read.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "add", "(Ljava/lang/Object;)Z", false);
			read.visitInsn(Opcodes.POP);

			read.visitIincInsn(5, 1); // i++
			read.visitJumpInsn(Opcodes.GOTO, start);
			read.visitLabel(end);
			read.visitVarInsn(Opcodes.ALOAD, 3); // collection
		}

		private void generateMap(MethodVisitor write, MethodVisitor read) {
			write.visitVarInsn(Opcodes.ALOAD, 0);
			write.visitFieldInsn(Opcodes.GETFIELD, codecName, "FORMAT", "Lunknow/serialize/binary/BinaryFormat;");
			write.visitVarInsn(Opcodes.ASTORE, 3); // FORMAT

			write.visitVarInsn(Opcodes.ALOAD, 2); // out
			write.visitVarInsn(Opcodes.ALOAD, 1); // o
			write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "size", "()I", false);
			write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

			write.visitVarInsn(Opcodes.ALOAD, 1);
			write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "entrySet", "()Ljava/util/Set;", false);
			write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;", true);
			write.visitVarInsn(Opcodes.ASTORE, 4); // Iterator

			Label start = new Label();
			Label end = new Label();
			write.visitLabel(start);
			write.visitVarInsn(Opcodes.ALOAD, 4); // iterator
			write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
			write.visitJumpInsn(Opcodes.IFEQ, end);

			write.visitVarInsn(Opcodes.ALOAD, 4);
			write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
			write.visitTypeInsn(Opcodes.CHECKCAST, "java/util/Map$Entry");
			write.visitVarInsn(Opcodes.ASTORE, 5); // entry

			write.visitVarInsn(Opcodes.ALOAD, 3); // FORMAT
			write.visitVarInsn(Opcodes.ALOAD, 5); // entry
			write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;", true);
			write.visitVarInsn(Opcodes.ALOAD, 2); // out
			write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "unknow/serialize/binary/BinaryFormat", "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", false);

			write.visitVarInsn(Opcodes.ALOAD, 3); // FORMAT
			write.visitVarInsn(Opcodes.ALOAD, 5); // entry
			write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;", true);
			write.visitVarInsn(Opcodes.ALOAD, 2); // out
			write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "unknow/serialize/binary/BinaryFormat", "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", false);

			write.visitJumpInsn(Opcodes.GOTO, start);
			write.visitLabel(end);

			read.visitVarInsn(Opcodes.ALOAD, 0);
			read.visitFieldInsn(Opcodes.GETFIELD, codecName, "FORMAT", "Lunknow/serialize/binary/BinaryFormat;");
			read.visitVarInsn(Opcodes.ASTORE, 2); // FORMAT

			read.visitTypeInsn(Opcodes.NEW, clazz);
			read.visitInsn(Opcodes.DUP);
			read.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz, "<init>", "()V", false);
			read.visitVarInsn(Opcodes.ASTORE, 3); // map

			read.visitVarInsn(Opcodes.ALOAD, 1); // in
			read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
			read.visitVarInsn(Opcodes.ISTORE, 4); // len

			end = new Label();
			start = new Label();
			read.visitLabel(start);
			read.visitVarInsn(Opcodes.ILOAD, 4); // len
			read.visitJumpInsn(Opcodes.IFEQ, end);

			read.visitVarInsn(Opcodes.ALOAD, 3); // map
			read.visitVarInsn(Opcodes.ALOAD, 2); // FORMAT
			read.visitVarInsn(Opcodes.ALOAD, 1); // in
			read.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "unknow/serialize/binary/BinaryFormat", "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", false);
			read.visitVarInsn(Opcodes.ALOAD, 2); // FORMAT
			read.visitVarInsn(Opcodes.ALOAD, 1); // in
			read.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "unknow/serialize/binary/BinaryFormat", "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", false);
			read.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
			read.visitInsn(Opcodes.POP);

			read.visitIincInsn(4, -1); // len--
			read.visitJumpInsn(Opcodes.GOTO, start);
			read.visitLabel(end);
			read.visitVarInsn(Opcodes.ALOAD, 3); // map
		}

		private void generateObject(Class<?> cl, MethodVisitor write, MethodVisitor read) {
			fields.clear();
			boolFields.clear();
			booleanFields.clear();
			getFields(cl);

			write.visitVarInsn(Opcodes.ALOAD, 0);
			write.visitFieldInsn(Opcodes.GETFIELD, codecName, "FORMAT", "Lunknow/serialize/binary/BinaryFormat;");
			write.visitVarInsn(Opcodes.ASTORE, 3); // FORMAT
			// bundle boolean fields
			if (!boolFields.isEmpty() || !booleanFields.isEmpty()) {
				int i = 0;
				write.visitInsn(Opcodes.ICONST_0);
				write.visitVarInsn(Opcodes.ISTORE, 4); // bundling
				for (Field f : booleanFields) {
					if (i == 8) {
						write.visitVarInsn(Opcodes.ALOAD, 2); // out
						write.visitVarInsn(Opcodes.ILOAD, 4); // bundling
						write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/OutputStream", "write", "(I)V", false);

						write.visitInsn(Opcodes.ICONST_0);
						write.visitVarInsn(Opcodes.ISTORE, 4);
						i = 0;
					}
					write.visitVarInsn(Opcodes.ALOAD, 1); // o
					getValue(write, f);
					Label end = new Label();
					Label save = new Label();
					write.visitJumpInsn(Opcodes.IFNULL, end);

					loadInt(write, 1 << (i + 1));
					write.visitVarInsn(Opcodes.ILOAD, 4); // bundling
					write.visitInsn(Opcodes.IOR);

					write.visitVarInsn(Opcodes.ALOAD, 1); // o
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
						write.visitVarInsn(Opcodes.ALOAD, 2); // out
						write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/OutputStream", "write", "(I)V", false);

						write.visitInsn(Opcodes.ICONST_0);
						write.visitVarInsn(Opcodes.ISTORE, 4);
						i = 0;
					}
					write.visitVarInsn(Opcodes.ALOAD, 1); // o
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
					write.visitVarInsn(Opcodes.ALOAD, 2); // out
					write.visitVarInsn(Opcodes.ILOAD, 4); // bundling
					write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/OutputStream", "write", "(I)V", false);
				}
			}

			for (Field f : fields) {
				Class<?> type = f.getType();
				if (type.isPrimitive()) {
					write.visitVarInsn(Opcodes.ALOAD, 2); // out
					write.visitVarInsn(Opcodes.ALOAD, 1); // o
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
					write.visitVarInsn(Opcodes.ALOAD, 3); // FORMAT
					write.visitVarInsn(Opcodes.ALOAD, 1); // o
					getValue(write, f);
					write.visitVarInsn(Opcodes.ALOAD, 2); // out
					write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "unknow/serialize/binary/BinaryFormat", "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", false);
				}
			}

			read.visitVarInsn(Opcodes.ALOAD, 0);
			read.visitFieldInsn(Opcodes.GETFIELD, codecName, "FORMAT", "Lunknow/serialize/binary/BinaryFormat;");
			read.visitVarInsn(Opcodes.ASTORE, 2); // FORMAT

			read.visitTypeInsn(Opcodes.NEW, clazz);
			read.visitInsn(Opcodes.DUP);
			read.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz, "<init>", "()V", false);
			read.visitVarInsn(Opcodes.ASTORE, 3); // object

			// unbundle boolean
			if (!boolFields.isEmpty() || !booleanFields.isEmpty()) {
				int i = 0;
				read.visitVarInsn(Opcodes.ALOAD, 1); // in
				read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "read", "(Ljava/io/InputStream;)I", false);
				read.visitVarInsn(Opcodes.ISTORE, 4); // bundle
				for (Field f : booleanFields) {
					if (i == 8) {
						read.visitVarInsn(Opcodes.ALOAD, 1); // in
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
						read.visitVarInsn(Opcodes.ALOAD, 1); // in
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
					read.visitVarInsn(Opcodes.ALOAD, 1); // in
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
					read.visitVarInsn(Opcodes.ALOAD, 2); // FORMAT
					read.visitVarInsn(Opcodes.ALOAD, 1); // in
					read.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "unknow/serialize/binary/BinaryFormat", "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", false);
					read.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
				}
				setValue(read, f);
			}
			read.visitVarInsn(Opcodes.ALOAD, 3); // object
		}

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
	}
}
