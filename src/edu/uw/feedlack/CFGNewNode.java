package edu.uw.feedlack;

import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGNewNode extends CFGNode {

	public CFGNewNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	protected void constructEdges() {

		// First, add successive edges to all expressions of this call, skipping over the name.
		CFGNode previous = null;
		for(int i = 1; i < node.getChildCount(); i++) {
			
			Node child = node.getChildAtIndex(i);
			CFGNode childCFG = getCFGNodeFor(child);
			// If this is the first one, add an edge from this call node to the first expression.
			if(previous == null) addEdge(childCFG, "next argument", Assumption.FUNCTION_EXISTS);
			else {
				previous.addEdgeToLeaves(childCFG, "next argument", Assumption.NONE);
			}
			previous = childCFG;
			
		}

	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		for(int i = 1; i < node.getChildCount(); i++) {
			
			Node child = node.getChildAtIndex(i);
			getCFGNodeFor(child).markOutputAffectingDataDependenciesHelper(stack);
		
		}

	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	public String getCodeString() { 
		
		return "[new]"; 
		
	}

}