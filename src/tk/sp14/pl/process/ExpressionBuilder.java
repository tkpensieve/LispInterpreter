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
			int unprocessedTokensCount = unprocessedTokens.size();
			if(unprocessedTokensCount >0 && 
					!unprocessedTokens.get(unprocessedTokensCount-1).equals(" ") &&
					!validTokens.get(0).equals(" "))
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
			else if(t.equals("(")){
				ArrayList<String> tokensWithinBrackets = verifyBracketsAndExtract(validTokens);
				unprocessedTokens.addAll(validTokens.subList(tokensWithinBrackets.size()+2, validTokens.size()));
				exp =  buildSingleExpressionFrom(tokensWithinBrackets, false);
			}
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
			SExpression right = null;
			try{
				 right = build(newTokens, false);
			}catch(Exception e){
				unprocessedTokens.addAll(newTokens);
				return exp;
			}
			return new ComplexSExpression(exp, right);
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
		return contentsToBuildSExpression;
	}

	private SExpression buildSingleExpressionFrom(List<String> tokens, boolean isList) throws InvalidInputException, IncompleteInputException {
		int tokensSize = tokens.size();
		if(tokensSize == 0)
			return Primitives.NIL;
		String currentToken = tokens.get(0);
		if(primitiveFields.contains(currentToken) || 
				currentToken.matches(validIdentifierFormat) || 
				currentToken.matches(validNumberFormat)){
			if(tokensSize == 1){
				Atom atom = createAtom(currentToken);
				return new ComplexSExpression(atom, Primitives.NIL);
			}
			String nextToken = tokens.get(1);
			//atom part of a dotted S-Expression (Valid only with QUOTE)
			if(nextToken.equals(".")){
				SExpression left = createAtom(currentToken);
				String nextNextToken = tokens.get(2);
				if(nextNextToken.equals("(")){
					ArrayList<String> extractedTokens = verifyBracketsAndExtract(tokens.subList(2, tokensSize));
					List<String> remainingTokens = tokens.subList(2+extractedTokens.size()+2, tokensSize);
					ComplexSExpression exp = new ComplexSExpression(buildSingleExpressionFrom(extractedTokens, true), Primitives.NIL);
					if(remainingTokens.size() == 0)
						return new ComplexSExpression(left, exp);
					String tokenAfterBrackets = remainingTokens.get(0);
					if(tokenAfterBrackets.equals("."))
						throw new InvalidInputException("Error - More than two dotted atoms");
					if(tokenAfterBrackets.equals(" "))
						throw new InvalidInputException("Error - More than two dotted atoms");
				}
				return new ComplexSExpression(left, createAtom(nextNextToken));
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
			if(isList)
				return new ComplexSExpression(createAtom(currentToken), Primitives.NIL);
			else
				return createAtom(currentToken);
		}
		else if(currentToken.equals("(")){
			// List of List scenario - Identify if inside list is in between or at end
			ArrayList<String> extractedTokens = verifyBracketsAndExtract(tokens);
			SExpression left = buildSingleExpressionFrom(extractedTokens, true);
			List<String> remainingTokens = tokens.subList(2+extractedTokens.size(), tokensSize);
			if(remainingTokens.size() == 0)
				return new ComplexSExpression(left, Primitives.NIL);
			String tokenAfterBrackets = remainingTokens.get(0);
			if(tokenAfterBrackets.equals(" "))
				return new ComplexSExpression(left, buildSingleExpressionFrom(remainingTokens, true));
			else 
				throw new InvalidInputException(currentToken + " - Error - Wrong placement of symbols");
		}
		else if(currentToken.equals(" ")){
			return buildSingleExpressionFrom(tokens.subList(1, tokensSize), true);
		}
		else 
			throw new InvalidInputException(currentToken + " - Error - Illegal atom");
	}
}
