package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.FunctionNode;
import com.google.javascript.rhino.Node;

public class CFGBlockNode extends CFGNode {

	public CFGBlockNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	/*
	 * Block node points to it's first child.
	 */
	protected void constructEdges() {

		if(node.getChildCount() > 0) {

			CFGNode previous = null;
			for(int i = 0; i < node.getChildCount(); i++) {
				
				Node child = node.getChildAtIndex(i);
				if(!(child instanceof FunctionNode)) {
    				CFGNode childCFG = getCFGNodeFor(child);
    				// If this is the first one, add an edge from this call node to the first expression.
    				if(previous == null) addEdge(childCFG, "first in block", Assumption.NONE);
    				else previous.addEdgeToLeaves(childCFG, "next in block", Assumption.NONE);
    				previous = childCFG;
				}
				
			}
			
		}
		
	}
	
	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		assert false : "Nothing should be data dependent on a block; it returns no value.";
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	@Override
	public String getCodeString() {

		return "[block code]";
		
	}

	
}