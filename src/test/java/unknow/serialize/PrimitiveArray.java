/**
 * 
 */
package unknow.serialize;

import java.util.Arrays;

/**
 * @author unknow
 */
public class PrimitiveArray {
	public boolean[] bool;
	public byte[] b;
	public char[] c;
	public short[] s;
	public int[] i;
	public long[] l;
	public float[] f;
	public double[] d;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(b);
		result = prime * result + Arrays.hashCode(bool);
		result = prime * result + Arrays.hashCode(c);
		result = prime * result + Arrays.hashCode(d);
		result = prime * result + Arrays.hashCode(f);
		result = prime * result + Arrays.hashCode(i);
		result = prime * result + Arrays.hashCode(l);
		result = prime * result + Arrays.hashCode(s);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrimitiveArray other = (PrimitiveArray) obj;
		if (!Arrays.equals(b, other.b))
			return false;
		if (!Arrays.equals(bool, other.bool))
			return false;
		if (!Arrays.equals(c, other.c))
			return false;
		if (!Arrays.equals(d, other.d))
			return false;
		if (!Arrays.equals(f, other.f))
			return false;
		if (!Arrays.equals(i, other.i))
			return false;
		if (!Arrays.equals(l, other.l))
			return false;
		if (!Arrays.equals(s, other.s))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PrimitiveArray [bool=" + Arrays.toString(bool) + ", b=" + Arrays.toString(b) + ", c=" + Arrays.toString(c) + ", s=" + Arrays.toString(s) + ", i=" + Arrays.toString(i) + ", l=" + Arrays.toString(l) + ", f=" + Arrays.toString(f) + ", d=" + Arrays.toString(d) + "]";
	}
}
