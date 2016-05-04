package edu.uw.feedlack;

import java.util.*;

import com.google.javascript.rhino.Node;

public class CFGCallNode extends CFGNode {

	private CFGNode invocation;
	private List<CFGNode> arguments = null;
	private boolean checkedCanAffectOutput = false;
	private boolean canAffectOutput = false;
	
	public CFGCallNode(Node node, CFGFunction function) {
		super(node, function);
	}

	public CFGNode getInvocationNode() { return invocation; }
	
	@Override
	public TypeInfo determineType() {
	
		// What do we know about the return value of certain functions?
		TypeInfo type = invocation.getTypeInfo();
		
		// Is this a jQuery call?
		if(getCodeString().startsWith("$("))
			return new TypeInfo(this, JavaScriptDictionary.JQUERY);

		return type;
	
	}

	public int getArgumentCount() { return arguments.size(); }
	
	@Override
	protected void constructEdges() {

		arguments = new Vector<CFGNode>(2);
		
		// First, add successive edges to all expressions of this call, skipping over the name.
		CFGNode previous = null;
		
		for(int i = 0; i < node.getChildCount(); i++) {

			// Make a new node and add it to the argument list.
			Node child = node.getChildAtIndex(i);
			CFGNode childCFG = getCFGNodeFor(child);
			
			// If this is the first one, add an edge from this call node to the first expression.
			if(previous == null) {
				this.invocation = childCFG;
				addEdge(childCFG, "object", Assumption.NONE);
			}
			else {
				previous.addEdgeToLeaves(childCFG, "next argument", Assumption.NONE);
				arguments.add(childCFG);
			}
			
			previous = childCFG;
			
		}

	}
		
	public OutputExplanation getOutputExplanation() {
		
		// What is the type of this call's invocation? Should be a function...
		TypeInfo type = invocation.getTypeInfo();

		String functionName = getFunctionCalled();
		
		// Where did this type come from?
		if(type.getContainer() != null) {
			
			// If the parent type is DOCUMENT and the property is a document output, then yes, it produces output.
			JavaScriptType container = type.getContainer();
			String property = type.getProperty();

			// TODO Hack, since I haven't supported checking types of arguments
			if(container == JavaScriptDictionary.JQUERY && functionName.equals("css") && arguments.size() == 1 && arguments.get(0) instanceof CFGObjectLiteralNode) {
				return new OutputExplanation(Likelihood.YES, functionName + "()", "affects css properties");
			}
			
			// Does this container have an output-producing function of the given name? (Such as alert()?) If so, this produces output.
			if(container.hasOutputFunctionNamed(type.getProperty(), arguments.size())) {
				return new OutputExplanation(container.getLikelihoodOfOutput(property), functionName + "()", container.getReasonForLikelihood(property));
			}				
			
		}

		return CFGNode.NO_OUTPUT_EFFECT;

	}
	
	@Override
	public OutputExplanation alwaysAffectsOutput(Feedlack feedlack, Stack<CFGFunction> callstack) {
		
		String functionName = getFunctionCalled();
		
		// Find out whether ths node produces output and why.
		OutputExplanation explanation = getOutputExplanation();

		// If this call might produce native output, return the given explanation.
		if(explanation.getLikelihood() != Likelihood.NO) {
			return explanation;
		}
		
		// Do we know the type of this invocation? If it has a function, but it doesn't produce output (as determined above), then return no.
		TypeInfo info = invocation.getTypeInfo();

		// If it's not native output, but it is native, then this can't produce output.
		if(info.getContainer() != null && info.getContainer().hasFunctionNamed(functionName) && !info.getContainer().isUserDefined())
			return new OutputExplanation(Likelihood.NO, functionName + "()", "doesn't produce output");

		if(info.getContainer() != null && info.getContainer().isUserDefined()) {
			
			CFGFunction userDefinedFunction = info.getContainer().getUserDefinedFunctionNamed(functionName);
			if(userDefinedFunction != null) {
				
				if(userDefinedFunction.alwaysAffectsOutput(feedlack, callstack))
					return new OutputExplanation(Likelihood.YES, functionName + "()", "this function call always produces output.");
				else
					return new OutputExplanation(Likelihood.NO, functionName + "()", "this function call does not always produce output.");
									
			}
		}

		// Otherwise, find the functions this call might execute and see if *they* produce output.
		Set<CFGFunction> functions = feedlack.resolveFunctionReference(this);

		// If this is a call to setInterval() or setTimeout(), also resolve the function passed in.
		if(functionName.equals("setInterval") || functionName.equals("setTimeout")) {
			functions = resolveTimeoutIntervalFunctions(feedlack);
		}
		
		// If we resolved this to some functions, do all of them produce output?
		if(functions != null && !functions.isEmpty()) {
	
			// Go through all of the functions found. If all of them always produce output, then say yes.
			// Otherwise, say no, and explain that some of the functions this might call don't always produce output.
			Set<CFGFunction> functionsThatMightNotProduceOutput = new HashSet<CFGFunction>();		
			Set<CFGFunction> functionsThatAlwaysProduceOutput = new HashSet<CFGFunction>();		
			for(CFGFunction function : functions) {
				
				// If this isn't a recursive call, see if the call always produces output
				if(!callstack.contains(function)) {
					
					// Does this function produce output? Then yes, this call will produce output!
					if(function.alwaysAffectsOutput(feedlack, callstack))
						functionsThatAlwaysProduceOutput.add(function);
					else
						functionsThatMightNotProduceOutput.add(function);

				}
				// If it is a recursive call, and the function can produce output, assume it will.
				else {
					
					if(function.canAffectOutput(feedlack, callstack))
						functionsThatAlwaysProduceOutput.add(function);
					else
						functionsThatMightNotProduceOutput.add(function);
					
				}
				
			}
			
			// Now that we know which of the resolved functions always produce output and which don't, determine the output explanation for this call.
			if(functionsThatMightNotProduceOutput.size() == 0)		
				return new OutputExplanation(Likelihood.YES, functionName + "()", "all functions this might call always produce output.");
			else {
				StringBuilder names = new StringBuilder();
				for(CFGFunction f : functionsThatMightNotProduceOutput) {
					names.append(f.getDescriptionOfFunction());
					names.append(", ");
				}
				String namesString = names.toString();
				namesString = namesString.substring(0, names.length() - 2);
				return new OutputExplanation(Likelihood.MAYBE, functionName + "()", "some functions this might call, including " + names + ", may not produce output.");				
			}
			
		} 
		// We found no functions!
		else {

			// If the function called is call() or apply(), just assume it produces output. There's no way for us to know!
			if(functionName.equals("call") || functionName.equals("apply")) {
				
				return new OutputExplanation(Likelihood.YES, functionName + "()", "dynamic dispatch is assumed to affect output");				
				
			}
			else if(invocation != null && invocation instanceof CFGGetPropertyNode) {
				
				
				CFGNode propertyRepresentingFunction = ((CFGGetPropertyNode)invocation).getPropertyNode();
				if(!(propertyRepresentingFunction instanceof CFGBasicNode) && !(propertyRepresentingFunction instanceof CFGNameNode)) {
				
					return new OutputExplanation(Likelihood.YES, "function name expression", "dynamic dispatch is assumed to affect output");

				}
				
			}

			// If we didn't find any functions that match this function call, let's assume that it produces no output.
			return new OutputExplanation(Likelihood.NO, functionName + "()", "no function by this name could be found; assuming it doesn't produce output");
			
		}
	
	}
	
	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		// Mark all of the state that affects its arguments as output affecting
		for(CFGNode argument : arguments) {

			argument.markOutputAffectingDataDependenciesHelper(stack);
			
		}
		
		// Find the functions this might call.
		Set<CFGFunction> matches = getFunction().getFeedlack().resolveFunctionReference(this);
		
		if(matches != null) {
//			Feedlack.out("Found " + matches + " for " + this);
			for(CFGFunction fun : matches) {
				
				Collection<CFGReturnNode> returns = fun.getReturns();
				for(CFGReturnNode ret : returns) {
//					Feedlack.out("Marking call " + this + " as output affecting, so marking return " + ret);
					ret.markOutputAffectingDataDependenciesHelper(stack);
				}
				
			}
			
		}
		
	}
	
	public boolean canAffectOutput(Feedlack feedlack, Stack<CFGFunction> callstack) { 
		
		if(checkedCanAffectOutput) {
			return canAffectOutput;
		}
		
		checkedCanAffectOutput = true;

		String functionName = getFunctionCalled();

		if(isNativeOutput()) {
			
//			Feedlack.out("Found native output: " + functionName + "() " + getDescriptionOfLocation());
			canAffectOutput = true;
			return canAffectOutput;
		}
		
		TypeInfo info = invocation.getTypeInfo();

		// If it's not native output, but it is native, then this can't produce output.
		if(info.getContainer() != null && info.getContainer().hasFunctionNamed(functionName) && !info.getContainer().isUserDefined()) {
			canAffectOutput = false;
			return canAffectOutput;
		}

		
		// Did we parse a function with this name in an object literal?
		if(info.getContainer() != null && info.getContainer().isUserDefined()) {
			CFGFunction userDefinedFunction = info.getContainer().getUserDefinedFunctionNamed(functionName);
			if(userDefinedFunction != null) {
				
				canAffectOutput = userDefinedFunction.canAffectOutput(feedlack, callstack);
				return canAffectOutput;
				
			}
		}
		
		// Find the functions with this name.					
		Set<CFGFunction> functions = feedlack.resolveFunctionReference(this);
		
		// If this is a call to setInterval() or setTimeout(), also resolve the function passed in.
		if(functionName.equals("setInterval") || functionName.equals("setTimeout")) {
			functions = resolveTimeoutIntervalFunctions(feedlack);
		}
		
		if(functions != null) {
		
//			if(functions.size() > 1)
//				Feedlack.out("" + functionName + "():" + getDescriptionOfLocation() + " resolved to " + functions);
			
			for(CFGFunction function : functions) {
				
				if(!callstack.contains(function)) {
	
					// Does this function produce output?
					if(function.canAffectOutput(feedlack, callstack)) {
						// TODO This reasoning needs to be exposed to the UI, to help understand why Feedlack thinks a call can affect output.
//						Feedlack.out("" + this.getDescriptionOfLocation() + " affects output because " + function.getDescriptionOfLocation() + " affects output");
						canAffectOutput = true;
						return canAffectOutput;
					}
					
				}
				
			}					
		
		} 
		else {
			
			// If we didn't find any functions that match this function call, let's assume that it produces no output.
			canAffectOutput = false;
			return canAffectOutput;
			
		}

		canAffectOutput = false;
		return canAffectOutput;
		
	}


	public String toString() { 
		
		return getCodeString();
		
	}
	
	public String getCodeString() {
		
		return invocation != null ? invocation.getCodeString() + "([args])" : "???()";
		
	}
	
	private String functionCalledCache = null;
	
	/**
	 * This gets called a lot to traverse control flow paths, hence the cache.
	 */
	public String getFunctionCalled() { 

		if(functionCalledCache == null && invocation != null) {
		
			String functionName = invocation.getCodeString();
			int indexOfLastDot = functionName.lastIndexOf(".");
			if(indexOfLastDot >= 0) functionName = functionName.substring(indexOfLastDot + 1);
			functionCalledCache = functionName;

		}
		
		return functionCalledCache;
		
	}

	
	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {

		String functionName = getFunctionCalled();
		
		// The jQuery event binding model.
		if(JavaScriptDictionary.isJQueryEventName(functionName) && arguments.size() > 0) {
			
			if(getCodeString().startsWith("$(")) {
			
				CFGNode handlerExpression;
				String event;

				if(functionName.equals("one") || functionName.equals("bind")) {
					
					event = arguments.get(0).getCodeString();
					handlerExpression  = arguments.get(1);
					if(JavaScriptDictionary.isInputEventNameRequiringOutput(event))
						return new JavaScriptInputHandler(event, this, feedlack.resolveFunctionReference(handlerExpression));
					
				}
				else {
					
					event = functionName;
					handlerExpression  = arguments.get(0);
					if(JavaScriptDictionary.isInputEventNameRequiringOutput(event))
						return new JavaScriptInputHandler(event, this, feedlack.resolveFunctionReference(handlerExpression));
					
				}
				
			}
			
		}
		
		// The W3C event binding model.
		// TODO Need to support Internet Explorer too.
		else if((functionName.equals("addEvent") || functionName.equals("addEventListener")) && arguments.size() >= 2) {

			CFGNode eventExpression = arguments.get(0);
			
			if(eventExpression instanceof CFGBasicNode && JavaScriptDictionary.isInputEventNameRequiringOutput(eventExpression.getCodeString())){

				String event = arguments.get(0).getCodeString();
				
				// Need to get the value.
				CFGNode value = arguments.get(1);

				Set<CFGFunction> functions = feedlack.resolveFunctionReference(value);
				
				// If the value is a function node, find the corresponding function
				// If the value is a name, find the function with the corresponding name (unless its a local name?)
				// If the value is an expression, who knows what will happen! It could be any function. What do we warn on this?
				return new JavaScriptInputHandler(event, this, functions);

			}

			
		}

		return null;
	}
	
	
	public Set<CFGFunction> resolveTimeoutIntervalFunctions(Feedlack feedlack) {
		
		// There are many ways to refer to a function, but there are only a few that we can resolve here: name references to functions, getProperty nodes that reference functions
		// and anonymous functions.
		if(arguments.size() >= 1)
			return feedlack.resolveFunctionReference(arguments.get(0));
		else
			return new HashSet<CFGFunction>();
		
	}

	public CFGNode getArgument(int argumentNumber) {
		
		if(argumentNumber < 0 || argumentNumber >= arguments.size())
			return null;
		else return arguments.get(argumentNumber);
		
	}
	
}