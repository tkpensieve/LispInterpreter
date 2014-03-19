package tk.sp14.pl.process;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import tk.sp14.pl.domain.Atom;
import tk.sp14.pl.domain.ComplexSExpression;
import tk.sp14.pl.domain.Primitives;
import tk.sp14.pl.domain.SExpression;
import tk.sp14.pl.error.InvalidOperationException;

public class ExpressionEvaluator {
	private static ArrayList<String> primitiveFields = new ArrayList<String>();
	private static ArrayList<Method> primitiveMethods = new ArrayList<Method>();
	private static HashMap<String, Integer> primitiveMethodsParameterCount = new HashMap<String, Integer>();
//	private static ArrayList<SExpression> dList = new ArrayList<>();
	private static ArrayList<String> specialFunctionsNames = new ArrayList<>();
	private Primitives primitiveUtilities;
	
	public ExpressionEvaluator() {
		super();
		primitiveUtilities = new Primitives();
		specialFunctionsNames.add("COND");
		for (Field f : Primitives.class.getDeclaredFields()) {
			primitiveFields.add(f.getName());
		}
		for (Method m : Primitives.class.getDeclaredMethods()) {
			primitiveMethods.add(m);
			primitiveMethodsParameterCount.put(m.getName(), m.getParameterTypes().length);
		}
	}

	public SExpression evaluate(SExpression exp, boolean isInternal) throws InvalidOperationException{
		if(primitiveUtilities.ATOM(exp).equals(Primitives.T))
			return exp;
		SExpression left = primitiveUtilities.CAR(exp);
		SExpression right = primitiveUtilities.CDR(exp);
		String leftValue = ((Atom)left).getValue();
		if(!isInternal){
			//handle UDF
			if(!primitiveMethodsParameterCount.keySet().contains(leftValue) && !specialFunctionsNames.contains(leftValue)){
				left.print();
				System.out.println(" not a function.");
				throw new InvalidOperationException("Must be a function call");
			}
		}
		if(leftValue.equals("QUOTE"))
			return right;
		if(leftValue.equals("COND"))
			return evaluateConditional(exp);
		SExpression result = null;
		ArrayList<SExpression> params = getParams(right);
		//handle UDF
		int expectedParameterCount = primitiveMethodsParameterCount.get(leftValue);
		//verify params
		if(params.size() != expectedParameterCount)
			throw new InvalidOperationException("Wrong number of arguments passed");
		if(leftValue.equals("CAR"))
			result = primitiveUtilities.CAR(params.get(0));
		else if(leftValue.equals("CDR"))
			result = primitiveUtilities.CDR(params.get(0));
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
		return result;
	}

	private SExpression evaluateConditional(SExpression exp) throws InvalidOperationException{
		SExpression allParams = primitiveUtilities.CDR(exp);
		SExpression conditionExpressionPair = null;
		while(!allParams.equals(Primitives.NIL)){
			try {
				conditionExpressionPair = validationAndExtractBooleanParam(allParams);
			} catch (InvalidOperationException e) {
				throw new InvalidOperationException("Error in Params of COND" + e.getMessage());
			}
			if(evaluate(primitiveUtilities.CAR(conditionExpressionPair),true).equals(Primitives.T)){
				return evaluate(primitiveUtilities.CDR(conditionExpressionPair), true);
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

	private ArrayList<SExpression> getParams(SExpression expression) throws InvalidOperationException {
		ArrayList<SExpression> params = new ArrayList<>();
		SExpression cdr = primitiveUtilities.CDR(expression);
		SExpression car = primitiveUtilities.CAR(expression);
		if(cdr.equals(Primitives.NIL)){
			params.add(evaluateOnNeedAndExtract(car));
		}else{
			params.add(evaluateOnNeedAndExtract(car));
			params.addAll(getParams(cdr));
		}
		return params;
	}

	private SExpression evaluateOnNeedAndExtract(SExpression expression) throws InvalidOperationException {
		if(primitiveUtilities.ATOM(expression).equals(Primitives.T))
			return expression;
		SExpression left = primitiveUtilities.CAR(expression);
		String leftValue = ((Atom)left).getValue();
		if(primitiveMethodsParameterCount.keySet().contains(leftValue))
			return evaluate(expression, true);
		//handle UDF
		return expression;
	}	
}