package tk.sp14.pl.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import tk.sp14.pl.process.ExpressionBuilder;
import tk.sp14.pl.process.ExpressionEvaluator;
import tk.sp14.pl.process.Tokenizer;
import tk.sp14.pl.domain.SExpression;
import tk.sp14.pl.error.IncompleteInputException;
import tk.sp14.pl.error.InvalidInputException;
import tk.sp14.pl.error.InvalidOperationException;

public class Runner {

	public static void main(String[] args) {
		BufferedReader systemIn = null;
		ExpressionBuilder eb = new ExpressionBuilder();
		ExpressionEvaluator ev = new ExpressionEvaluator();
		try {
			systemIn = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String line;
		try {
			System.out.println("Press q to quit.");
			System.out.println("Input>>");
			while(systemIn!= null && !(line = systemIn.readLine()).equals("q")) {
			    // my program loop.
				ArrayList<String> validTokens = new ArrayList<String>();
				SExpression exp = null;
				try {
					 validTokens = Tokenizer.tokenize(line);
					 //Build Expression Tree
					 try {
						 exp = eb.build(validTokens, true);
						 System.out.print("] ");
						 exp.print();
						 System.out.println("");
						 SExpression result = null;
						 //Call eval on the expression tree
						 try {
							 result = ev.evaluate(exp, false, new ArrayList<SExpression>());
							 //Print the result
							 System.out.print("]] ");
							 result.print();
							 System.out.println("");
							 System.out.println("Press q to quit.");
							 System.out.println("Input>>");
						 } catch (InvalidOperationException e) {
							 System.out.println(e.getMessage());
							 return;
						 }
					 } catch (InvalidInputException e) {
						 System.out.println(e.getMessage());
						 return;
					 } catch (IncompleteInputException e) {
						 System.out.println(e.getMessage());
					 }
				} catch (InvalidInputException e) {
					System.out.println(e.getMessage());
					return;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
