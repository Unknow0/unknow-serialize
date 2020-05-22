/**
 * 
 */
package unknow.serialize;

/**
 * @author unknow
 */
public class Primitive {
	public boolean bool;
	public byte b;
	public char c;
	public short s;
	public int i;
	public long l;
	public float f;
	public double d;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + b;
		result = prime * result + (bool ? 1231 : 1237);
		result = prime * result + c;
		long temp;
		temp = Double.doubleToLongBits(d);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Float.floatToIntBits(f);
		result = prime * result + i;
		result = prime * result + (int) (l ^ (l >>> 32));
		result = prime * result + s;
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
		Primitive other = (Primitive) obj;
		if (b != other.b)
			return false;
		if (bool != other.bool)
			return false;
		if (c != other.c)
			return false;
		if (Double.doubleToLongBits(d) != Double.doubleToLongBits(other.d))
			return false;
		if (Float.floatToIntBits(f) != Float.floatToIntBits(other.f))
			return false;
		if (i != other.i)
			return false;
		if (l != other.l)
			return false;
		if (s != other.s)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Primitive [bool=" + bool + ", b=" + b + ", c=" + c + ", s=" + s + ", i=" + i + ", l=" + l + ", f=" + f + ", d=" + d + "]";
	}
}
