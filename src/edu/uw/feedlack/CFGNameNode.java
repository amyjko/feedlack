package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGNameNode extends CFGNode {

	public CFGNameNode(Node node, CFGFunction function) {
		super(node, function);
	}
	
	@Override
	public TypeInfo determineType() {
		
		String name = node.getString();
		
		JavaScriptType namedObjectLiteral = getFunction().getFeedlack().getNamedObjectLiteralType(name);
		if(namedObjectLiteral != null)
			return new TypeInfo(this, namedObjectLiteral);
		
		if(JavaScriptDictionary.GLOBAL.hasMemberNamed(name))
			return new TypeInfo(this, JavaScriptDictionary.GLOBAL.getTypeOfProperty(name), JavaScriptDictionary.GLOBAL, name);
		
		return new TypeInfo(this, JavaScriptDictionary.NONE);
		
	}

	@Override
	protected void constructEdges() {

		// Names have an optional initialization expression.
		if(node.hasOneChild())
			addEdge(getCFGNodeFor(node.getChildAtIndex(0)), "name assignment", Assumption.NONE);
		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		// There are two types of names we don't care about.
		// - Assignee of an assignment
		// - Name of function to call (we call these CFGInvocationNode, so these are taken care of).
		CFGNode parent = getCFGNodeFor(getASTNode().getParent());
		if(parent instanceof CFGAssignNode && ((CFGAssignNode)parent).getAssignee() == this)
			return;

		// If it's neither of these, we need to resolve it as one of these and mark assignments to it.
		// - Local variable (see the local use/def analysis to find recent assignments to mark
		// - Function argument (check to see if the argument name matches and if it does, find calls and mark the corresponding argument)
		// - Global variable (find all assignments to the global variable)
		
//		Feedlack.out("Need to mark " + this + " in " + getASTNode().getParent());
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		
		return null;
	}

	public String toString() {
		
		return node.getString();
		
	}
	
	public String getCodeString() { 
		
		return node.getString(); 
		
	}

	public String getName() {
		
		return node.getString(); 

	}

}