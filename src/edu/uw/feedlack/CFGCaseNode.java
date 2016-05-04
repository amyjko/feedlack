package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Token;

import com.google.javascript.rhino.Node;

public class CFGCaseNode extends CFGNode {

	public CFGCaseNode(Node node, CFGFunction function) {
		
		super(node, function);
		
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	protected void constructEdges() {

		if(node.getType() == Token.DEFAULT) {
			
			CFGNode block = getCFGNodeFor(node.getChildAtIndex(0)); 
			addEdge(block, "block", Assumption.NONE);
			
		}
		else {
			// The condition executes first.
			CFGNode value = getCFGNodeFor(node.getChildAtIndex(0)); 
			addEdge(value, "value", Assumption.NONE);
	
			CFGNode block = getCFGNodeFor(node.getChildAtIndex(1));
			value.addEdge(block, "block", Assumption.NONE);
		}
		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		assert false : "This shouldn't have data dependencies.";
		
	}
	
	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {		
		return null;
	}

	public String toString() {
		
		return "case";
		
	}
	
	public String getCodeString() { 
		
		if(node.getType() == Token.DEFAULT) return "default";
		else return "case " + getCFGNodeFor(node.getChildAtIndex(0)).getCodeString();
		
	}

}