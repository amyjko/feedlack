package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGUnaryOperatorNode extends CFGNode {

	public CFGUnaryOperatorNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.BOOL);
	}

	@Override
	/*
	 * Block node points to it's first child.
	 */
	protected void constructEdges() {

		CFGNode left = getCFGNodeFor(node.getChildAtIndex(0));
		addEdge(left, "expression", Assumption.NONE);
				
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) { 
		
		getCFGNodeFor(node.getChildAtIndex(0)).markOutputAffectingDataDependenciesHelper(stack);
		
	}

	public String getCodeString() { 
		
		return "[unary]"; 
		
	}

}