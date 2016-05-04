package edu.uw.feedlack;

import java.util.Set;


/**
 * @author Andrew J. Ko
 *
 * Represents a place in the code where user input is handled. At least at the time of its creation, 
 * this include HTML attributes representing events and JavaScript event registration mechanisms such as
 * addEventHandler().
 */
public abstract class InputHandler {

	public abstract String getDescription();
	public abstract Set<CFGFunction> getFunctions();
	public abstract String getEventDescription();
	public abstract String getPath();
	public abstract int getLine(Feedlack feedlack);

	public abstract boolean equals(Object o);
	public abstract int hashCode();
	
}
