package edu.uw.feedlack;

import java.util.*;

public class CFGPath implements Iterable<CFGEdge> {

	private final List<AbstractCFGEdge> path = new Vector<AbstractCFGEdge>(10);
//	private final Set<CFGNode> nodesExplicitlyInThisPath = new HashSet<CFGNode>();
//	private final List<CFGPathEdge> pathsInThisPath = new Vector<CFGPathEdge>();
	
	public static class CycleException extends Exception {

		public CycleException() {
			
			super("We're in a loopy path!");
			
		}
		
	}
	
	/**
	 * Create an empty path.
	 */
	public CFGPath() {
		
	}
	
	/**
	 * This iterator simply goes through the list of abstract edges in a path, bounded by the specified first and last indices.
	 * 
	 * @author Andrew J. Ko
	 */
	private static class PathSegmentIterator implements Iterator<AbstractCFGEdge> {
		
		private final CFGPath path;
		private int currentIndex;
		private final int firstIndex, lastIndex;
		
		public PathSegmentIterator(CFGPath path, int firstIndex, int lastIndex) {

			this.path = path;
			this.firstIndex = firstIndex;
			this.lastIndex = lastIndex;
			this.currentIndex = firstIndex - 1;
			
		}
		
		@Override
		public boolean hasNext() {
			
			return currentIndex + 1 <= lastIndex;

		}

		@Override
		public AbstractCFGEdge next() {

			currentIndex++;
			return path.path.get(currentIndex);

		}

		@Override
		public void remove() {

			throw new UnsupportedOperationException("Can't remove edges from paths");
			
		}

	}
	
	/**
	 * This iterator maintains a stack of PathSegmentIterators and moves through them.
	 * 
	 * @author Andrew J. Ko
	 *
	 */
	private static class ConcreteEdgeIterator implements Iterator<CFGEdge> {
		
		/**
		 * We use this to keep track of which nested path we're in. We need this so that when
		 * we reach the end of a path, we know which path to pop back into.
		 */
		private Stack<PathSegmentIterator> iteratorStack = new Stack<PathSegmentIterator>();
		private boolean gotNext = false;
		private CFGEdge next = null;

		public ConcreteEdgeIterator(CFGPath path, int firstIndex, int lastIndex) {
			
			// This path iterator starts and stops at particular indices.
			iteratorStack.add(new PathSegmentIterator(path, firstIndex, lastIndex));
			
		}
						
		@Override
		public boolean hasNext() {

//			// Check the iterator stack, from top to bottom, until finding an iterator that has a next node.
//			// If we do find one, this has a next edge, otherwise it does not.
//			for(int level = iteratorStack.size() - 1; level >= 0; level--) {
//				
//				if(iteratorStack.get(level).hasNext())
//					return true;
//				
//			}
//			
//			return false;
			
			int i = 0;
			
			if(!gotNext) {
			
				next = null;
				
				// Does the iterator on the to of the stack have a next? If so, get it and return it, otherwise pop and keep searching the available iterators.
				// If the next edge in the iterator is a path edge, push it onto the stack.
				while(!iteratorStack.isEmpty()) {
					
					// If this iterator has a next, get it.
					if(iteratorStack.peek().hasNext()) {
					
						AbstractCFGEdge nextEdge = iteratorStack.peek().next();
				
						// If this is a concrete edge, return it.
						if(nextEdge instanceof CFGEdge) {
						
							next = (CFGEdge)nextEdge;
							break;
						
						}
						// If this is a path edge, create a path segment iterator and add it to the stack. Keep going until we find a concrete edge.
						else if(nextEdge instanceof CFGPathEdge) {
						
							CFGPathEdge pathEdge = (CFGPathEdge)nextEdge;
							iteratorStack.push(new PathSegmentIterator(pathEdge.getPath(), pathEdge.getFirstIndex(), pathEdge.getLastIndex()));
							
						}
						
					}
					else iteratorStack.pop();
					
				}
				
				gotNext = true;
				
			}
			
			return next != null;
			
		}

		@Override
		public CFGEdge next() {

			if(!gotNext) hasNext();
			gotNext = false;
			return next;
			
		}

		@Override
		public void remove() {
			
			throw new UnsupportedOperationException("Can't remove edges from paths");
			
		}
		
		class PathPosition {
			
			CFGPath path;
			int currentIndex;
			
			public PathPosition(CFGPath path, int currentIndex) {
				
				this.path = path;
				this.currentIndex = currentIndex;
				
			}
			
		}

	}
	
	public Iterator<CFGEdge> iterator() {
		
		// Return a special kind of iterator, which can handle both concrete edges and edges to paths.
		return new ConcreteEdgeIterator(this, 0, path.size() - 1);
		
	}
	
	public AbstractCFGEdge getEdgeAtIndex(int index) {
		
		return path.get(index);
		
	}
	
	public void addEdge(CFGEdge edge) throws CycleException {

		path.add(edge);

		CFGNode node = edge.getTo();
		
		assert node != null : "There shouldn't be any null nodes in a node path";
		
//		if(contains(node)) {
//			throw new CycleException();
//		}

//		nodesExplicitlyInThisPath.add(node);

	}
	
//	public boolean contains(CFGNode node) {
//		
//		if(nodesExplicitlyInThisPath.contains(node)) {
//			return true;
//		}
//		for(CFGPathEdge pathEdge : pathsInThisPath)
//			if(pathEdge.getPath().contains(node))
//				return true;
//		
//		return false;
//		
//	}
	
	/**
	 * Add the given path, as is, to this path. Future additions to the given path will not be part of this path.
	 */
	public void addPath(CFGPath newPath) throws CycleException {

		// TODO This is really slow. I realize it might cause correctness problems, but it's getting in my way.
//		for(CFGEdge edge : newPath)
//			if(contains(edge.getTo()))
//				throw new CycleException();
		
		// Add the segment of the path as it is now, from the first node to the last node in the path.
		// We remember the last index of the given path so that future additions to the path aren't included.
		path.add(new CFGPathEdge(newPath, 0, newPath.path.size() - 1));

	}
			
	public CFGNode getLastNode() { 
		
		// Ask the last edge in this path for it's last node. If it's a concrete edge, it'll return it's 
		// node. If it's a path, it will recursively ask the path's last edge for it's last node.
		return path.get(path.size() - 1).getLastNode(); 
		
	}
	
	public boolean alwaysProducesOutput(Feedlack feedlack) {
		
		Stack<CFGFunction> callstack = new Stack<CFGFunction>();
		if(length() > 0) callstack.push(iterator().next().getTo().getFunction());
		
		return alwaysProducesOutput(feedlack, callstack);
		
	}
	
	protected boolean alwaysProducesOutput(Feedlack feedlack, Stack<CFGFunction> callstack) {
		
		// Guilty until proven innocent.
		boolean producesOutput = false;
						
		// Go through each node in the path. Ask the node if it produces output. If it does, goody good, we're done.
		// If it doesn't, continue.
		for(CFGEdge edge : this) {
			
			CFGNode node = edge.getTo();

			OutputExplanation explanation = node.alwaysAffectsOutput(feedlack, callstack); 
			
			if(explanation.getLikelihood() == Likelihood.YES) {
				producesOutput = true;
				break;
			}
			
			
		}
		
		return producesOutput;
		
	}
	
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		for(CFGEdge edge : this) {
			sb.append(edge.toString() + " ");			
		}
		
		return sb.toString();
		
	}

	public int length() { 
	
		int totalLength = 0;
		for(int i = 0; i < path.size(); i++)
			totalLength += path.get(i).length(); 		
	
		return totalLength;
		
	}
	
	public Collection<CFGPath> getPathsWithExpandedOutputProducingInvocations(Feedlack feedlack, Stack<CFGFunction> callstack) throws CFGFunction.TooManyPathsException {
		
		// Go through this path and for each invocation node that might produce output
		// get all of it's expanded paths.
		Collection<CFGPath> invocationExpandedPaths = new Vector<CFGPath>();
		invocationExpandedPaths.add(new CFGPath());

		for(CFGEdge edge : this) {
			
			CFGNode node = edge.getTo();
			
			// Add the next node to all of the current expanded paths.
			// If we've already been there, break out of this loop, because we're done.
			try {
				for(CFGPath expandedPath : invocationExpandedPaths) {
					expandedPath.addEdge(edge);
				}
			} catch (CycleException e) {
				return invocationExpandedPaths;
			}

			// If this is a call, find the function and get it's paths!
			if(node instanceof CFGCallNode) {

				// After we're done going through the matches, we'll replace the invocation expanded paths with these new paths.
				Collection<CFGPath> newExpandedPaths = new Vector<CFGPath>();

				// Find the functions, but use the call node.
				CFGCallNode call = (CFGCallNode)node;
				Set<CFGFunction> matches = feedlack.resolveFunctionReference(call);

				// If this is a call to setInterval() or setTimeout(), also resolve the function passed in.
				if(call.getFunctionCalled().equals("setInterval") || call.getFunctionCalled().equals("setTimeout")) {
					matches = call.resolveTimeoutIntervalFunctions(feedlack);
				}
				
				// If there were any matches at all...
				if(matches != null && matches.size() > 0) {
					
					// Before expanding paths, find out if any of them can affect output (and aren't trusted functions).
					boolean noneAreOutputAffecting = true;
					for(CFGFunction function : matches) {
						if(function.canAffectOutput(feedlack) && !function.isTrusted()) {
							noneAreOutputAffecting = false;
							break;
						}
					}
					
					// If at least one of the potential functions called can affect output... 
					if(!noneAreOutputAffecting) {
					
						// Then go through all of the matches and append their various paths after the most recent edge in this loop.
						for(CFGFunction function : matches) {
	
							// We only follow this function if we aren't already in this function on this simulated call stack.
							// Otherwise, we would recurse infinitely.
							if(!callstack.contains(function)) {
	
								// We only expand this call if the function isn't not a trusted function. No one wants to see the innards of trusted code.
								if(!function.isTrusted()) {
	
									Collection<CFGPath> functionPaths = function.getPathsWithExpandedOutputAffectingInvocations(callstack);
									
									assert functionPaths.size() > 0 : "Why does " + function.getDescriptionOfLocation() + " have 0 expanded paths?";

									// We found an invocation to expand! If there are X function paths from the statement above,
									// and Y current paths, we go through each of the Y current paths and each of the X function paths to
									// them, resulting in X * Y paths.
									if(invocationExpandedPaths.size() * functionPaths.size() > 1000) {
										Feedlack.out("Warning: combining " + 
											invocationExpandedPaths.size() + " existing paths with " + 
											functionPaths.size() + " paths through " + function.getDescriptionOfFunction() + " " + function.getDescriptionOfLocation() + " to create " + (invocationExpandedPaths.size() * functionPaths.size()) + " paths");
										throw new CFGFunction.TooManyPathsException();
									}
	
									Feedlack.out("Expanding " + (invocationExpandedPaths.size() * functionPaths.size()) + " for call to " + call.getFunctionCalled() + " at " + call.getDescriptionOfLocation());

									// If the set of expanded paths through a function is empty, then we don't bother duplicating the existing expanded paths. 
									if(functionPaths.size() > 0) {
									
										// Go through each of the existing expanded paths and append each possible function path to them.
										for(CFGPath expandedPath : invocationExpandedPaths) {
											
											// Make |functionPaths| copies of expandedPath and append each function path to one of them.
											for(CFGPath functionPath : functionPaths) {
		
												CFGPath copy = new CFGPath();
												try {
												
													copy.addPath(expandedPath);
													copy.addPath(functionPath);
													newExpandedPaths.add(copy);
		
												} 
												// If this existing path already has nodes on the function path in it, then stop.
												catch (CycleException e) {
													return invocationExpandedPaths;
												}
			
											}
				
										}
																			
									} 
									// If there weren't any function paths, then we should be adding a path that involves not following the function.
									else {

										Assumption ass = new Assumption("couldn't find paths through $1");
										ass.addLocation(function.getEnter());
										
										for(CFGPath expandedPath : invocationExpandedPaths) {
										
											CFGPath copy = new CFGPath();
											try {
												
												copy.addPath(expandedPath);
												copy.addEdge(new CFGEdge(node, function.getEnter(), "enter", ass));
												newExpandedPaths.add(copy);
	
											} 
											// If this existing path already has nodes on the function path in it, then stop.
											catch (CycleException e) {
												return invocationExpandedPaths;
											}
										
										}
										
									}
	
								} 
								// If the function is trusted, then we should be adding a path that involves following the trusted function.
								else {
									
									Assumption ass = new Assumption("trusted function");
									ass.addLocation(function.getEnter());
									
									for(CFGPath expandedPath : invocationExpandedPaths) {
									
										CFGPath copy = new CFGPath();
										try {
											
											copy.addPath(expandedPath);
											copy.addEdge(new CFGEdge(node, function.getEnter(), "enter", ass));
											newExpandedPaths.add(copy);

										} 
										// If this existing path already has nodes on the function path in it, then stop.
										catch (CycleException e) {
											return invocationExpandedPaths;
										}
									
									}									
									
								}
								
							}
							// If the call was recursive, then we should be adding at least one edge down the recursive path.
							else {
								
								Assumption ass = new Assumption("call is recursive");
								ass.addLocation(function.getEnter());
								
								for(CFGPath expandedPath : invocationExpandedPaths) {
								
									CFGPath copy = new CFGPath();
									try {
										
										copy.addPath(expandedPath);
										copy.addEdge(new CFGEdge(node, function.getEnter(), "enter", ass));
										newExpandedPaths.add(copy);

									} 
									// If this existing path already has nodes on the function path in it, then stop.
									catch (CycleException e) {
										return invocationExpandedPaths;
									}
								
								}									
								
							}
							
						} // End loop through matched functions

					}
					// If none of the functions were output affecting, we should note that none of them were followed.
					else {

						Assumption ass = new Assumption("");

						if(matches.size() > 1) {
							StringBuilder message = new StringBuilder("the functions this might call, which include ");
							int i = 1;
							for(CFGFunction function : matches) {
								ass.addLocation(function.getEnter());
								message.append((i == matches.size() ? " and " : "") + "$" + i + ", ");
								i++;
							}
							message.append("don't affect output");
							ass.setMessage(message.toString());
						}
						else
							ass.setMessage("this call doesn't affect output");
						
						edge.setAssumption(ass);

						// Add these to the new expanded paths list, so we don't lose them when we clear the list below.
						for(CFGPath expandedPath : invocationExpandedPaths)
							newExpandedPaths.add(expandedPath);

					}

					// By now, we should have handled all cases and there should be at least one path in the new paths.
					assert newExpandedPaths.size() > 0;
					
					invocationExpandedPaths.clear();
					invocationExpandedPaths.addAll(newExpandedPaths);

				} 
				// If we didn't find any matches, than don't worry about expanding the invocation, but we do update the edge's assumption.
				else {

					edge.setAssumption(new Assumption("no function by this name exists"));
										
				}
				
			} // End handling call node
					
		}
		
		assert invocationExpandedPaths.size() > 0 : "Something's wrong if we converted this path, which has " + length() + " edges, to " + invocationExpandedPaths.size() + " paths";
		
		return invocationExpandedPaths;
	
	}

	
	public Scenario getScenario(Set<InputHandler> calls, CFGFunction function, boolean showLackingPaths) {
		
		Scenario scenario = new Scenario(calls, function, showLackingPaths);
		
		List<CFGEdge> nonOutputAffectingCallsToSummarize = new Vector<CFGEdge>();
		
		// Return a path with only conditional nodes and invocations.
		for(CFGEdge edge : this) {
			
			Decision decisionToAdd;
			boolean summarizeCalls = true;
			
			// We always include a conditional in the explanation, since this is a key decision point.
			if(edge.getFrom() != null && edge.getFrom().isConditional() && edge.getAssumption() != Assumption.BLOCK_CANNOT_AFFECT_OUTPUT) {
				
				// Peek ahead and see what the decision is. We're not interested in skipped conditionals.
				decisionToAdd = new Decision(edge.getFrom(), edge.getLabel(), "expression", edge.getAssumption(), Likelihood.YES, false, false);

			}
			// We only include invocations if they they execute conditionals in this path as a result of being called.
			
			/*
			 * Here's the rule: if there are conditions between the call and return of a function, then the function's invocation is included, 
			 * otherwise it's not. Intuitively, we just expose the invocation contexts of the primary subject of the explanation, 
			 * which is the conditional behavior. But there's another case here: what if a function get's called in an expression and that 
			 * function has conditionals? Should it report those conditionals as part of the explanation? It would see not, since the explanation
			 * will already say "if the condition that called this function with the conditional is true", so explaining the call would be redundant. 
			 */
			
			// We keep track of function entry to help the user understand the control flow.
			// We implicitly skip over functions that don't produce output.
			// We don't do this for the first function since it's usually the one being analyzed.
			// If the node is output, include it in the path. (If the path is proof of output being produced, the UI will explain that, whereas
			// if the path is proof of output lacking, we'll explain why the output statement might not produce output).
			else if(edge.getTo().isNativeOutput()) {
				OutputExplanation explanation = edge.getTo().getOutputExplanation();
				decisionToAdd = new Decision(edge.getTo(), explanation.getName(), explanation.getReason(), edge.getAssumption(), explanation.getLikelihood(), true, false);
			}
			else if(edge.getTo() instanceof CFGEnterNode) {
				
				Assumption ass = edge.getAssumption();
				if(edge.getTo().getFunction().getReasonFunctionCanProduceOutput() != null) {
					ass = new Assumption("this function can produce output because $1 can produce output");
					ass.addLocation(edge.getTo().getFunction().getReasonFunctionCanProduceOutput());
				}
				
				decisionToAdd = new Decision(edge.getTo(), ((CFGEnterNode)edge.getTo()).getFunction().getFunctionName(), "enter", ass, Likelihood.YES, false, false);
			}
			else if(edge.getTo() instanceof CFGExitNode) {
				decisionToAdd = new Decision(edge.getTo(), ((CFGExitNode)edge.getTo()).getFunction().getFunctionName(), "exit", edge.getAssumption(), Likelihood.YES, false, false);
			}
			else if(edge.getLabel().equals("exception")) {
				decisionToAdd = new Decision(edge.getTo(), "exception", "exception is thrown", edge.getAssumption(), Likelihood.YES, false, false);
			}
			else if(edge.getLabel().equals("no exception")) {
				decisionToAdd = new Decision(edge.getTo(), "exception", "no exception is thrown", edge.getAssumption(), Likelihood.YES, false, false); 				
			}
			else if(edge.getTo() instanceof CFGCallNode) {
				
				// Only expand nodes that affect output.
				Stack<CFGFunction> callstack = new Stack<CFGFunction>();
				callstack.push(edge.getTo().getFunction());
				
				if(edge.getTo().canAffectOutput(edge.getTo().getFunction().getFeedlack(), callstack)) {
					
					Set<CFGFunction> functions = function.getFeedlack().resolveFunctionReference(edge.getTo());
					
					Assumption ass;
					if(functions.size() == 1) {

						ass = new Assumption("this calls $1, because no other functions by this name were found");
						ass.addLocation(functions.iterator().next().getEnter());
						
					}
					else if(functions.size() > 1) {
						ass = new Assumption("this calls one of ");
						String assumptionMessage = "";
						assumptionMessage = "this calls one of ";
						int i = 0;
						for(CFGFunction f : functions) {
							
							assumptionMessage = assumptionMessage + "$" + (i + 1) + ((i < functions.size() - 1) ? "," : "");
							ass.addLocation(f.getEnter());
							
							i++;
						}
						ass.setMessage(assumptionMessage);
					}
					else {
						ass = new Assumption("function does not exist, because no function by this name was found");
					}
				
					if(edge.getAssumption() != Assumption.NONE)
						ass = edge.getAssumption();
					
					decisionToAdd = new Decision(edge.getTo(), ((CFGCallNode)edge.getTo()).getFunctionCalled() + "()", "call", ass, Likelihood.YES, false, false);
					
				}
				else {
					
					nonOutputAffectingCallsToSummarize.add(edge);
					decisionToAdd = null;
					summarizeCalls = false;
					
				}
					
			}
			else if(edge.getTo() instanceof CFGCaseNode) {
				decisionToAdd = new Decision(edge.getTo(), edge.getTo().getCodeString(), "case", edge.getAssumption(), Likelihood.YES, false, false);
			}
			else {
				
				decisionToAdd = null;
				summarizeCalls = false;
				
			}
		
			// If there are non-output affecting calls to add and we didn't just add one, add them.
			if(summarizeCalls && nonOutputAffectingCallsToSummarize.size() > 0) {
				
				CFGNode to = null;
				Assumption assumption = new Assumption("");
				StringBuilder message = new StringBuilder();
				int i = 1;
				for(CFGEdge call : nonOutputAffectingCallsToSummarize) {
					
					// Use the first call in the sequence as the one for the decision.
					if(to == null) to = call.getTo();
					
					Collection<CFGFunction> funs = function.getFeedlack().resolveFunctionReference(call.getTo());
					
					String aside = funs == null || funs.isEmpty() ? " (not found)" : funs.size() > 1 ? "(" + funs.size() + " matches)" : "";
					CFGNode singleMatch = funs != null && funs.size() == 1 ? funs.iterator().next().getEnter() : null;
					
					message.append("$" + i + aside + (i < nonOutputAffectingCallsToSummarize.size() ? ", " : ""));

					assumption.addLocation(singleMatch != null ? singleMatch : call.getTo());
					i++;
					
				}
				message.append(" " + (nonOutputAffectingCallsToSummarize.size() == 1 ? "does" : "do") + " not affect output");
				assumption.setMessage(message.toString());
				
				// Note that we mark it as a call sequence so the UI formats it properly.
				Decision aggregateDecision = new Decision(to, "several functions are called that ", "do not affect output", assumption, Likelihood.YES, false, true);
				
				scenario.addDecision(aggregateDecision);
				
				nonOutputAffectingCallsToSummarize.clear();
				
			}
			
			// Then, if there's a decision to 
			if(decisionToAdd != null)
				scenario.addDecision(decisionToAdd);
			
		}
		
		return scenario;
		
	}

	/**
	 * Determines whether the given edge should be included in paths generated by various Feedlack analysis routines.
	 */
	private static boolean isEdgeOfInterest(CFGEdge edge) {

		CFGNode node = edge.getTo();
		
		if(node.isConditional())
			return true;
		else if(node.isNativeOutput())
			return true;
		else if(node instanceof CFGCallNode)
			return true;
		else if(node instanceof CFGEnterNode)
			return true;
		else if(node instanceof CFGExitNode)
			return true;
		else if(edge.getLabel().equals("exception"))
			return true;		
		else if(edge.getLabel().equals("no exception"))
			return true;
		else if(node instanceof CFGCaseNode)
			return true;		

		return false;
		
	}
	
	public static CFGPath deriveTrimmedPath(CFGPath path) {
		
		CFGPath trimmed = new CFGPath();
		
		boolean previousWasConditional = false;
		for(CFGEdge edge : path) {
			
			if(isEdgeOfInterest(edge) || previousWasConditional) {
				previousWasConditional = false;
				try {
					trimmed.addEdge(edge);
				} catch (CycleException e) {}
			}
				
			if(edge.getTo().isConditional())
				previousWasConditional = true;
			
		}
		
		return trimmed;
		
	}

}