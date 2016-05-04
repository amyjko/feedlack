package edu.uw.feedlack;

import java.io.File;
import java.util.Set;

import org.htmlparser.Attribute;
import org.htmlparser.Tag;

public class HTMLInputHandler extends InputHandler {

	private File file;
	private Tag tag;
	private Attribute attribute;
	private Set<CFGFunction> functions;
	private final int lineNumber;

	public HTMLInputHandler(Attribute attr, Tag tag, int lineNumber, File file, Set<CFGFunction> functions) {

		this.file = file;
		this.tag = tag;
		this.attribute = attr;
		this.functions = functions;
		this.lineNumber = lineNumber + 1;
		
	}

	public String getDescription() {
		
		return "on " + getEventDescription() + " (" + file.getName() + ":" + (tag.getStartingLineNumber() + 1) + ")";
		
	}

	
	@Override
	public Set<CFGFunction> getFunctions() {
		
		return functions;
		
	}

	@Override
	public String getPath() {

		return file.getAbsolutePath();

	}
	
	public int getLine(Feedlack feedlack) {
		
		return lineNumber;
		
	}

	@Override
	public String getEventDescription() {
		
		String event = attribute.getName();
		if(event.startsWith("on")) event = event.substring(2);
		else if(event.equals("href")) event = "link click";
		return event;
		
	}

	@Override
	public boolean equals(Object o) {
		
		return o instanceof HTMLInputHandler && this.tag == ((HTMLInputHandler)o).tag;

	}

	@Override
	public int hashCode() {
	
		return tag.hashCode();
		
	}

	public Tag getTag() { return tag; }	
	
}