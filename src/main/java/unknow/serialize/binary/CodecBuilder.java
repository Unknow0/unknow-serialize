/**
 * 
 */
package unknow.serialize.binary;

import java.util.Collection;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * ASM builder for codec
 * 
 * @author unknow
 */
public interface CodecBuilder {
	/**
	 * generate a codec for this class
	 * 
	 * @param required collection of dependency
	 * @param clazz    the class to build codec for
	 * @param write    the Codec.write method
	 * @param read     the Codec.read method
	 */
	void generate(Collection<Class<?>> required, Class<?> clazz, MethodVisitor write, MethodVisitor read);

	/**
	 * generator for enum class
	 */
	public static final CodecBuilder ENUM = (required, cl, write, read) -> {
		String clazz = Type.getInternalName(cl);

		write.visitVarInsn(Opcodes.ALOAD, 3); // out
		write.visitVarInsn(Opcodes.ALOAD, 2); // o
		write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "ordinal", "()I", false);
		write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

		read.visitMethodInsn(Opcodes.INVOKESTATIC, clazz, "values", "()[" + Type.getDescriptor(cl), false);
		read.visitVarInsn(Opcodes.ALOAD, 1); // in
		read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
		read.visitInsn(Opcodes.AALOAD);
	};

	/**
	 * generator for object array
	 */
	public static final CodecBuilder ARRAY = (required, cl, write, read) -> {
		cl = cl.getComponentType();
		required.add(cl);

		write.visitVarInsn(Opcodes.ALOAD, 3); // out
		write.visitVarInsn(Opcodes.ALOAD, 2); // o
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
		write.visitVarInsn(Opcodes.ALOAD, 1); // FORMAT
		write.visitVarInsn(Opcodes.ALOAD, 2); // o
		write.visitVarInsn(Opcodes.ILOAD, 4); // i
		write.visitInsn(Opcodes.AALOAD);
		write.visitVarInsn(Opcodes.ALOAD, 3); // out
		write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/binary/BinaryFormat", "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", true);

		write.visitIincInsn(4, 1);
		write.visitJumpInsn(Opcodes.GOTO, start);
		write.visitLabel(end);

		read.visitVarInsn(Opcodes.ALOAD, 2); // in
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

		read.visitVarInsn(Opcodes.ALOAD, 1); // FORMAT
		read.visitVarInsn(Opcodes.ALOAD, 2); // in
		read.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/binary/BinaryFormat", "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", true);
		read.visitInsn(Opcodes.AASTORE);

		read.visitIincInsn(5, 1); // i++
		read.visitJumpInsn(Opcodes.GOTO, start);
		read.visitLabel(end);
		read.visitVarInsn(Opcodes.ALOAD, 4); // array
	};
	/**
	 * generator for IntCollection from unknow-data
	 */
	public static final CodecBuilder INTCOLLECTION = (required, cl, write, read) -> {
		String clazz = Type.getInternalName(cl);

		write.visitVarInsn(Opcodes.ALOAD, 3); // out
		write.visitVarInsn(Opcodes.ALOAD, 2); // o
		write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "size", "()I", false);
		write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

		write.visitVarInsn(Opcodes.ALOAD, 2);
		write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "iterator", "()Lunknow/common/data/IntIterator;", false);
		write.visitVarInsn(Opcodes.ASTORE, 4); // Iterator

		Label start = new Label();
		Label end = new Label();
		write.visitLabel(start);
		write.visitVarInsn(Opcodes.ALOAD, 4); // iterator
		write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/common/data/IntIterator", "hasNext", "()Z", true);
		write.visitJumpInsn(Opcodes.IFEQ, end);

		write.visitVarInsn(Opcodes.ALOAD, 3); // out
		write.visitVarInsn(Opcodes.ALOAD, 4); // iterator
		write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/common/data/IntIterator", "nextInt", "()I", true);
		write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

		write.visitJumpInsn(Opcodes.GOTO, start);
		write.visitLabel(end);

		read.visitTypeInsn(Opcodes.NEW, clazz);
		read.visitInsn(Opcodes.DUP);
		read.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz, "<init>", "()V", false);
		read.visitVarInsn(Opcodes.ASTORE, 3); // collection

		read.visitVarInsn(Opcodes.ALOAD, 2); // in
		read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
		read.visitVarInsn(Opcodes.ISTORE, 4); // len

		end = new Label();
		start = new Label();
		read.visitVarInsn(Opcodes.ILOAD, 4); // len
		read.visitJumpInsn(Opcodes.IFEQ, end);
		read.visitLabel(start);

		read.visitVarInsn(Opcodes.ALOAD, 3); // collection
		read.visitVarInsn(Opcodes.ALOAD, 2); // in
		read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
		read.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "add", "(I)Z", false);
		read.visitInsn(Opcodes.POP);

		read.visitIincInsn(4, -1); // len--
		read.visitVarInsn(Opcodes.ILOAD, 4); // len
		read.visitJumpInsn(Opcodes.IFNE, start);
		read.visitLabel(end);
		read.visitVarInsn(Opcodes.ALOAD, 3); // collection
	};
	public static final CodecBuilder COLLECTION = (required, cl, write, read) -> {
		String clazz = Type.getInternalName(cl);

		write.visitVarInsn(Opcodes.ALOAD, 3); // out
		write.visitVarInsn(Opcodes.ALOAD, 2); // o
		write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "size", "()I", false);
		write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

		write.visitVarInsn(Opcodes.ALOAD, 2); // o
		write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "iterator", "()Ljava/util/Iterator;", false);
		write.visitVarInsn(Opcodes.ASTORE, 4); // Iterator

		Label start = new Label();
		Label end = new Label();
		write.visitLabel(start);
		write.visitVarInsn(Opcodes.ALOAD, 4); // iterator
		write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
		write.visitJumpInsn(Opcodes.IFEQ, end);

		write.visitVarInsn(Opcodes.ALOAD, 1); // FORMAT
		write.visitVarInsn(Opcodes.ALOAD, 4); // iterator
		write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
		write.visitVarInsn(Opcodes.ALOAD, 3); // out
		write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/binary/BinaryFormat", "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", true);

		write.visitJumpInsn(Opcodes.GOTO, start);
		write.visitLabel(end);

		read.visitTypeInsn(Opcodes.NEW, clazz);
		read.visitInsn(Opcodes.DUP);
		read.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz, "<init>", "()V", false);
		read.visitVarInsn(Opcodes.ASTORE, 3); // collection

		read.visitVarInsn(Opcodes.ALOAD, 2); // in
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
		read.visitVarInsn(Opcodes.ALOAD, 1); // FORMAT
		read.visitVarInsn(Opcodes.ALOAD, 2); // in
		read.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/binary/BinaryFormat", "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", true);
		read.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "add", "(Ljava/lang/Object;)Z", false);
		read.visitInsn(Opcodes.POP);

		read.visitIincInsn(5, 1); // i++
		read.visitJumpInsn(Opcodes.GOTO, start);
		read.visitLabel(end);
		read.visitVarInsn(Opcodes.ALOAD, 3); // collection
	};
	public static final CodecBuilder MAP = (required, cl, write, read) -> {
		String clazz = Type.getInternalName(cl);

		write.visitVarInsn(Opcodes.ALOAD, 3); // out
		write.visitVarInsn(Opcodes.ALOAD, 2); // o
		write.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "size", "()I", false);
		write.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "write", "(Ljava/io/OutputStream;I)V", false);

		write.visitVarInsn(Opcodes.ALOAD, 2); // o
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

		write.visitVarInsn(Opcodes.ALOAD, 1); // FORMAT
		write.visitVarInsn(Opcodes.ALOAD, 5); // entry
		write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;", true);
		write.visitVarInsn(Opcodes.ALOAD, 3); // out
		write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/binary/BinaryFormat", "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", true);

		write.visitVarInsn(Opcodes.ALOAD, 1); // FORMAT
		write.visitVarInsn(Opcodes.ALOAD, 5); // entry
		write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;", true);
		write.visitVarInsn(Opcodes.ALOAD, 3); // out
		write.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/binary/BinaryFormat", "write", "(Ljava/lang/Object;Ljava/io/OutputStream;)V", true);

		write.visitJumpInsn(Opcodes.GOTO, start);
		write.visitLabel(end);

		read.visitTypeInsn(Opcodes.NEW, clazz);
		read.visitInsn(Opcodes.DUP);
		read.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz, "<init>", "()V", false);
		read.visitVarInsn(Opcodes.ASTORE, 3); // map

		read.visitVarInsn(Opcodes.ALOAD, 2); // in
		read.visitMethodInsn(Opcodes.INVOKESTATIC, "unknow/serialize/binary/IoUtils", "readInt", "(Ljava/io/InputStream;)I", false);
		read.visitVarInsn(Opcodes.ISTORE, 4); // len

		end = new Label();
		start = new Label();
		read.visitLabel(start);
		read.visitVarInsn(Opcodes.ILOAD, 4); // len
		read.visitJumpInsn(Opcodes.IFEQ, end);

		read.visitVarInsn(Opcodes.ALOAD, 3); // map
		read.visitVarInsn(Opcodes.ALOAD, 1); // FORMAT
		read.visitVarInsn(Opcodes.ALOAD, 2); // in
		read.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/binary/BinaryFormat", "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", true);
		read.visitVarInsn(Opcodes.ALOAD, 1); // FORMAT
		read.visitVarInsn(Opcodes.ALOAD, 2); // in
		read.visitMethodInsn(Opcodes.INVOKEINTERFACE, "unknow/serialize/binary/BinaryFormat", "read", "(Ljava/io/InputStream;)Ljava/lang/Object;", true);
		read.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clazz, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
		read.visitInsn(Opcodes.POP);

		read.visitIincInsn(4, -1); // len--
		read.visitJumpInsn(Opcodes.GOTO, start);
		read.visitLabel(end);
		read.visitVarInsn(Opcodes.ALOAD, 3); // map
	};
}
