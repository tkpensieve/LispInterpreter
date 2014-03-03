package tk.sp14.pl.domain;

public class ComplexSExpression implements SExpression {
	private SExpression left;
	private SExpression right;

	public ComplexSExpression(SExpression left, SExpression right) {
		super();
		this.left = left;
		this.right = right;
	}

	public SExpression getLeft() {
		return left;
	}
	public void setLeft(SExpression left) {
		this.left = left;
	}
	public SExpression getRight() {
		return right;
	}
	public void setRight(SExpression right) {
		this.right = right;
	}
}
