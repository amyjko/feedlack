package edu.uw.feedlack;

public class TypeInfo {
	
	private final CFGNode origin;
	private final JavaScriptType type;
	private final JavaScriptType container;
	private final String property;

	public TypeInfo(CFGNode origin, JavaScriptType name, JavaScriptType container, String property) {
		
		this.origin = origin;
		this.type = name;		
		this.container = container;
		this.property = property;
		
	}
	
	public TypeInfo(CFGNode origin, JavaScriptType name) {
		
		this.origin = origin;
		this.type = name;		
		this.container = null;
		this.property = null;
		
	}

	public CFGNode getOrigin() { return origin; }
	public JavaScriptType getType() { return type; }
	public JavaScriptType getContainer() { return container; }
	public String getProperty() { return property; }
	
	public String toString() { return type.toString(); }
	
}
