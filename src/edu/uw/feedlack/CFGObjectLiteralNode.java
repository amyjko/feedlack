package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.FunctionNode;
import com.google.javascript.rhino.Node;

public class CFGObjectLiteralNode extends CFGNode {

	public CFGObjectLiteralNode(Node node, CFGFunction function) {
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

		// There should be a value for every property...
		assert node.getChildCount() % 2 == 0 : "What kind of object literal declares a property with no value?";

		// We evaluate each of the value expressions in succession, except for functions.
		CFGNode previous = null;
		for(int i = 0; i < node.getChildCount(); i+=2) {
			
			Node property = node.getChildAtIndex(i);
			Node value = node.getChildAtIndex(i + 1);

			if(!(value instanceof FunctionNode)) {
				CFGNode childCFG = getCFGNodeFor(value);
				// If this is the first one, add an edge from this call node to the first expression.
				if(previous == null) addEdge(childCFG, "first object property value", Assumption.NONE);
				else previous.addEdgeToLeaves(childCFG, "next object property value", Assumption.NONE);
				previous = childCFG;
			}
			
		}
		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		for(int i = 0; i < node.getChildCount(); i+=2) {
			
			Node value = node.getChildAtIndex(i + 1);
			getCFGNodeFor(value).markOutputAffectingDataDependenciesHelper(stack);
			
		}
		
	}
	
	
	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	@Override
	public String getCodeString() {

		return "[object literal]";
		
	}

	
}