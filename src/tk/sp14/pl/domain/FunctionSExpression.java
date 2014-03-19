package tk.sp14.pl.domain;

import java.util.ArrayList;

public class FunctionSExpression implements SExpression {
	private String name;
	private ArrayList<String> parameterNames;
	private SExpression body;

	public FunctionSExpression(String name, ArrayList<String> paramNames, SExpression body) {
		super();
		this.setName(name);
		this.setParameterNames(paramNames);
		this.setBody(body);
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ArrayList<String> getParameterNames() {
		return parameterNames;
	}
	public void setParameterNames(ArrayList<String> paramNames) {
		this.parameterNames = paramNames;
	}
	public SExpression getBody() {
		return body;
	}
	public void setBody(SExpression body) {
		this.body = body;
	}
	
	@Override
	public void print() {
		System.out.println("Your function " + name + " is added");
	}
}
