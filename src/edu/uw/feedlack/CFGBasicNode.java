package edu.uw.feedlack;


import java.util.Stack;

import com.google.javascript.rhino.ScriptOrFnNode;
import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.Node;

public class CFGBasicNode extends CFGNode {

	public CFGBasicNode(Node node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		
		if(node.getType() == Token.NUMBER) return new TypeInfo(this, JavaScriptDictionary.NUMBER);
		else if(node.getType() == Token.STRING) return new TypeInfo(this, JavaScriptDictionary.STRING);
		else if(node.getType() == Token.TRUE || node.getType() == Token.FALSE) return new TypeInfo(this, JavaScriptDictionary.BOOL);
		else if(node.getType() == Token.THIS) {
			
			// Is this "this" reference declared in a function declared in an object literal? If so, it's type info
			// should include the scope of the object literal, so that functions called with respect to "this" first
			// search the object literal for matching functions. Not infallible, but a good guess.
			CFGFunction function = getFunction();
			ScriptOrFnNode funNode = function.getFunctionNode();
			if(funNode.getParent() != null && funNode.getParent().getType() == Token.OBJECTLIT) {
								
				JavaScriptType type = getFunction().getFeedlack().getObjectLiteralType(funNode.getParent());
				if(type != null)
					return new TypeInfo(this, type);
				
			}
			
			return new TypeInfo(this, JavaScriptDictionary.NONE);
			
		}
		else return new TypeInfo(this, JavaScriptDictionary.NONE);
		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {
		
	}

	@Override
	/*
	 * Basic nodes, which represent literals, identifiers, and other stuff, have no outgoing edges.
	 * We let the things that contain them decide what comes next.
	 */
	protected void constructEdges() {
		
	}

	public String toString() { 
		
		return node.toString(false, false, false); 
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	@Override
	public String getCodeString() {

		if(node.getType() == Token.NUMBER) return "" + node.getDouble();
		else if(node.getType() == Token.STRING) return "" + node.getString();
		else if(node.getType() == Token.THIS) return "this";
		else return "[BasicNode " + Token.name(node.getType()) + "]";
		
	}

}