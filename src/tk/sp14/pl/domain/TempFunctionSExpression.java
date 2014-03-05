package tk.sp14.pl.domain;

import java.util.ArrayList;

public class TempFunctionSExpression implements SExpression {
	private String name;
	private int noOfArgs;
	private ArrayList<SExpression> args;
	
	public TempFunctionSExpression(String name, int noOfArgs) {
		super();
		this.setName(name);
		this.setNoOfArgs(noOfArgs);
		this.args = new ArrayList<SExpression>();
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getNoOfArgs() {
		return noOfArgs;
	}
	public void setNoOfArgs(int noOfArgs) {
		this.noOfArgs = noOfArgs;
	}
	public ArrayList<SExpression> getArgs() {
		return args;
	}
	public void setArgs(ArrayList<SExpression> args) {
		this.args = args;
	}

	@Override
	public void print() {
		System.out.print("(" + name);
		for(SExpression arg: args){
			System.out.print(" ");
			arg.print();
		}
		System.out.print(")");
	}
}
