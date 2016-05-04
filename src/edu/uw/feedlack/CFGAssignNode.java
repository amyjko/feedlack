package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGAssignNode extends CFGNode {

	private CFGNode value, assignee;
	
	public CFGAssignNode(Node node, CFGFunction function) {
		super(node, function);
	}

	public CFGNode getAssignee() { return assignee; }
	public CFGNode getValue() { return value; }
	
	@Override
	public TypeInfo determineType() {
				
		return new TypeInfo(this, JavaScriptDictionary.NONE);
		
	}
	
	@Override
	/*
	 * Block node points to it's first child.
	 */
	protected void constructEdges() {

		this.value = getCFGNodeFor(node.getChildAtIndex(1));
		addEdge(value, "value expression", Assumption.NONE);
		this.assignee = getCFGNodeFor(node.getChildAtIndex(0));
		value.addEdgeToLeaves(assignee, "assignee", Assumption.NONE);
				
	}
	
	public OutputExplanation getOutputExplanation() {

		TypeInfo type = assignee.getTypeInfo();
		JavaScriptType container = type.getContainer();
		String property = type.getProperty();
				
		// If the assignment is happening on a property of an object and the property is output affecting...
		if(container != null && property != null && container.hasOutputAffectingPropertyNamed(property))
			return new OutputExplanation(container.getLikelihoodOfOutput(property), property, container.getReasonForLikelihood(property));			

		return CFGNode.NO_OUTPUT_EFFECT;
		
	}

	@Override
	public OutputExplanation alwaysAffectsOutput(Feedlack feedlack, Stack<CFGFunction> callstack) {

		return getOutputExplanation();
		
	}
	
	public boolean canAffectOutput(Feedlack feedlack, Stack<CFGFunction> callstack) { 
		
		return isNativeOutput(); 
		
	}
		
	public String toString() { return getCodeString(); }

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

//		Feedlack.out("Feedlack figured out that this property " + assignee + " can affect output!");
		
		value.markOutputAffectingDataDependenciesHelper(stack);
		
	}

	public String getNameAssigned() {
		
		String assigneeString = assignee.getCodeString();
		int indexOfLastDot = assigneeString.lastIndexOf(".");
		String propertyName = assigneeString;
		if(indexOfLastDot >= 0) propertyName = assigneeString.substring(indexOfLastDot + 1);
		
		return propertyName;
		
	}
	
	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		
		if(assignee instanceof CFGGetPropertyNode) {

			String assigneeString = assignee.getCodeString();
			int indexOfLastDot = assigneeString.lastIndexOf(".");
			String propertyName = assigneeString;
			if(indexOfLastDot >= 0) propertyName = assigneeString.substring(indexOfLastDot + 1);

			// Remove the "on" and see if its an input event requiring output
			if(propertyName.length() >= 2 && JavaScriptDictionary.isInputEventNameRequiringOutput(propertyName.substring(2))) {

				// If the value is a function node, find the corresponding function
				// If the value is a name, find the function with the corresponding name (unless its a local name?)
				// If the value is an expression, who knows what will happen! It could be any function. What do we warn on this?
				return new JavaScriptInputHandler(propertyName.substring(2), this, 	feedlack.resolveFunctionReference(value));
				
			}
						
		}
		
		return null;
	}

	@Override
	public String getCodeString() {

		return assignee.getCodeString() + " = " + value.getCodeString();
		
	}

}