package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGWhileNode extends CFGNode {

	private CFGNode condition, block;
	
	public CFGWhileNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	protected void constructEdges() {

		// For now we just assume that the condition is always true and we go through the loop once.
		
		this.condition = getCFGNodeFor(node.getChildAtIndex(0));
		addEdge(condition, "condition", Assumption.NONE);
		this.block = getCFGNodeFor(node.getChildAtIndex(1));
		condition.addEdgeToLeaves(block, "block", Assumption.WHILE_IS_TRUE_ONCE);
				
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		
		return null;
	
	}
	
	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) { assert false : "Shouldn't be data dependent on " + this; }

	public String getCodeString() {
		
		return "while";
		
	}

}