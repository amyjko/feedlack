package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGExpressionVoidNode extends CFGNode {

	private CFGNode expression;
	
	public CFGExpressionVoidNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	/*
	 * Basic nodes, which represent literals, identifiers, and other stuff, have no outgoing edges.
	 * We let the things that contain them decide what comes next.
	 */
	protected void constructEdges() {
		
		if(node.getFirstChild() != null) {

			this.expression = getCFGNodeFor(node.getFirstChild());
			addEdge(expression, "expression", Assumption.NONE);
		}
		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		assert false : "Nothing should be data dependent on a void expression.";
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	public String getCodeString() {
		
		return expression == null ? "[null expression void node]" : expression.getCodeString();
		
	}

}