package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.Node;

/**
 * @author Andrew J. Ko
 *
 * This isn't actually the node that represents a call. It's just a special node to represent a name that represents a function call.
 *
 */
public class CFGInvocationNode extends CFGNode {

	public CFGInvocationNode(Node node, CFGFunction function) {
		super(node, function);
	}
	
	public TypeInfo determineType() {
		
		String name = node.getString();

		if(JavaScriptDictionary.GLOBAL.hasFunctionNamed(name))
			return new TypeInfo(this, JavaScriptDictionary.GLOBAL.getTypeOfProperty(name), JavaScriptDictionary.GLOBAL, name);
		else
			return new TypeInfo(this, JavaScriptDictionary.NONE);
		
	}

	public String getFunctionName() { return node.getString(); }

	@Override
	/*
	 * Basic nodes, which represent literals, identifiers, and other stuff, have no outgoing edges.
	 * We let the things that contain them decide what comes next.
	 */
	protected void constructEdges() {
		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		assert false : "Nothing should be data dependent " + this;
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	public String toString() { 
		
		return node.getString() + "()"; 
		
	}

	public String getCodeString() { 
		
		return node.getString(); 
		
	}

}