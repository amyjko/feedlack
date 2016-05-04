package edu.uw.feedlack;

public class CFGEdge implements AbstractCFGEdge {

	private final CFGNode from, to;
	private final String label;
	private Assumption assumption = Assumption.NONE;
	
	public CFGEdge(CFGNode from, CFGNode to, String label, Assumption assumption) {
		
		this.from = from;
		this.to = to;
		this.label = label;
		this.assumption = assumption;
		
		if(from == to) throw new RuntimeException("You can't just make an edge from this node to itself.");
		
	}

	public int length() { return 1; }
	
	public CFGNode getFrom() { return from; }
	
	public CFGNode getTo() { return to; }

	public String getLabel() { return label; }
	
	public String toString() { return " -> " + to.toString(); }

	public void setAssumption(Assumption assumption) {

		assert assumption != null;
		// We want to ensure that we're only overriding edges with no assumptions. If this isn't true, I want to know about it.
		this.assumption = assumption;
		
	}

	public Assumption getAssumption() {
		
		return assumption;

	} 
	
	
	public CFGNode getLastNode() {

		return getTo();
		
	}
	
}
