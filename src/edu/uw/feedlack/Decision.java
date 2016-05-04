package edu.uw.feedlack;

public class Decision {
	
	private final CFGNode node;
	private final String decision;
	private final String reason;
	private final Assumption assumption;
	private final Likelihood likelihood;
	private final boolean isNative;
	private final boolean isCallSequence;
	
	public Decision(CFGNode c, String d, String r, Assumption ass, Likelihood l, boolean isNative, boolean isCallSequence) {
		
		node = c;
		decision = d;
		reason = r;
		assumption = ass;
		likelihood = l;
		this.isNative = isNative;
		this.isCallSequence = isCallSequence;
		
	}

	public String getDecision() { return decision; }
	public CFGNode getNode() { return node; }
	public String getReason() { return reason; }
	public Likelihood getLikelihood() { return likelihood; }	
	public boolean isNative() { return isNative; }
	
	public String toString() {
		
		return node.getCodeString() + " is " + decision + " (" + node.toLineNumberString() + ")";
		
	}

	public String getKind() {

		return 
			isCallSequence ? "calls" :
			node.isConditional() ? "conditional" : 
			node instanceof CFGCallNode ? "invocation" : 
			node.isNativeOutput() ? "output" : 
			node instanceof CFGEnterNode ? "enter" : 
			node instanceof CFGExitNode ? "exit" : 
			node instanceof CFGCaseNode ? "case" : 
			decision.equals("exception") ? "exception" : 
			"unknown";
	
	}

	public Assumption getAssumption() { 
		return assumption;
	}
	
	public boolean equals(Object o) {
		
		if(!(o instanceof Decision)) return false;
		
		if(node != ((Decision)o).node) return false;
		
		return decision.equals(((Decision)o).decision);
		
	}
	
	public int hashCode() {
		
		return System.identityHashCode(node) * decision.hashCode();
		
	}
	
}