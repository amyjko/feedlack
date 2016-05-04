package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGFunctionNode extends CFGNode {

	public CFGFunctionNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.FUNCTION);
	}

	@Override
	/*
	 * Just skip over function declarations. They should have no outgoing edges.
	 */
	protected void constructEdges() {

	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		
	}

	public String getCodeString() {
		
		return "[function]";
		
	}

}