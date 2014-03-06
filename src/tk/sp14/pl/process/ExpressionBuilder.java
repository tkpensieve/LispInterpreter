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
import tk.sp14.pl.error.IncompleteInputException;
import tk.sp14.pl.error.InvalidInputException;

public class ExpressionBuilder {
	private static ArrayList<String> primitiveFields = new ArrayList<String>();
	private static ArrayList<String> primitiveMethods = new ArrayList<String>();
	private static String validNumberFormat= "^[+\\-]?[0-9]+$";
	private static String validIdentifierFormat= "^[A-Z][A-Z0-9]*$";
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

	public SExpression build(ArrayList<String> validTokens, boolean isBeginning) throws InvalidInputException, IncompleteInputException{
		if(isBeginning){
			if(unprocessedTokens.size() >0)
				unprocessedTokens.add(" ");
			validTokens.addAll(0, unprocessedTokens);
			unprocessedTokens.clear();
		}
		String t = validTokens.get(0);
		SExpression exp;
		if(validTokens.size() == 1)
			exp = createAtom(t);
		else{
			if(primitiveFields.contains(t)){
				throw new InvalidInputException("Error - T and NIL can only occur alone");
			}
			else if(t.equals("("))
				exp =  parseWithinParams(validTokens, 1).get(0);
			else
				//shouldn't come here at all
				throw new InvalidInputException("Error - Invalid input. Should be single atom or start with paranthesis");
		}
		if(unprocessedTokens.size() > 0){
			ArrayList<String> newTokens = new ArrayList<>();
			for(String s:unprocessedTokens){
				newTokens.add(s);
			}
			System.out.println(newTokens);
			unprocessedTokens.clear();
			System.out.println(newTokens);
			return new ComplexSExpression(exp, build(newTokens, false));
		}
		return exp;
	}
	
	private Atom createAtom(String t) throws InvalidInputException, IncompleteInputException {
		if("T".equals(t))
			return Primitives.T;
		else if("NIL".equals(t))
			return Primitives.NIL;
		else if(t.matches(validNumberFormat)){
			int value = Integer.parseInt(t);
			return new Atom(Integer.toString(value), AtomType.NUMBERS);
		}
		else if(t.matches(validIdentifierFormat)){ // && !primitiveMethods.contains(t)){
			if(t.length() > 10)
				throw new InvalidInputException("Error - Identifier cannot exceed 10 characters");
			return new Atom(t, AtomType.IDENTIFIERS);
		}
		unprocessedTokens.add(t);
		throw new IncompleteInputException("Warning - Currently not a valid atom. Waiting for further input.");
	}

	private ArrayList<SExpression> parseWithinParams(List<String> tokens, Integer expectedCount) 
			throws InvalidInputException, IncompleteInputException {
		ArrayList<SExpression> args = new ArrayList<SExpression>();
		//extract within brackets - Brackets verification
		ArrayList<String> extractedTokens = verifyBracketsAndExtract(tokens);
		//split based on noOfArgs
		if(expectedCount > 1){
			int splitPoint = extractedTokens.indexOf(" ");
			if(splitPoint == -1)
				throw new InvalidInputException("Error - Invalid number of arguments to function call"
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

	private ArrayList<String> verifyBracketsAndExtract(List<String> tokens) throws InvalidInputException, IncompleteInputException {
		String firstToken = tokens.get(0);
		if(!firstToken.equals("("))
			throw new InvalidInputException("Error - Missing ( paranthesis");
		ArrayList<String> contentsToBuildSExpression = new ArrayList<>();
		int isEnd = 0, tokensLength = tokens.size(), i;
		for (i = 1; i < tokensLength; i++) {
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
		if(isEnd != 1){
			unprocessedTokens.addAll(tokens);
			throw new IncompleteInputException("Warning - No matching closing paranthesis. Waiting for further input");
		}
		if(i < tokensLength-1){
			String nextTokenAfterBracket = tokens.get(i+1);
//			if(nextTokenAfterBracket.equals(".") || nextTokenAfterBracket.equals(" "))
//				unprocessedTokens.addAll(tokens.subList(i+2, tokensLength));
//			else
			if(!nextTokenAfterBracket.equals(" "))
				throw new InvalidInputException("Error - Illegal characters after close paranthesis");
		}
		return contentsToBuildSExpression;
	}

	private SExpression buildSingleExpressionFrom(List<String> tokens, boolean isList) throws InvalidInputException, IncompleteInputException {
		int tokensSize = tokens.size();
		if(tokensSize == 0)
			return Primitives.NIL;
		String currentToken = tokens.get(0);
//		if(primitiveMethods.contains(currentToken)){
//			int argsCount = primitiveMethodsParameterCount.get(currentToken);
//			TempFunctionSExpression functionSExpression = new TempFunctionSExpression(currentToken, argsCount);
//			functionSExpression.setArgs(parseWithinParams(tokens.subList(1, tokensSize), argsCount));
//			return functionSExpression;
//		}
		if(primitiveFields.contains(currentToken) || 
				currentToken.matches(validIdentifierFormat) || 
				currentToken.matches(validNumberFormat)){
			if(tokensSize == 1){
				Atom atom = createAtom(currentToken);
				return new ComplexSExpression(atom, Primitives.NIL);
			}
			String nextToken = tokens.get(1);
			//atom part of a dotted S-Expression
			if(nextToken.equals(".")){
				String nextNextToken = tokens.get(2);
//				if(primitiveMethods.contains(nextNextToken)){
//					int argsCount = primitiveMethodsParameterCount.get(nextNextToken);
//					TempFunctionSExpression functionSExpression = new TempFunctionSExpression(nextNextToken, argsCount);
//					functionSExpression.setArgs(parseWithinParams(
//													tokens.subList(3, tokensSize), 
//													argsCount));
//					return new ComplexSExpression(createAtom(currentToken), functionSExpression);
//				}
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
		else if(currentToken.equals("(")){
			// List of List scenario - Identify if inside list is in between or at end
			if(tokens.indexOf(")") == tokensSize-1)
					return new ComplexSExpression(parseWithinParams(tokens, 1).get(0), Primitives.NIL);
			else{
				ArrayList<String> innerList = verifyBracketsAndExtract(tokens);
				SExpression left = buildSingleExpressionFrom(innerList, true);
				SExpression right = buildSingleExpressionFrom(tokens.subList(tokens.indexOf(")")+2, tokensSize), 
																true);
				return new ComplexSExpression(left, right);
			}
		}
		else if(currentToken.equals(")") || currentToken.equals(" ")){
			return buildSingleExpressionFrom(tokens.subList(1, tokensSize), isList);
		}
		else 
			throw new InvalidInputException(currentToken + "Error - Wrong placement of symbols");
	}
}
