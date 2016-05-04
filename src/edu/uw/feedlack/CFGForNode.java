package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGForNode extends CFGNode {

	private CFGNode declaration, condition, action, block;
	
	public CFGForNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	protected void constructEdges() {

		// For now we just assume that the condition is always true and we go through the loop once.
		
		this.declaration = getCFGNodeFor(node.getChildAtIndex(0));
		addEdge(declaration, "declaration", Assumption.NONE);

		this.condition = getCFGNodeFor(node.getChildAtIndex(1));
		declaration.addEdgeToLeaves(condition, "condition", Assumption.NONE);
		
		if(node.getChildCount() == 4) {

			this.block = getCFGNodeFor(node.getChildAtIndex(3));
			condition.addEdgeToLeaves(block, "block", Assumption.FOR_IS_TRUE_ONCE);
			
			this.action = getCFGNodeFor(node.getChildAtIndex(2));
			block.addEdgeToLeaves(action, "action", Assumption.NONE);

		}
		else {

			this.action = null;
			this.block = getCFGNodeFor(node.getChildAtIndex(2));
			
		}
		
		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		assert false : "Nothing should be data dependent " + this;
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		
		return null;
	
	}
	
	public String getCodeString() {
		
		return "for loop";
		
	}

}