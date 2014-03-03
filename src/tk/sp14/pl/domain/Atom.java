package tk.sp14.pl.domain;

public class Atom implements SExpression {
	private AtomType type;
	private Object value;

	public Atom(Object value, AtomType type) {
		super();
		this.value = value;
		this.type = type;
	}
	
	public boolean isNumber() {
		return this.type == AtomType.NUMBERS;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		Atom other = (Atom) obj;
		if (type != other.type)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	public AtomType getType() {
		return type;
	}
	public void setType(AtomType type) {
		this.type = type;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
}
