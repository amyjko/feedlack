package edu.uw.feedlack;

import java.util.Stack;

import com.google.javascript.rhino.FunctionNode;
import com.google.javascript.rhino.Node;

public class CFGArrayLiteralNode extends CFGNode {

	public CFGArrayLiteralNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	/*
	 * Block node points to it's first child.
	 */
	protected void constructEdges() {

		// We evaluate each of the value expressions in succession, except for functions.
		CFGNode previous = null;
		for(int i = 0; i < node.getChildCount(); i++) {
			
			Node value = node.getChildAtIndex(i);

			if(!(value instanceof FunctionNode)) {
				CFGNode childCFG = getCFGNodeFor(value);
				// If this is the first one, add an edge from this call node to the first expression.
				if(previous == null) addEdge(childCFG, "first array value", Assumption.NONE);
				else previous.addEdgeToLeaves(childCFG, "next array value", Assumption.NONE);
				previous = childCFG;
			}
			
		}
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		for(int i = 0; i < node.getChildCount(); i++) {
			getCFGNodeFor(node.getChildAtIndex(i)).markOutputAffectingDataDependenciesHelper(stack);
		}
		
	}

	@Override
	public String getCodeString() {

		return "[array literal]";
		
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.ARRAY);
	}

	
}