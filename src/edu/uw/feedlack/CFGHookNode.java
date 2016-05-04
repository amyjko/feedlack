package edu.uw.feedlack;

import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGHookNode extends CFGNode {

	public class CFGEndIfNode extends CFGNode {

		public CFGEndIfNode(Node node, CFGFunction function) {
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
		public String toString() { return "endif"; }
		public String getCodeString() { return "[endif]"; }
		
		protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

			assert false : "Nothing should be data dependent " + this;
			
		}

	}

	public class CFGIfDecisionNode extends CFGNode {

		public CFGIfDecisionNode(Node node, CFGFunction function) {
			super(node, function);
		}
		
		@Override
		public TypeInfo determineType() {
			return new TypeInfo(this, JavaScriptDictionary.NONE);
		}

		protected void constructEdges() {}
		public boolean isConditional() { return true; }

		@Override
		public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
			return null;
		}
		public String toString() { return "if"; }
		public String getCodeString() { return "[if]"; }
		
		public CFGEndIfNode getEndIfNode() { return end; }

		protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

			assert false : "Nothing should be data dependent " + this;
			
		}

	}

	private CFGEndIfNode end;
	private CFGIfDecisionNode decision;
	
	public CFGHookNode(Node node, CFGFunction function) {
		super(node, function);
		end = new CFGEndIfNode(node, function);
		decision = new CFGIfDecisionNode(node, function);
	}
	
	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	protected void constructEdges() {

		// Expression executes first.
		CFGNode expression = getCFGNodeFor(node.getChildAtIndex(0)); 
		addEdge(expression, "conditional expression", Assumption.NONE);
		
		// Add the if block after the last node of the expression.
		CFGNode thenBlock = getCFGNodeFor(node.getChildAtIndex(1));
		expression.addEdgeToLeaves(decision, "?", Assumption.NONE);

		decision.addEdge(thenBlock, "true", Assumption.NONE);
		thenBlock.addEdgeToLeaves(end, "endif", Assumption.NONE);
		
		CFGNode elseBlock = getCFGNodeFor(node.getChildAtIndex(2));
		decision.addEdge(elseBlock, "false", Assumption.NONE);
		elseBlock.addEdgeToLeaves(end, "endif", Assumption.NONE);
		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		assert false : "Nothing should be data dependent " + this;
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	public String getCodeString() {
		
		return "[expr ? a : b]";
		
	}

}
