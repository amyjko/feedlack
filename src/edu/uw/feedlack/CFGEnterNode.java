package edu.uw.feedlack;

import java.util.Stack;

import com.google.javascript.rhino.FunctionNode;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.ScriptOrFnNode;
import com.google.javascript.rhino.Token;

public class CFGEnterNode extends CFGNode {

	public CFGEnterNode(ScriptOrFnNode node, CFGFunction function) {
		super(node, function);
	}

	@Override
	public TypeInfo determineType() {
		return new TypeInfo(this, JavaScriptDictionary.NONE);
	}

	@Override
	/*
	 * Since a start node's node is the function, we iterate through until
	 * we find the function's BLOCK node. Then we get a node for it.
	 * 
	 */
	protected void constructEdges() {
		
		// If it's a function node, find the block.
		if(node instanceof FunctionNode) {
			for(Node child : node.children()) {
				if(child.getType() == Token.BLOCK) {
					addEdge(getCFGNodeFor(child), "beginning of function block", Assumption.NONE);
					return;
				}			
			}
			
			throw new RuntimeException("Didn't find a BLOCK child for the function node; something's wrong.");
		}
		// Otherwise, it's a script node. Construct a graph based on all of its children.
		else {
		    
			if(node.getChildCount() > 0) {

				CFGNode previous = null;
				for(int i = 0; i < node.getChildCount(); i++) {
					
					Node child = node.getChildAtIndex(i);
					if(!(child instanceof FunctionNode)) {
        					CFGNode childCFG = getCFGNodeFor(child);
        					// If this is the first one, add an edge from this call node to the first expression.
        					if(previous == null) addEdge(childCFG, "first statement in script", Assumption.NONE);
        					else previous.addEdgeToLeaves(childCFG, "next statement in script", Assumption.NONE);
        					previous = childCFG;
					}
					
				}
				
			}

		}
		
	}
	
	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

		assert false : "Nothing should be data dependent " + this;
		
	}

	public String toString() { return "enter"; }

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	public String getCodeString() {
		
		return "[enter]";
		
	}

}
