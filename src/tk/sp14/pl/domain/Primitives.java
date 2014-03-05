package tk.sp14.pl.domain;

import tk.sp14.pl.error.InvalidOperationException;

public class Primitives {
	public static Atom T = new Atom("T", AtomType.TERMINATORS);
	public static Atom NIL = new Atom("NIL", AtomType.TERMINATORS);
	
	public SExpression CAR(SExpression s) throws InvalidOperationException{
		if(!(s instanceof ComplexSExpression))
			throw new InvalidOperationException();
		return ((ComplexSExpression)s).getLeft();
	}
	
	public SExpression CDR(SExpression s) throws InvalidOperationException{
		if(!(s instanceof ComplexSExpression))
			throw new InvalidOperationException();
		return ((ComplexSExpression)s).getRight();
	}
	
	public ComplexSExpression CONS(SExpression left, SExpression right){
		return new ComplexSExpression(left, right);
	}
	
	public Atom ATOM(SExpression sExp){
		return (sExp instanceof Atom) ? T : NIL;
	}
	
	public Atom EQ(SExpression a, SExpression b) throws InvalidOperationException{
		if(!(a instanceof Atom) || !(b instanceof Atom))
			throw new InvalidOperationException();
		if(a.equals(b))
			return T;
		else
			return NIL;
	}
	
	public Atom NULL(SExpression a){
		return (a instanceof Atom && ((Atom)a).equals(NIL)) ? T : NIL;
	}
	
	public Atom INT(SExpression a){
		return (a instanceof Atom && ((Atom)a).isNumber()) ? T : NIL;
	}
	
	public Atom PLUS(SExpression a, SExpression b) throws InvalidOperationException{
		if(!(a instanceof Atom) || !(b instanceof Atom))
			throw new InvalidOperationException();
		int valA = Integer.parseInt(((Atom)a).getValue());
		int valB = Integer.parseInt(((Atom)b).getValue());
		return new Atom(Integer.toString(valA+valB), AtomType.NUMBERS);
	}
	
	public Atom MINUS(SExpression a, SExpression b) throws InvalidOperationException{
		if(!(a instanceof Atom) || !(b instanceof Atom))
			throw new InvalidOperationException();
		int valA = Integer.parseInt(((Atom)a).getValue());
		int valB = Integer.parseInt(((Atom)b).getValue());
		return new Atom(Integer.toString(valA - valB), AtomType.NUMBERS);
	}
	
	public Atom LESS(SExpression a, SExpression b) throws InvalidOperationException{
		if(!(a instanceof Atom) || !(b instanceof Atom))
			throw new InvalidOperationException();
		int valA = Integer.parseInt(((Atom)a).getValue());
		int valB = Integer.parseInt(((Atom)b).getValue());
		return valA < valB ? T : NIL;
	}
	
	public Atom GREATER(SExpression a, SExpression b) throws InvalidOperationException{
		if(!(a instanceof Atom) || !(b instanceof Atom))
			throw new InvalidOperationException();
		int valA = Integer.parseInt(((Atom)a).getValue());
		int valB = Integer.parseInt(((Atom)b).getValue());
		return valA > valB ? T : NIL;
	}
	
	public Atom TIMES(SExpression a, SExpression b) throws InvalidOperationException{
		if(!(a instanceof Atom) || !(b instanceof Atom))
			throw new InvalidOperationException();
		int valA = Integer.parseInt(((Atom)a).getValue());
		int valB = Integer.parseInt(((Atom)b).getValue());
		return new Atom(Integer.toString(valA*valB), AtomType.NUMBERS);
	}
	
	public Atom QUOTIENT(SExpression a, SExpression b) throws InvalidOperationException{
		if(!(a instanceof Atom) || !(b instanceof Atom))
			throw new InvalidOperationException();
		int valA = Integer.parseInt(((Atom)a).getValue());
		int valB = Integer.parseInt(((Atom)b).getValue());
		return new Atom(Integer.toString(valA/valB), AtomType.NUMBERS);
	}
	
	public Atom REMAINDER(SExpression a, SExpression b) throws InvalidOperationException{
		if(!(a instanceof Atom) || !(b instanceof Atom))
			throw new InvalidOperationException();
		int valA = Integer.parseInt(((Atom)a).getValue());
		int valB = Integer.parseInt(((Atom)b).getValue());
		return new Atom(Integer.toString(valA%valB), AtomType.NUMBERS);
	}
}
