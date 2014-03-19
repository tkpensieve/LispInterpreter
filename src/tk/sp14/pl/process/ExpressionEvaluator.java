package tk.sp14.pl.process;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import tk.sp14.pl.domain.Atom;
import tk.sp14.pl.domain.AtomType;
import tk.sp14.pl.domain.ComplexSExpression;
import tk.sp14.pl.domain.Primitives;
import tk.sp14.pl.domain.SExpression;
import tk.sp14.pl.domain.FunctionSExpression;
import tk.sp14.pl.error.InvalidOperationException;

public class ExpressionEvaluator {
	private static ArrayList<String> primitiveFields = new ArrayList<String>();
	private static ArrayList<Method> primitiveMethods = new ArrayList<Method>();
	private static HashMap<String, Integer> primitiveMethodsParameterCount = new HashMap<String, Integer>();
	private static ArrayList<FunctionSExpression> dList = new ArrayList<>();
	private static ArrayList<String> specialFunctionsNames = new ArrayList<>();
	private Primitives primitiveUtilities;
	
	public ExpressionEvaluator() {
		super();
		primitiveUtilities = new Primitives();
		specialFunctionsNames.add("COND");
		specialFunctionsNames.add("DEFUN");
		for (Field f : Primitives.class.getDeclaredFields()) {
			primitiveFields.add(f.getName());
		}
		for (Method m : Primitives.class.getDeclaredMethods()) {
			primitiveMethods.add(m);
			primitiveMethodsParameterCount.put(m.getName(), m.getParameterTypes().length);
		}
	}

	public SExpression evaluate(SExpression exp, boolean isInternal, ArrayList<SExpression> aList) throws InvalidOperationException{
		if(primitiveUtilities.ATOM(exp).equals(Primitives.T))
			return exp;
		SExpression left = primitiveUtilities.CAR(exp);
		SExpression right = primitiveUtilities.CDR(exp);
		String leftValue = ((Atom)left).getValue();
		if(!isInternal){
			if(!primitiveMethodsParameterCount.keySet().contains(leftValue) && !specialFunctionsNames.contains(leftValue)){
				left.print();
				System.out.println(" not a function.");
				throw new InvalidOperationException("Must be a function call");
			}
		}
		if(leftValue.equals("QUOTE"))
			return right;
		if(leftValue.equals("COND"))
			return evaluateConditional(exp, aList);
		if(leftValue.equals("DEFUN")){
			return addUserDefinedFunction(exp, aList);
		}
		SExpression result = null;
		ArrayList<SExpression> params = getParams(right, aList, false);
		if(primitiveMethodsParameterCount.keySet().contains(leftValue)){
			int expectedParameterCount = primitiveMethodsParameterCount.get(leftValue);
			//verify params count
			if(params.size() != expectedParameterCount)
				throw new InvalidOperationException("Wrong number of arguments passed");
			//call appropriate function
			if(leftValue.equals("CAR"))
				result = primitiveUtilities.CAR(params.get(0));
			else if(leftValue.equals("CDR"))
				result = primitiveUtilities.CDR(params.get(0));
			else if(leftValue.equals("ATOM"))
				result = primitiveUtilities.ATOM(params.get(0));
			else if(leftValue.equals("INT"))
				result = primitiveUtilities.INT(params.get(0));
			else if(leftValue.equals("NULL"))
				result = primitiveUtilities.NULL(params.get(0));
			else if(leftValue.equals("EQ"))
				result = primitiveUtilities.EQ(params.get(0), params.get(1));
			else if(leftValue.equals("CONS"))
				result = primitiveUtilities.CONS(params.get(0), params.get(1));
			else if(leftValue.equals("PLUS"))
				result = primitiveUtilities.PLUS(params.get(0), params.get(1));
			else if(leftValue.equals("MINUS"))
				result = primitiveUtilities.MINUS(params.get(0), params.get(1));
			else if(leftValue.equals("LESS"))
				result = primitiveUtilities.LESS(params.get(0), params.get(1));
			else if(leftValue.equals("GREATER"))
				result = primitiveUtilities.GREATER(params.get(0), params.get(1));
			else if(leftValue.equals("TIMES"))
				result = primitiveUtilities.TIMES(params.get(0), params.get(1));
			else if(leftValue.equals("QUOTIENT"))
				result = primitiveUtilities.QUOTIENT(params.get(0), params.get(1));
			else if(leftValue.equals("REMAINDER"))
				result = primitiveUtilities.REMAINDER(params.get(0), params.get(1));
		}
		//handle UDF
		else if(specialFunctionsNames.contains(leftValue)){
			for(FunctionSExpression fn : dList){
				if(fn.getName().equals(leftValue)){
					ArrayList<String> parameterNames = fn.getParameterNames();
					int expectedParameterCount = parameterNames.size();
					//verify params count
					if(params.size() != expectedParameterCount)
						throw new InvalidOperationException("Wrong number of arguments passed");
					int i = 0;
					for (String p : parameterNames) {
						aList.add(new ComplexSExpression(new Atom(p, AtomType.IDENTIFIERS), params.get(i)));
						i++;
					}
					SExpression body = fn.getBody();
					SExpression toBeExecuted = primitiveUtilities.CAR(body);
					result = evaluate(toBeExecuted, true, aList);
					break;
				}
			}
		}
		return result;
	}

	private SExpression addUserDefinedFunction(SExpression exp, ArrayList<SExpression> aList) throws InvalidOperationException {
		try{
			SExpression allParams = primitiveUtilities.CDR(exp);
			String functionName = ((Atom)primitiveUtilities.CAR(allParams)).getValue();
			allParams = primitiveUtilities.CDR(allParams);
			ArrayList<String> parameterNames = new ArrayList<>();
			for(SExpression s: getParams(primitiveUtilities.CAR(allParams), aList, true)){
				Atom a = (Atom)s;
				parameterNames.add(a.getValue());
			}
			SExpression body = primitiveUtilities.CDR(allParams);
			//handle syntax error
			if(!primitiveUtilities.CDR(body).equals(Primitives.NIL))
				throw new InvalidOperationException("More than one s-expression for body");
			
			FunctionSExpression expression = new FunctionSExpression(functionName, parameterNames, body);
			addToDList(expression);
			expression.print();
			
			return new Atom(functionName, AtomType.IDENTIFIERS);
		} catch (InvalidOperationException e) {
			throw new InvalidOperationException("Error in DEFUN syntax: " + e.getMessage());
		}
	}

	private void addToDList(FunctionSExpression expression) {
		dList.add(expression);
		specialFunctionsNames.add(expression.getName());
	}

	private SExpression evaluateConditional(SExpression exp, ArrayList<SExpression> aList) throws InvalidOperationException{
		SExpression allParams = primitiveUtilities.CDR(exp);
		SExpression conditionExpressionPair = null;
		while(!allParams.equals(Primitives.NIL)){
			try {
				conditionExpressionPair = validationAndExtractBooleanParam(allParams);
			} catch (InvalidOperationException e) {
				throw new InvalidOperationException("Error in Params of COND: " + e.getMessage());
			}
			//FIXME:must consider as boolean 
			if(evaluate(primitiveUtilities.CAR(conditionExpressionPair),true, aList).equals(Primitives.T)){
				return evaluate(primitiveUtilities.CDR(conditionExpressionPair), true, aList);
			}
			allParams = primitiveUtilities.CDR(allParams);
		}
		throw new InvalidOperationException("No conditions matched");
	}

	private SExpression validationAndExtractBooleanParam(SExpression params) throws InvalidOperationException {
		SExpression booleanExpression = primitiveUtilities.CAR(params);
		SExpression left = primitiveUtilities.CAR(booleanExpression);
		SExpression right = primitiveUtilities.CAR(primitiveUtilities.CDR(booleanExpression));
		return new ComplexSExpression(left, right);
	}

	private ArrayList<SExpression> getParams(SExpression expression, ArrayList<SExpression> aList, boolean isFunctionDeclaration) throws InvalidOperationException {
		ArrayList<SExpression> params = new ArrayList<>();
		SExpression cdr = primitiveUtilities.CDR(expression);
		SExpression car = primitiveUtilities.CAR(expression);
		if(cdr.equals(Primitives.NIL)){
			params.add(evaluateOnNeedAndExtract(car, aList, isFunctionDeclaration));
		}else{
			params.add(evaluateOnNeedAndExtract(car, aList, isFunctionDeclaration));
			params.addAll(getParams(cdr, aList, isFunctionDeclaration));
		}
		return params;
	}

	private SExpression evaluateOnNeedAndExtract(SExpression expression, ArrayList<SExpression> aList, boolean isFunctionDeclaration) throws InvalidOperationException {
		if(primitiveUtilities.ATOM(expression).equals(Primitives.T)){
			Atom a = (Atom)expression;
			if(a.getType().equals(AtomType.IDENTIFIERS)){
				SExpression match = getValueFromAList(a, aList, isFunctionDeclaration);
				if(!isFunctionDeclaration && match.equals(Primitives.NIL))
					throw new InvalidOperationException("Undeclared symbol is used");
				return primitiveUtilities.CDR(match);
			}
			return a;
		}
		SExpression left = primitiveUtilities.CAR(expression);
		String leftValue = ((Atom)left).getValue();
		if(primitiveMethodsParameterCount.keySet().contains(leftValue) || specialFunctionsNames.contains(leftValue))
			return evaluate(expression, true, aList);
		return expression;
	}

	private SExpression getValueFromAList(Atom atom, ArrayList<SExpression> aList, boolean isFunctionDeclaration) throws InvalidOperationException {
		if(isFunctionDeclaration)
			return new ComplexSExpression(atom, atom);
		Collections.reverse(aList);
		SExpression match = null;
		for (SExpression sExpression : aList) {
			if(((Atom)primitiveUtilities.CAR(sExpression)).getValue().equals(atom.getValue())){
				match = sExpression;
				break;
			}	
		}
		Collections.reverse(aList);
		if(match == null)
			return Primitives.NIL;
		return match;
	}	
}