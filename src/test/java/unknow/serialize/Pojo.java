/**
 * 
 */
package unknow.serialize;

import java.util.Arrays;
import java.util.List;

/**
 * @author unknow
 */
public class Pojo {
	private int value;
	private List<String> list;
	private int[][] multiArray;
	private Inner inner;

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public List<String> getList() {
		return list;
	}

	public void setList(List<String> list) {
		this.list = list;
	}

	public int[][] getMultiArray() {
		return multiArray;
	}

	public void setMultiArray(int[][] multiArray) {
		this.multiArray = multiArray;
	}

	public Inner getInner() {
		return inner;
	}

	public void setInner(Inner inner) {
		this.inner = inner;
	}

	public void addInner(Inner inner) {
		inner.parent = this;
		inner.next = this.inner;
		this.inner = inner;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((inner == null) ? 0 : inner.hashCode());
		result = prime * result + ((list == null) ? 0 : list.hashCode());
		result = prime * result + Arrays.deepHashCode(multiArray);
		result = prime * result + value;
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
		Pojo other = (Pojo) obj;
		if (inner == null) {
			if (other.inner != null)
				return false;
		} else if (!inner.equals(other.inner))
			return false;
		if (list == null) {
			if (other.list != null)
				return false;
		} else if (!list.equals(other.list))
			return false;
		if (!Arrays.deepEquals(multiArray, other.multiArray))
			return false;
		if (value != other.value)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Pojo [value=" + value + ", list=" + list + ", multiArray=" + Arrays.toString(multiArray) + ", inner=" + inner + "]";
	}

	public static class Inner {
		public transient Pojo parent;
		public int value;
		public Inner next;

		public Inner() {
		}

		public Inner(int value) {
			this.value = value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((next == null) ? 0 : next.hashCode());
			result = prime * result + value;
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
			Inner other = (Inner) obj;
			if (next == null) {
				if (other.next != null)
					return false;
			} else if (!next.equals(other.next))
				return false;
			if (value != other.value)
				return false;
			return true;
		}
	}
}
