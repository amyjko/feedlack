package edu.uw.feedlack;

import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGTryNode extends CFGNode {

	private final CFGNoExceptionNode noException;
	
	public class CFGNoExceptionNode extends CFGNode {

		public CFGNoExceptionNode(Node node, CFGFunction function) {
			super(node, function);
		}
		
		@Override
		public TypeInfo determineType() {
			return new TypeInfo(this, JavaScriptDictionary.NONE);
		}

		protected void constructEdges() {}

		@Override
		public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
			return null;
		}
		protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) { assert false : "Shouldn't be data dependent on " + this; }

		public String getCodeString() { return "[no exception]"; }
		
	}

	public CFGTryNode(Node node, CFGFunction function) {

		super(node, function);
		
		noException = new CFGNoExceptionNode(node, function);

	}

	@Override
	protected void constructEdges() {

		CFGNode tryBlock = getCFGNodeFor(node.getChildAtIndex(0));
		addEdge(tryBlock, "try block", Assumption.NONE);

		tryBlock.addEdgeToLeaves(noException, "no exception", Assumption.NONE);
		
		// Add each catch block as an outgoing edge of the try block's graph. 
		for(int i = 1; i < node.getChildCount(); i++) {
			
			Node child = node.getChildAtIndex(i);
			CFGNode childCFG = getCFGNodeFor(child);
			addEdge(childCFG, "exception", Assumption.NONE);
			
		}

	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) { assert false : "Shouldn't be data dependent on " + this; }

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	@Override
	public String getCodeString() {
		return "try"; 
	}

}
