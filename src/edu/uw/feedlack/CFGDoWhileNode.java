package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGDoWhileNode extends CFGNode {

	private CFGNode condition, block;
	
	public CFGDoWhileNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	protected void constructEdges() {

		// For now we just assume that the condition is always true and we go through the loop once.
		this.block = getCFGNodeFor(node.getChildAtIndex(0));
		addEdge(block, "block", Assumption.WHILE_IS_TRUE_ONCE);

		this.condition = getCFGNodeFor(node.getChildAtIndex(1));
		block.addEdgeToLeaves(condition, "property", Assumption.NONE);
				
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		assert false : "Nothing should be data dependent " + this;
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		
		return null;
	
	}
	
	public String getCodeString() {
		
		return "do while";
		
	}

}