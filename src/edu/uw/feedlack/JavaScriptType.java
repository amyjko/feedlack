package edu.uw.feedlack;

import java.util.Hashtable;
import java.util.Map;

public class JavaScriptType {
	
	private static class Property {
	
		public final boolean isFunction;
		public final String name;
		public final JavaScriptType typeAfterEvaluation;
		public final Likelihood isOutput;
		public final String reasonForLikelihood;
		public final int expectedArguments;	// How many arguments does this require to count as output?
		public CFGFunction function = null;

		public Property(boolean function, String name, JavaScriptType type, Likelihood isOutput, String reasonForLikelihood, int expectedArguments) {
			
			this.isFunction = function;
			this.name = name;
			this.typeAfterEvaluation = type;
			this.isOutput = isOutput;
			this.reasonForLikelihood = reasonForLikelihood;
			this.expectedArguments = expectedArguments;
			
		}
	
	}

	private final String name;
	public final boolean isUserDefined;
	private Map<String,JavaScriptType.Property> properties = new Hashtable<String,JavaScriptType.Property>();
	
	public JavaScriptType(String name) {
		
		this(name, false);
		
	}

	public JavaScriptType(String name, boolean isUserDefined) {
		
		this.name = name;
		this.isUserDefined = isUserDefined;

		if(!isUserDefined)
			JavaScriptDictionary.PRIMITIVES.put(name, this);
		
	}

	public String getName() { return name; }

	public void addFunction(CFGFunction fun) {
		
		JavaScriptType.Property p = new JavaScriptType.Property(true, fun.getFunctionName(), JavaScriptDictionary.NONE, Likelihood.NO, "user defined function", -1);
		p.function = fun;
		properties.put(fun.getFunctionName(), p);
		
	}
	
	public void addFunction(String name, JavaScriptType type) {
		
		JavaScriptType.Property p = new JavaScriptType.Property(true, name, type, Likelihood.NO, "does not affect output", -1);
		properties.put(name, p);
		
	}

	public void addFunction(String name, JavaScriptType type, Likelihood isOutput, String reasonForLikelihood) {
		
		JavaScriptType.Property p = new JavaScriptType.Property(true, name, type, isOutput, reasonForLikelihood, -1);
		properties.put(name, p);
		
	}
	
	public void addFunction(String name, JavaScriptType type, Likelihood isOutput, String reasonForLikelihood, int expectedArguments) {

		JavaScriptType.Property p = new JavaScriptType.Property(true, name, type, isOutput, reasonForLikelihood, expectedArguments);
		properties.put(name, p);
		
	}

	public void addProperty(String name, JavaScriptType type) {
		
		JavaScriptType.Property p = new JavaScriptType.Property(false, name, type, Likelihood.NO, "does not affect output", -1);
		properties.put(name, p);
		
	}

	public void addProperty(String name, JavaScriptType type, Likelihood isOutput, String reasonForLikelihood) {
		
		JavaScriptType.Property p = new JavaScriptType.Property(false, name, type, isOutput, reasonForLikelihood, -1);
		properties.put(name, p);
		
	}

	public boolean  hasMemberNamed(String name) {
		
		JavaScriptType.Property p = properties.get(name);
		return p != null;
		
	}

	public boolean  hasFunctionNamed(String name) {
		
		JavaScriptType.Property p = properties.get(name);
		return p != null && p.isFunction;
		
	}

	public boolean hasOutputFunctionNamed(String name, int numberOfArgumentsInCall) {
		
		JavaScriptType.Property p = properties.get(name);
				
		return p != null && p.isFunction && p.isOutput != Likelihood.NO && (p.expectedArguments < 0 || p.expectedArguments == numberOfArgumentsInCall);
		
	}

	public boolean hasOutputAffectingPropertyNamed(String property) {

		JavaScriptType.Property p = properties.get(property);
		return p != null && !p.isFunction && p.isOutput != Likelihood.NO;
	
	}

	public JavaScriptType getTypeOfProperty(String name) {

		JavaScriptType.Property p = properties.get(name);
		if(p != null) {
			return p.typeAfterEvaluation;
		}
		else return JavaScriptDictionary.NONE;

	}

	public Likelihood getLikelihoodOfOutput(String property) {
		
		JavaScriptType.Property p = properties.get(property);
		return p.isOutput;

	}

	public String getReasonForLikelihood(String property) {
		
		JavaScriptType.Property p = properties.get(property);
		return p.reasonForLikelihood;

	}

	public CFGFunction getUserDefinedFunctionNamed(String name) {
		
		Property p = properties.get(name);
		if(p != null)
			if(p.function != null) 
				return p.function;
		return null;
		
	}
	
	public String toString() { return name; }

	public boolean isUserDefined() {
		
		return isUserDefined;
		
	}
	
}