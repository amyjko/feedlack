/**
 * 
 */
package edu.uw.feedlack;

import java.io.File;
import java.util.*;

import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.ScriptOrFnNode;
import com.google.javascript.rhino.Token;

public abstract class CFGNode {
	
	private final CFGFunction function;
	protected Set<CFGEdge> outgoing = new HashSet<CFGEdge>();
	protected Node node;
	protected List<CFGNode> leaves = null;
	private TypeInfo type;
	
	public CFGNode(Node node, CFGFunction function) {
		
		this.node = node;
		this.function = function;
		
	}

	/**
	 * We use this to determine whether the conditional block can be skipped in the analysis.
	 */
	public boolean isConditionalBlock() {
		
		return false;
		
	}
	
	public Feedlack getFeedlack() {
		
		return function.getFeedlack();
		
	}
	
	public ScriptOrFnNode getFunctionNode() {
		
		return getFunction().getFunctionNode();
		
	}
	
	public void setTypeInfo(TypeInfo info) {
				
		this.type = info;
		
	}

	public final TypeInfo getTypeInfo() {
		
		if(type == null) { 
			type = determineType(); 
		}
		return type;
		
	}
	
	
	/**
	 * Follows a functions data dependencies, marking state as output affecting.
	 */
	protected abstract void markOutputAffectingDataDependencies(Stack<CFGNode> stack);
	
	public final void markOutputAffectingDataDependencies() {
		
		markOutputAffectingDataDependenciesHelper(new Stack<CFGNode>());
		
	}
	
	protected final void markOutputAffectingDataDependenciesHelper(Stack<CFGNode> stack) {

		if(stack.contains(this))
			return;

		stack.push(this);
			markOutputAffectingDataDependencies(stack);
		stack.pop();		
		
	}
	
	public String getDescriptionOfLocation() {
		
		StringBuilder sb = new StringBuilder();
		String file = getFunctionNode().getSourceName();
		file = file.substring(file.lastIndexOf('/') + 1);
		sb.append(file + ":" + getFeedlack().getLineNumberOfNode(this));
		return sb.toString();

	}
	
	/**
	 * Should interrogate its parse tree and find out what type, if any, the node represents.
	 */
	protected abstract TypeInfo determineType();
	
	public final boolean isNativeOutput() {

		return getOutputExplanation().getLikelihood() != Likelihood.NO;
		
	}
	
	public OutputExplanation getOutputExplanation() {

		return NO_OUTPUT_EFFECT;

	}
	
	public static final OutputExplanation NO_OUTPUT_EFFECT = new OutputExplanation(Likelihood.NO, "", "does not affect output.");
	
	/**
	 * By default, nodes don't always produce output. At the time of writing this comment,
	 * the only nodes that do are CFGCallNodes and CFGAssignNodes.
	 */
	public OutputExplanation alwaysAffectsOutput(Feedlack feedlack, Stack<CFGFunction> callstack) {
		
		return NO_OUTPUT_EFFECT;
		
	}

	/**
	 * By default, nodes don't always produce output. At the time of writing this comment,
	 * the only nodes that do are CFGCallNodes and CFGAssignNodes.
	 */
	public boolean canAffectOutput(Feedlack feedlack, Stack<CFGFunction> callstack) { 
		
		return false; 
		
	}
	
	public abstract JavaScriptInputHandler getInputHandler(Feedlack feedlack);
	
	/**
	 * Decompiler.
	 */
	public abstract String getCodeString();
	
	protected CFGNode getCFGNodeFor(Node node) { return getFunction().getCFGNodeFor(node); }

	// If, switch, etc.
	public boolean isConditional() { return false; }
	
	public Node getASTNode() { return node; }
	
	/*
	 * Returns the node passed in, for convenience.
	 */
	protected void addEdge(CFGNode nodeToPointTo, String label, Assumption ass) {
		
		// TODO
		// Don't add this edge if it's already been added. This is a nasty lazy hack
		// from a change from the outgoing edges to a set of nodes to a set of edges.
		for(CFGEdge edge : outgoing)
			if(edge.getTo() == nodeToPointTo)
				return;
		
		outgoing.add(new CFGEdge(this, nodeToPointTo, label, ass));
		
	}

	
	public boolean isTerminal() { return outgoing.size() == 0; }
		
	public Collection<CFGEdge> getOutgoingEdges() { return outgoing; }
	
	protected void addEdgeToLeaves(CFGNode nodeToPointTo, String label, Assumption ass) {
		
		if(leaves == null) throw new RuntimeException("Haven't computed leaves yet!");
		
		for(CFGNode leaf : leaves) {
			leaf.addEdge(nodeToPointTo, label, ass);
		}

	}
	
	private void computeLeaves() {

		leaves = new Vector<CFGNode>(3);
		Set<CFGNode> visited = new HashSet<CFGNode>();
		getLeaves(leaves, visited);

	}

	private final void getLeaves(List<CFGNode> leaves, Set<CFGNode> visited) {
		
		// This would happen, for example, if both paths through a conditional led to an endif.
		if(visited.contains(this)) {
			return;
		}
		else visited.add(this);
		
		if(this instanceof CFGExitNode) return;
		else if(isTerminal()) leaves.add(this);
		else {
			for(CFGEdge next : outgoing) {
				next.getTo().getLeaves(leaves, visited);
			}
		}
		
	}
	
	protected abstract void constructEdges();
	
	public void computeGraph() {
		
		constructEdges();
		computeLeaves();
		
	}
	
	public CFGFunction getFunction() { return function; }
	
	public String toGraphString(int depth) {
		
		String result = "";
		for(int i = 0; i < depth; i++) {
			result = result + "   ";
		}
		result = result + toString() + " -> \n";
		if(outgoing.size() == 1) result = result + outgoing.iterator().next().getTo().toGraphString(depth);
		else {
			for(CFGEdge edge : outgoing) {
				CFGNode out = edge.getTo();
				for(int i = 0; i < depth + 1; i++) {
					result = result + "   ";
				}
				result = result + "-------------\n";
				result = result + out.toGraphString(depth + 1) + "\n";
			}
		}
		
		return result;
		
	}
	
	public String toString() {

		return Token.name(node.getType()); 
		
	}
		
	public String toLineNumberString() { 

		int lineNumber = getFeedlack().getLineNumberOfNode(this);
		
		return (new File(getFunctionNode().getSourceName())).getName() + ":" + lineNumber;
		
	}
	
}