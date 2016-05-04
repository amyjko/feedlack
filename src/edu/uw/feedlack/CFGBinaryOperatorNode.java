package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGBinaryOperatorNode extends CFGNode {

	private CFGNode left, right;
	
	public CFGBinaryOperatorNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NUMBER);
	}

	@Override
	/*
	 * Block node points to it's first child.
	 */
	protected void constructEdges() {

		this.left = getCFGNodeFor(node.getChildAtIndex(0));
		addEdge(left, "value expression", Assumption.NONE);
		this.right = getCFGNodeFor(node.getChildAtIndex(1));
		left.addEdgeToLeaves(right, "right", Assumption.NONE);
				
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		left.markOutputAffectingDataDependenciesHelper(stack);
		right.markOutputAffectingDataDependenciesHelper(stack);
		
	}

	@Override
	public String getCodeString() {

		return left.getCodeString() + " op " + right.getCodeString();
		
	}

}