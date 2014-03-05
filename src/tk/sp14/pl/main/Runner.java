package tk.sp14.pl.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import tk.sp14.pl.process.ExpressionBuilder;
import tk.sp14.pl.process.Tokenizer;
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
				SExpression exp = null;
				try {
					 validTokens = Tokenizer.tokenize(line);
					 //Build Expression Tree
					 try {
						 exp = eb.build(validTokens, true);
						 //Call eval on the expression tree
						 //Print the result
						 exp.print();
					 } catch (InvalidInputException e) {
						 //e.printStackTrace();
						 System.out.println(e.getMessage());
					 }
				} catch (InvalidInputException e) {
					//e.printStackTrace();
					System.out.println(e.getMessage());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}