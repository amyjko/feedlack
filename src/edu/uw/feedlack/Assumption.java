package edu.uw.feedlack;

import java.util.List;
import java.util.Vector;

public final class Assumption {

	public static final Assumption NONE = new Assumption("nothing");
	public static final Assumption WHILE_IS_TRUE_ONCE = new Assumption("condition is true exactly once");
	public static final Assumption FOR_IS_TRUE_ONCE = new Assumption("condition is true exactly once");
	public static final Assumption NOT_UNDEFINED = new Assumption("expression is not undefined");
	public static final Assumption FUNCTION_EXISTS = new Assumption("function exists");
	public static final Assumption PRODUCES_OUTPUT = new Assumption("produces output");
	public static final Assumption CONDITION_CAN_BE_TRUE = new Assumption("condition can be true");
	public static final Assumption CONDITION_CAN_BE_FALSE = new Assumption("condition can be false");
	public static final Assumption BLOCK_CANNOT_AFFECT_OUTPUT = new Assumption("block cannot affect output");
	public static final Assumption NON_ZERO = new Assumption("denominator is non-zero");
	public static final Assumption DIVIDE_BY_ZERO = new Assumption("denominator can be zero");
	
	private String message;
	private final List<CFGNode> locations = new Vector<CFGNode>();
	
	public Assumption(String message) {
		
		this.message = message;
		
	}
	
	public void addLocation(CFGNode node) {
		
		locations.add(node);
		
	}

	public String getMessage() {
		
		return message;

	}

	public List<CFGNode> getLocations() {
		
		return locations;
		
	}

	public void setMessage(String assumptionMessage) {
		
		this.message = assumptionMessage;

		
	}
	
}

