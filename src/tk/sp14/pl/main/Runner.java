package tk.sp14.pl.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import process.ExpressionBuilder;
import process.Tokenizer;
import tk.sp14.pl.domain.SExpression;
import tk.sp14.pl.error.InvalidInputException;

public class Runner {

	public static void main(String[] args) {
		BufferedReader systemIn = null;
		ExpressionBuilder eb = new ExpressionBuilder();
		try {
			systemIn = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		String line;
		try {
			while(systemIn!= null && (line = systemIn.readLine()) != "null") {
			    // my program loop.
				ArrayList<String> validTokens = new ArrayList<String>();
				try {
					 validTokens = Tokenizer.tokenize(line);
				} catch (InvalidInputException e) {
					//e.printStackTrace();
					System.out.println(e.getMessage());
				}
				
				//Build Expression Tree
				SExpression exp = null;
				try {
					exp = eb.build(validTokens, true);
					exp.print();
				} catch (InvalidInputException e) {
					//e.printStackTrace();
					System.out.println(e.getMessage());
				}
				
				//Call eval on the expression tree
				//Print the result
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
