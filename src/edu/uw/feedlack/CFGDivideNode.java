package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.Node;

public class CFGDivideNode extends CFGNode {

	public class CFGEndDivideNode extends CFGNode {

		public CFGEndDivideNode(Node node, CFGFunction function) {
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
		public String toString() { return "enddivide"; }
		public String getCodeString() { return "[enddivide]"; }
		
		protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

			assert false : "Nothing should be data dependent " + this;
			
		}

	}
	
	private CFGNode left, right;
	private final CFGEndDivideNode end;
	
	public CFGDivideNode(Node node, CFGFunction function) {
		super(node, function);
		
		end = new CFGEndDivideNode(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NUMBER);
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		left.markOutputAffectingDataDependenciesHelper(stack);
		right.markOutputAffectingDataDependenciesHelper(stack);
	
	}

	@Override
	/*
	 * Block node points to it's first child.
	 */
	protected void constructEdges() {

		this.left = getCFGNodeFor(node.getChildAtIndex(0));
		addEdge(left, "numerator", Assumption.NONE);
		this.right = getCFGNodeFor(node.getChildAtIndex(1));
		left.addEdgeToLeaves(right, "denominator", Assumption.NONE);
		
		// Then, after evaluating the left and right, this division either succeeds and goes to the
		// end node, or it's a zero and goes to the end of the function.
		right.addEdge(end, "divide", Assumption.NON_ZERO);
		
		// We only add an edge for divide by zero if it's not a number.
		if(right.getASTNode().getType() != Token.NUMBER)
			right.addEdge(getFunction().getExit(), "divide by zero", Assumption.DIVIDE_BY_ZERO);
				
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	@Override
	public String getCodeString() {

		return left.getCodeString() + " / " + right.getCodeString();
		
	}

}