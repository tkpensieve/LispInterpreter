package tk.sp14.pl.process;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
				throw new InvalidOperationException("Error: Must be a function call");
			}
		}
		if(leftValue.equals("QUOTE"))
			return primitiveUtilities.CAR(right);
		if(leftValue.equals("COND"))
			return evaluateConditional(exp, aList);
		if(leftValue.equals("DEFUN")){
			if(isInternal)
				throw new InvalidOperationException("Error: Cannot have Defun inside function body");
			return addUserDefinedFunction(exp, aList);
		}
		SExpression result = null;
		ArrayList<SExpression> params = getParams(right, aList, false);
		if(primitiveMethodsParameterCount.keySet().contains(leftValue)){
			int expectedParameterCount = primitiveMethodsParameterCount.get(leftValue);
			//verify params count
			if(params.size() != expectedParameterCount)
				throw new InvalidOperationException("Error: Wrong number of arguments passed");
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
						throw new InvalidOperationException("Error: Wrong number of arguments passed");
					int i = 0;
					for (String p : parameterNames) {
						aList.add(new ComplexSExpression(new Atom(p, AtomType.IDENTIFIERS), params.get(i)));
						i++;
					}
					SExpression body = fn.getBody();
					SExpression toBeExecuted = primitiveUtilities.CAR(body);
					result = evaluateOnNeedAndExtract(toBeExecuted, aList, false);
					aList.subList(aList.size() - expectedParameterCount, aList.size()).clear();
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
			Set<String> set = new HashSet<String>();
			for(SExpression s: getParams(primitiveUtilities.CAR(allParams), aList, true)){
				Atom a = (Atom)s;
				parameterNames.add(a.getValue());
				set.add(a.getValue());
			}
			//duplicate parameter names
			if(set.size() < parameterNames.size()){
			    throw new InvalidOperationException("Duplicate parameter name used");
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
		}catch (Exception e) {
			throw new InvalidOperationException("Error in DEFUN syntax");
		}
	}

	private void addToDList(FunctionSExpression expression) {
		String fnName = expression.getName();
		if(specialFunctionsNames.contains(fnName)){
			FunctionSExpression fnToRemove = null;
			for (FunctionSExpression fn : dList) {
				if(fn.getName().equals(fnName)){
					fnToRemove = fn;
					break;
				}
			}
			dList.remove(fnToRemove);
			specialFunctionsNames.remove(fnName);
		}
		dList.add(expression);
		specialFunctionsNames.add(fnName);
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
			SExpression booleanResult = evaluateOnNeedAndExtract(primitiveUtilities.CAR(conditionExpressionPair), aList, false);
			if (!(booleanResult instanceof Atom) || !((Atom)booleanResult).getType().equals(AtomType.TERMINATORS))	
				throw new InvalidOperationException("Error: Condition must evaluate to T or NIL");
			if(booleanResult.equals(Primitives.T)){
				return evaluateOnNeedAndExtract(primitiveUtilities.CDR(conditionExpressionPair),aList, false);
			}
			allParams = primitiveUtilities.CDR(allParams);
		}
		throw new InvalidOperationException("Error: No conditions matched");
	}

	private SExpression validationAndExtractBooleanParam(SExpression params) throws InvalidOperationException {
		SExpression booleanExpression = primitiveUtilities.CAR(params);
		SExpression left = primitiveUtilities.CAR(booleanExpression);
		SExpression right = primitiveUtilities.CAR(primitiveUtilities.CDR(booleanExpression));
		return new ComplexSExpression(left, right);
	}

	private ArrayList<SExpression> getParams(SExpression expression, ArrayList<SExpression> aList, boolean isFunctionDeclaration) throws InvalidOperationException {
		ArrayList<SExpression> params = new ArrayList<>();
		if(primitiveUtilities.ATOM(expression).equals(Primitives.T) && expression.equals(Primitives.NIL))
			return params;
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
					throw new InvalidOperationException("Error: Undeclared symbol is used");
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