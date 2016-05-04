package edu.uw.feedlack;

import java.util.HashSet;
import java.util.Set;

public class JavaScriptInputHandler extends InputHandler {

	private final String event;
	private final CFGNode node;
	private final Set<CFGFunction> functions;

	public JavaScriptInputHandler(String event, CFGNode node, Set<CFGFunction> functions) {

		this.event = event;
		this.node = node;
		this.functions = functions;
		
	}

	public String getDescription() {
		
		return "on " + getEventDescription() + " (" + node.toLineNumberString() + ")";
		
	}

	@Override
	public String getEventDescription() {
		
		return event;
		
	}

	@Override
	public Set<CFGFunction> getFunctions() { return functions == null ? new HashSet<CFGFunction>() : functions; }	

	@Override
	public String getPath() {

		return node.getFunction().getFunctionNode().getSourceName();

	}
	
	public int getLine(Feedlack feedlack) {
		
		return feedlack.getLineNumberOfNode(node);
		
	}

	@Override
	public boolean equals(Object o) {
		
		return o instanceof JavaScriptInputHandler && this.node == ((JavaScriptInputHandler)o).node;

	}

	@Override
	public int hashCode() {
	
		return node.hashCode();
		
	}

}