package edu.uw.feedlack;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class ScenarioCluster implements Iterable<Scenario> {

	private final Set<Scenario> scenarios = new HashSet<Scenario>();
	
	public ScenarioCluster() {
		
		
		
		
	}

	public Iterator<Scenario> iterator() {
		
		return scenarios.iterator();

	}

	public void add(Scenario scenario) {
		
		scenarios.add(scenario);
		
	}

	public int getScenarioCount() {
		
		return scenarios.size();

	}
	
	public boolean isLackingOutput() {
		
		int lacking = 0, notLacking = 0;
		for(Scenario s : scenarios)
			if(s.isLackingOutput())
				lacking++;
			else
				notLacking++;
		
		assert ((lacking > 0 && notLacking == 0) || (notLacking > 0 && lacking == 0)) : "This cluster consist of scenarios that represent both output lacking and non-lacking paths";
		
		return lacking > 0;
		
	}
	
	public List<InputHandler> getInputHandlers() { 
		
		Set<InputHandler> handlers = new HashSet<InputHandler>();
		for(Scenario s : scenarios)
			for(InputHandler h : s.getInputHandlers())
				handlers.add(h);
		
		return new Vector<InputHandler>(handlers);
		
	}
	
	public List<Decision> getCommonSequence() {
		
		final Map<CFGNode, Integer> numbers = new HashMap<CFGNode, Integer>();
		Map<CFGNode, Decision> decisions = new HashMap<CFGNode, Decision>();

		// This set will represent the nodes that all scenarios in this cluster have in common
		Set<Decision> intersection = new HashSet<Decision>();

		// If this is the first scenario, we use it to determine an ordering for the nodes.
		boolean first = true;
		
		for(Scenario s : scenarios) {

			// Number all of the nodes in this scenario's node sequence so we know what order to put them in.
			// Add all of the nodes to the intersection set.
			if(first) {
				int number = 1;
				for(Decision d : s.getDecisions()) {
				
					// If we included exit nodes in the intersection, we wouldn't return contiguous paths. Exit nodes are the only nodes
					// to which paths can converge, because we don't return exits to conditional.
					if(!(d.getNode() instanceof CFGExitNode)) {
					
						intersection.add(d);
						numbers.put(d.getNode(), number);
						decisions.put(d.getNode(), d);
						number++;
						
					}
					
				}
			}
			// If this isn't the first scenario, find the intersection between the current intersection set
			// and this scenarios nodes, leaving only the nodes they have in common. 
			else {

				Set<Decision> set = new HashSet<Decision>();
				for(Decision d : s.getDecisions()) {

					CFGNode node = d.getNode();
					set.add(d);

					// Does this node have a number? If not,
					// what do we number it?
					if(!numbers.containsKey(node)) {
						
					}
					
				}
				intersection.retainAll(set);
			}
				
			// Finished with the first scenario in this cluster; all others in this loop are not first
			first = false;
			
		}
		
		// Now that we've found the intersection of the scenarios in this cluster, sort them by the numbers we assigned
		// to the nodes in the first set.
		List<Decision> intersectionList = new Vector<Decision>();
		for(Decision n : intersection)
			intersectionList.add(n);

		Collections.sort(intersectionList, new Comparator<Decision>() {
			@Override
			public int compare(Decision o1, Decision o2) {
				// There will always be a number, because we are guaranteed to only seek numbers for nodes
				// in the global intersection of the scenarios in this cluster (which must be in the first set).
				return numbers.get(o1.getNode()) - numbers.get(o2.getNode());
			}});
		
		return intersectionList;
				
	}
	
}
