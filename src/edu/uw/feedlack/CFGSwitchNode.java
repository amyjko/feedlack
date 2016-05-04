package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGSwitchNode extends CFGNode {

	private CFGNode value;
	private CFGEndSwitchNode end;
	private CFGSwitchDecisionNode decision;
	
	public class CFGEndSwitchNode extends CFGNode {

		public CFGEndSwitchNode(Node node, CFGFunction function) {
			super(node, function);
		}
		
		@Override
		public TypeInfo determineType() { return new TypeInfo(this, JavaScriptDictionary.NONE); }

		protected void constructEdges() {}

		@Override
		public JavaScriptInputHandler getInputHandler(Feedlack feedlack) { return null; }
		public String toString() { return "endswitch"; }
		public String getCodeString() { return "endswitch"; }
		protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) { assert false : "Shouldn't be data dependent on " + this; }
		
	}
	
	// We have this so that there's something to represent the whole switch when we're digging through the AST node's children looking for output affecting gems.
	public class CFGSwitchDecisionNode extends CFGNode {

		public CFGSwitchDecisionNode(Node node, CFGFunction function) {
			super(node, function);
		}
		
		@Override
		public TypeInfo determineType() { return new TypeInfo(this, JavaScriptDictionary.NONE); }

		protected void constructEdges() {}

		@Override
		public JavaScriptInputHandler getInputHandler(Feedlack feedlack) { return null; }
		public String toString() { return "switchdecision"; }
		public String getCodeString() { return "switchdecision"; }
		public boolean isConditionalBlock() { return true; }

		protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) { assert false : "Shouldn't be data dependent on " + this; }

	}

	public CFGSwitchNode(Node node, CFGFunction function) {
		
		super(node, function);
		end = new CFGEndSwitchNode(node, function);
		decision = new CFGSwitchDecisionNode(node, function);
		
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	protected void constructEdges() {

		// The condition executes first.
		value = getCFGNodeFor(node.getChildAtIndex(0)); 
		addEdge(value, "value expression", Assumption.NONE);
		
		value.addEdgeToLeaves(decision, "switchdecision", Assumption.NONE);
		
		// Then, depending on the result of the condition, we go to one of the cases.
		for(int i = 1; i < node.getChildCount(); i++) {
			
			CFGNode caseNode = getCFGNodeFor(node.getChildAtIndex(i));
			decision.addEdge(caseNode, "switch", Assumption.NONE);
			caseNode.addEdgeToLeaves(end, "endswitch", Assumption.NONE);
			
		}

		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) { assert false : "Shouldn't be data dependent on " + this; }

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {		
		return null;
	}

	public String toString() {
		
		return "switch";
		
	}
	
	public String getCodeString() { 
		
		return "switch"; 
		
	}

}