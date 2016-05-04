package edu.uw.feedlack;


import java.util.Set;
import java.util.Stack;

import com.google.javascript.rhino.Node;

public class CFGGetPropertyNode extends CFGNode {

	private CFGNode object, property;
	
	public CFGGetPropertyNode(Node node, CFGFunction function) {
		super(node, function);
	}

	public TypeInfo determineType() {
		
		TypeInfo objectType = object.getTypeInfo();
		String propertyName = property.getCodeString();
		
		// If we have no type information about the object and the property has the name of a standardized Element member, assume it's an element.
		if(objectType.getType() == JavaScriptDictionary.NONE && JavaScriptDictionary.ELEMENT.hasMemberNamed(propertyName))
			return new TypeInfo(this, JavaScriptDictionary.ELEMENT.getTypeOfProperty(propertyName), JavaScriptDictionary.ELEMENT, property.getCodeString());
		else if(objectType.getType() == JavaScriptDictionary.NONE && JavaScriptDictionary.JQUERY.hasMemberNamed(propertyName))
			return new TypeInfo(this, JavaScriptDictionary.JQUERY.getTypeOfProperty(propertyName), JavaScriptDictionary.JQUERY, property.getCodeString());
		
		TypeInfo propertyType = new TypeInfo(this, objectType.getType().getTypeOfProperty(propertyName), objectType.getType(), property.getCodeString());
		
		return propertyType;
		
	}

	protected void markOutputAffectingDataDependencies(Stack<CFGNode> stack) {

//		Feedlack.out("Feedlack figured out that this property " + this + " can affect output!");

		// Need a global list of assignments to this property to propagate to.
		Set<CFGAssignNode> assignments = getFunction().getFeedlack().getAssignmentsToPropertyName(getPropertyName());
		
		// Mark all of the potential assignments as output affecting
		if(assignments != null) {
			for(CFGAssignNode assignment : assignments) {
//				Feedlack.out("Marking value of assignment " + assignment + " as output affecting");
				assignment.getValue().markOutputAffectingDataDependenciesHelper(stack);
			}
		}
		else {
		}
		
	}

	@Override
	/*
	 * Block node points to it's first child.
	 */
	protected void constructEdges() {

		this.object = getCFGNodeFor(node.getChildAtIndex(0));
		addEdge(object, "object", Assumption.NONE);
		this.property = getCFGNodeFor(node.getChildAtIndex(1));
		object.addEdgeToLeaves(property, "property", Assumption.NOT_UNDEFINED);
				
	}

	public CFGNode getPropertyNode() { return property; }
	
	public String getPropertyName() {
		
		if(property instanceof CFGGetPropertyNode)
			return ((CFGGetPropertyNode)property).getPropertyName();
		else if(property instanceof CFGNameNode)
			return ((CFGNameNode)property).getName();
		else if(property instanceof CFGBasicNode)
			return ((CFGBasicNode)property).getCodeString();
		return "";
		
	}

	@Override
	public JavaScriptInputHandler getInputHandler(Feedlack feedlack) {
		
		return null;
	
	}
	
	public String toString() { return getCodeString(); }
	
	public String getCodeString() {
		
		return object.getCodeString() + "." + property.getCodeString();
		
	}

}