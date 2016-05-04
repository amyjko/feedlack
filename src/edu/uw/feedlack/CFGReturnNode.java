package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGReturnNode extends CFGNode {

	public CFGReturnNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	protected void constructEdges() {

		if(node.hasOneChild()) {
			CFGNode expr = getCFGNodeFor(node.getChildAtIndex(0));
			addEdge(expr, "return expression", Assumption.NONE);
			expr.addEdgeToLeaves(getFunction().getExit(), "return", Assumption.NONE);
		}
		else {
			addEdge(getFunction().getExit(), "return", Assumption.NONE);
		}

	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		if(node.hasOneChild())
			getCFGNodeFor(node.getChildAtIndex(0)).markOutputAffectingDataDependenciesHelper(stack);

	}

	public String getCodeString() { 
		
		return "[return]"; 
		
	}

}