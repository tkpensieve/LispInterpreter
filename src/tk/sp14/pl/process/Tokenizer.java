package tk.sp14.pl.process;

import java.util.ArrayList;
import java.util.Arrays;

import tk.sp14.pl.error.InvalidInputException;

public class Tokenizer {
	private static String validCharactersSet= "^[A-Z+()\\-0-9\\s\\.]+$";
	private static ArrayList<Character> splitChars = new ArrayList<Character>(
														Arrays.asList('(', ')', ' ', '.'));
	
	public static ArrayList<String> tokenize(String line) throws InvalidInputException{
		ArrayList<String> validTokens = new ArrayList<String>();
		if(!line.matches(validCharactersSet))
			throw new InvalidInputException("Input contains invalid characters. "
					+ "Valid characters are upper-case letters,numbers, plus, minus, left and right paranthesis and dot");
		else{
			//remove unnecessary spaces
			line = line.replace(" .", ".");
			line = line.replace(". ", ".");
			char[] chars = line.toCharArray();
			int parsedPosition = 0, currentPosition = 0;
			for (char c : chars) {
				if(splitChars.contains(c)){
					if(parsedPosition != currentPosition)
						validTokens.add(line.substring(parsedPosition, currentPosition));
					validTokens.add(Character.toString(c));
					parsedPosition = currentPosition + 1;
				}
				currentPosition++;
			}
			if(parsedPosition != currentPosition)
				validTokens.add(line.substring(parsedPosition, currentPosition));
		}
		
		return validTokens;
	}
}
