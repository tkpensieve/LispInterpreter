package process;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import tk.sp14.pl.domain.Atom;
import tk.sp14.pl.domain.AtomType;
import tk.sp14.pl.domain.Primitives;
import tk.sp14.pl.domain.SExpression;
import tk.sp14.pl.error.InvalidInputException;

public class ExpressionBuilder {
	private static ArrayList<String> primitiveFields = new ArrayList<String>();
	private static ArrayList<String> primitiveMethods = new ArrayList<String>();
	private static HashMap<String, Integer> primitiveMethodsParameterCount = new HashMap<String, Integer>();
	private ArrayList<SExpression> allExpressions;
	private static int parsedTokensCount = 0;
	
	public ExpressionBuilder() {
		super();
		for (Field f : Primitives.class.getDeclaredFields()) {
			primitiveFields.add(f.getName());
		}
		for (Method m : Primitives.class.getDeclaredMethods()) {
			primitiveMethods.add(m.getName());
			primitiveMethodsParameterCount.put(m.getName(), m.getParameterTypes().length);
		}
	}


	public ArrayList<SExpression> build(ArrayList<String> validTokens) throws InvalidInputException{
		System.out.println(primitiveFields);
		System.out.println(primitiveMethods);
		allExpressions = new ArrayList<SExpression>();
		for (parsedTokensCount = 0; parsedTokensCount < validTokens.size(); parsedTokensCount++) {
			allExpressions.addAll(parse(validTokens, 1));
		} 
		return allExpressions;
	}
	
	private ArrayList<SExpression> parse(ArrayList<String> validTokens, int num) throws InvalidInputException{
		ArrayList<SExpression> sexp = new ArrayList<SExpression>();
		while(num > 0){
			String t = validTokens.get(parsedTokensCount);
			if(primitiveFields.contains(t)){
				if("T".equals(t)){
					sexp.add(Primitives.T);
					num--;
				}
				if("NIL".equals(t)){
					sexp.add(Primitives.NIL);
					num--;
				}
			}
			else if(primitiveMethods.contains(t)){
				if("CAR".equals(t)){
					parsedTokensCount++;
					if(validTokens.get(parsedTokensCount) != "(")
						throw new InvalidInputException("Missing ( paranthesis");
					parsedTokensCount++;
					sexp.addAll(parse(validTokens, primitiveMethodsParameterCount.get(t.toString())));
				}
			}
			else if(t.matches("^[0-9]+$")){
				int value = Integer.parseInt(t);
				sexp.add(new Atom(value, AtomType.NUMBERS));
				parsedTokensCount++;
			}
			else if(t != ")"){
				sexp.add(new Atom(t, AtomType.IDENTIFIERS));
				parsedTokensCount++;
			}
			else{
			}
		}
		return sexp;
	}
}
