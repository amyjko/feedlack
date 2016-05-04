package edu.uw.feedlack;

import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGIfNode extends CFGNode {

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

		public boolean isConditionalBlock() { return true; }

		protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

			assert false : "Nothing should be data dependent " + this;
			
		}

	}

	private CFGEndIfNode end;
	private CFGIfDecisionNode decision;
	
	public CFGIfNode(Node node, CFGFunction function) {
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

		// An if block has two or three children:
		// 1) expression
		// 2) block
		// 3) optional else block

		// Expression executes first.
		CFGNode expression = getCFGNodeFor(node.getChildAtIndex(0)); 
		addEdge(expression, "conditional expression", Assumption.NONE);
		
		// Add the if block after the last node of the expression.
		CFGNode thenBlock = getCFGNodeFor(node.getChildAtIndex(1));
		expression.addEdgeToLeaves(decision, "?", Assumption.NONE);

		decision.addEdge(thenBlock, "true", Assumption.CONDITION_CAN_BE_TRUE);
		thenBlock.addEdgeToLeaves(end, "endif", Assumption.NONE);
		
		// Add one of two possible edges to the expression: the else block, or if there isn't one, the next node after this if.
		if(node.getChildCount() > 2) {
			CFGNode elseBlock = getCFGNodeFor(node.getChildAtIndex(2));
			decision.addEdge(elseBlock, "false", Assumption.CONDITION_CAN_BE_FALSE);
			elseBlock.addEdgeToLeaves(end, "endif", Assumption.NONE);
		} else {
			decision.addEdge(end, "false", Assumption.NONE);
		}
		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		assert false : "Nothing should be data dependent " + this;
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}
	
	public String getCodeString() { return "[if]"; }

}
