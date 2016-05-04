package edu.uw.feedlack;

import java.util.*;

import com.google.javascript.rhino.Node;

public class CFGVarNode extends CFGNode {

	private final List<CFGNode> locals = new Vector<CFGNode>();
	
	public CFGVarNode(Node node, CFGFunction function) {
		super(node, function);
	}

	public List<CFGNode> getLocals() { return locals; }
	
	public JavaScriptType getTypeOfLocal(String name) {
		
		return JavaScriptDictionary.NONE;
		
	}
	
	public TypeInfo determineType() {
		
		return new TypeInfo(this, JavaScriptDictionary.NONE);
		
	}

	@Override
	protected void constructEdges() {

		if(node.getChildCount() > 0) {

			CFGNode previous = null;
			for(int i = 0; i < node.getChildCount(); i++) {
				
				Node child = node.getChildAtIndex(i);
				CFGNode childCFG = getCFGNodeFor(child);
				// If this is the first one, add an edge from this call node to the first expression.
				if(previous == null) addEdge(childCFG, "first in var", Assumption.NONE);
				else previous.addEdgeToLeaves(childCFG, "next in var", Assumption.NONE);
				previous = childCFG;
				
				locals.add(childCFG);
				
			}
			
		}
		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) { 

		if(node.getChildCount() > 0) {

			for(int i = 0; i < node.getChildCount(); i++) {
				
				Node child = node.getChildAtIndex(i);
				CFGNode childCFG = getCFGNodeFor(child);
				childCFG.markOutputAffectingDataDependenciesHelper(stack);

			}
		
		}
			
	}
		
	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		return null;
	}

	public String getCodeString() { 
		
		StringBuilder b = new StringBuilder();
		b.append("var ");
		for(CFGNode child : locals) {
			b.append(child.getCodeString());
			b.append(',');
		}
		return b.toString(); 
		
	}

}