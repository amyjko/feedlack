package edu.uw.feedlack;

import java.util.*;

public class Scenario {

	private List<Decision> decisions = new Vector<Decision>();
	private final Set<InputHandler> calls;
	private final CFGFunction function;	// The function or script that this scenario starts with.
	private final boolean isLackingOutput;

	public Scenario(Set<InputHandler> calls, CFGFunction function, boolean showLackingPaths) {
		
		this.calls = calls;
		this.function = function;
		this.isLackingOutput = showLackingPaths;
		
	}

	public boolean isLackingOutput() { return isLackingOutput; }
	
	public Iterable<InputHandler> getInputHandlers() { return calls; }
	
	public int getLength() { return decisions.size(); }
	
	public Decision getNthDecision(int n) { return decisions.get(n); }
	
	public Iterable<Decision> getDecisions() { return decisions; }
	
	public boolean hasNoDecisions() { return decisions.size() == 0; } 
	
	public void addDecision(Decision d) {
		
		decisions.add(d);
		
	}

	public Likelihood getLikelihood() {
		
		int yes = 0, no = 0, maybe = 0;
		for(Decision decision : decisions) {

			if(decision.getLikelihood() == Likelihood.YES) yes++;
			else if(decision.getLikelihood() == Likelihood.MAYBE) maybe++;
			else if(decision.getLikelihood() == Likelihood.NO) no++;
			
		}
		
		if(no > 0) return Likelihood.NO;
		else if(maybe > 0) return Likelihood.MAYBE;
		else return Likelihood.YES;
		
	}

	
	public String toString() {
		
		StringBuilder sb = new StringBuilder();

		sb.append(function.getDescriptionOfLocation() + "\n");
		
    	for(Decision decision : decisions) {

    		sb.append(decision.getNode().getDescriptionOfLocation() + " " + decision.getNode().getClass().getSimpleName() + "\n");

    	}
    	
    	return sb.toString();
		
	}
	
}
