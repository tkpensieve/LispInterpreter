package tk.sp14.pl.process;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import tk.sp14.pl.domain.Atom;
import tk.sp14.pl.domain.AtomType;
import tk.sp14.pl.domain.ComplexSExpression;
import tk.sp14.pl.domain.Primitives;
import tk.sp14.pl.domain.SExpression;
import tk.sp14.pl.domain.TempFunctionSExpression;
import tk.sp14.pl.error.InvalidInputException;

public class ExpressionBuilder {
	private static ArrayList<String> primitiveFields = new ArrayList<String>();
	private static ArrayList<String> primitiveMethods = new ArrayList<String>();
	private static HashMap<String, Integer> primitiveMethodsParameterCount = new HashMap<String, Integer>();
	private static ArrayList<String> unprocessedTokens = new ArrayList<>();
	
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

	public SExpression build(ArrayList<String> validTokens, boolean isBeginning) throws InvalidInputException{
		if(isBeginning){
			validTokens.addAll(unprocessedTokens);
			unprocessedTokens.clear();
		}
		String t = validTokens.get(0);
		SExpression exp;
		if(primitiveMethods.contains(t)){
			int argsCount = primitiveMethodsParameterCount.get(t);
			TempFunctionSExpression functionSExpression = new TempFunctionSExpression(t, argsCount);
			functionSExpression.setArgs(parseWithinParams(
											validTokens.subList(1, validTokens.size()), 
											argsCount));
			exp = functionSExpression;
		}
		else if(primitiveFields.contains(t)){
			if(validTokens.size() > 1)
				throw new InvalidInputException("Error - T and NIL can only occur alone");
			exp = createAtom(t);
		}
		else if(t.equals("("))
			exp =  parseWithinParams(validTokens, 1).get(0);
		else if(validTokens.size() == 1)
			exp = createAtom(t);
		else
			throw new InvalidInputException("Error -  Must be a single atom or start with method or left paranthesis");
		if(unprocessedTokens.size() > 0){
			return new ComplexSExpression(exp, build(unprocessedTokens, false));
		}
		return exp;
	}
	
	private Atom createAtom(String t) throws InvalidInputException {
		if("T".equals(t))
			return Primitives.T;
		else if("NIL".equals(t))
			return Primitives.NIL;
		else if(t.matches("^[+\\-0-9]+$")){
			int value = Integer.parseInt(t);
			return new Atom(Integer.toString(value), AtomType.NUMBERS);
		}
		else if(t.matches("^[A-Z][A-Z0-9]+$") && !primitiveMethods.contains(t)){
			if(t.length() > 10)
				throw new InvalidInputException("Error - Identifier cannot exceed 10 characters");
			return new Atom(t, AtomType.IDENTIFIERS);
		}
		throw new InvalidInputException("Error - Not a valid atom");
	}

	private ArrayList<SExpression> parseWithinParams(List<String> tokens, Integer expectedCount) 
			throws InvalidInputException {
		ArrayList<SExpression> args = new ArrayList<SExpression>();
		//extract within brackets - Brackets verification
		ArrayList<String> extractedTokens = verifyBracketsAndExtract(tokens);
		//split based on noOfArgs
		if(expectedCount > 1){
			int splitPoint = extractedTokens.indexOf(" ");
			if(splitPoint == -1)
				throw new InvalidInputException("Invalid number of arguments to function call"
						+ " or wrong delimiter. Use space to separate the arguments");
			//parse each to SExp
			args.add(
					buildSingleExpressionFrom(extractedTokens.subList(0, splitPoint), false)
					);
			args.add(
					buildSingleExpressionFrom(extractedTokens.subList(splitPoint+1, extractedTokens.size()), false)
					);
		}
		else{
			//parse to SExp
			args.add(buildSingleExpressionFrom(extractedTokens, false));
		}
		return args;
	}

	private ArrayList<String> verifyBracketsAndExtract(List<String> tokens) throws InvalidInputException {
		String firstToken = tokens.get(0);
		if(!firstToken.equals("("))
			throw new InvalidInputException("Missing ( paranthesis");
		tokens.remove(0);
		ArrayList<String> contentsToBuildSExpression = new ArrayList<>();
		int isEnd = 0, tokensLength = tokens.size(), i;
		for (i = 0; i < tokensLength; i++) {
			String s = tokens.get(i);
			if(s.equals("("))
				isEnd--;
			else if(s.equals(")"))
				isEnd++;
			if(isEnd == 1){
				break;
			}
			contentsToBuildSExpression.add(s);
		}
		if(isEnd != 1)
			throw new InvalidInputException("No matching closing parantheis");
		if(i < tokensLength-1){
			if(tokens.get(i+1).equals("."))
				unprocessedTokens.addAll(tokens.subList(i+2, tokensLength));
			else
				throw new InvalidInputException("Error - Illegal characters after close paranthesis");
		}
		return contentsToBuildSExpression;
	}

	private SExpression buildSingleExpressionFrom(List<String> tokens, boolean isList) throws InvalidInputException {
		int tokensSize = tokens.size();
		if(tokensSize == 0)
			return Primitives.NIL;
		String currentToken = tokens.get(0);
		if(primitiveMethods.contains(currentToken)){
			int argsCount = primitiveMethodsParameterCount.get(currentToken);
			TempFunctionSExpression functionSExpression = new TempFunctionSExpression(currentToken, argsCount);
			functionSExpression.setArgs(parseWithinParams(tokens.subList(1, tokensSize), argsCount));
			return functionSExpression;
		}
		else if(primitiveFields.contains(currentToken) || currentToken.matches("^[+\\-0-9A-Z]+$")){
			if(tokensSize == 1){
				Atom atom = createAtom(currentToken);
				return new ComplexSExpression(atom, Primitives.NIL);
			}
			String nextToken = tokens.get(1);
			//atom part of a dotted S-Expression
			if(nextToken.equals(".")){
				String nextNextToken = tokens.get(2);
				if(primitiveMethods.contains(nextNextToken)){
					int argsCount = primitiveMethodsParameterCount.get(nextNextToken);
					TempFunctionSExpression functionSExpression = new TempFunctionSExpression(nextNextToken, argsCount);
					functionSExpression.setArgs(parseWithinParams(
													tokens.subList(3, tokensSize), 
													argsCount));
					return new ComplexSExpression(createAtom(currentToken), functionSExpression);
				}
				if(nextNextToken.equals("(")){
					return new ComplexSExpression(createAtom(currentToken), 
												parseWithinParams(tokens.subList(2, tokensSize), 1).get(0));
				}
				if(tokensSize > 3)
					throw new InvalidInputException("Error - More than two dotted atoms");
				return new ComplexSExpression(createAtom(currentToken), createAtom(nextNextToken));
			}
			//atom part of a list
			else if(nextToken.equals(" ")){ 
				//handle list
				Atom left = createAtom(currentToken);
				SExpression right = buildSingleExpressionFrom(tokens.subList(2, tokensSize), true);
				return new ComplexSExpression(left, right); 
			}
			else if(nextToken.equals("("))
				throw new InvalidInputException("Error - Invalid paranthesis occurrence after an atom");
			//atom standing on its own
			return createAtom(currentToken);
		}
		else if(currentToken.equals("("))
			return new ComplexSExpression(parseWithinParams(tokens, 1).get(0), Primitives.NIL); 
		else 
			throw new InvalidInputException("Error - Wrong placement of symbols");
	}
}
