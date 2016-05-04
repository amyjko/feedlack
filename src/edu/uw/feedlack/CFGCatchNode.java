package edu.uw.feedlack;

import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGCatchNode extends CFGNode {

	public CFGCatchNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	protected void constructEdges() {

		CFGNode previous = null;
		for(int i = 0; i < node.getChildCount(); i++) {
			
			Node child = node.getChildAtIndex(i);
			CFGNode childCFG = getCFGNodeFor(child);
			// If this is the first one, add an edge from this call node to the first expression.
			if(previous == null) addEdge(childCFG, "error expression", Assumption.NONE);
			else previous.addEdgeToLeaves(childCFG, "catch block", Assumption.NONE);
			previous = childCFG;
			
		}

	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		assert false : "Nothing should be data dependent on a block; it returns no value.";
		
	}

	@Override
	public String getCodeString() {
		return "catch"; 
	}

}
