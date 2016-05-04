package edu.uw.feedlack;

import java.util.Stack;

import com.google.javascript.rhino.ScriptOrFnNode;

public class CFGExitNode extends CFGNode {

	public CFGExitNode(ScriptOrFnNode node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	/*
	 * Exits have no edges out.
	 */
	protected void constructEdges() {

		
		
	}

	
	protected void addEdge(CFGNode nodeToPointTo, String label) {
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		assert false : "Nothing should be data dependent " + this;
		
	}

	public String toString() { return "exit"; }

	public String getCodeString() {
		
		return "[exit]";
		
	}

}
