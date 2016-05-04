/**
 * 
 */
package edu.uw.feedlack;

import java.util.*;

import com.google.javascript.rhino.FunctionNode;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.ScriptOrFnNode;
import com.google.javascript.rhino.Token;

import edu.uw.feedlack.CFGIfNode.CFGEndIfNode;
import edu.uw.feedlack.CFGPath.CycleException;

public class CFGFunction {
	
	
	/**
	 * The Rhino node this class represents.
	 */
	private final ScriptOrFnNode functionNode;
	
	/**
	 * A reference to the Feedlack instance this applies to.
	 */
	private final Feedlack feedlack;
	
	/**
	 * The name of the function, determined from AST context.
	 */
	private final String name;
	
	private CFGEnterNode enter;
	private CFGExitNode exit;
	private Set<CFGNode> visited = new HashSet<CFGNode>();
	private Set<CFGNode> children = new HashSet<CFGNode>();
	private Collection<CFGPath> paths;
	
	private String[] parameters;
	private int parameterCount = 0;

	private final Collection<JavaScriptInputHandler> handlers = new Vector<JavaScriptInputHandler>();
	private final Collection<CFGAssignNode> assignments = new Vector<CFGAssignNode>();
	private final Collection<CFGReturnNode> returns = new Vector<CFGReturnNode>();
	private final Collection<CFGVarNode> vars = new Vector<CFGVarNode>();
	private final Collection<CFGCallNode> calls = new Vector<CFGCallNode>();
	private boolean isTrusted = false; 
	
	public Collection<CFGReturnNode> getReturns() { return returns; }
	public Collection<CFGAssignNode> getAssignments() { return assignments; }
	public Collection<CFGVarNode> getVars() { return vars; }
	public Collection<CFGCallNode> getCalls() { return calls; }
	
	private CFGNode reasonFunctionCanProduceOutput = null; 
	private boolean canProduceOutput = false, checkedIfCanProduceOutput = false;
	
	private final boolean isScript;
	private final Node parent;
	
	public enum Scope {
		GLOBAL,		// Could be called from anywhere
		CALLBACK,	// Any function that escapes a local or object context.
		LOCAL,		// Named functions declared and used locally, but not sent elsewhere.
		OBJECT		// Named functions declared on an object.
	}
	
	private final Scope scope;
	
	public int getParameterCount() { return parameterCount; }
	
	public CFGFunction(Feedlack feedlack, ScriptOrFnNode function, Node astParent, boolean isScript) {
		
		this.feedlack = feedlack;
		this.functionNode = function;
		this.isScript = isScript;
		this.parent = astParent;
		
		
		// Figure out this function's name. First see if the function is a function node (because it might be a script.
		String name = null;
		if(function instanceof FunctionNode) {

			name = ((FunctionNode)function).getFunctionName();
			
			// How many arguments can be sent to this?
			for(Node child : function.children()) {
				
				if(child.getType() == Token.LP) {
					
					parameterCount = 0;
					parameters = new String[child.getChildCount()];
					for(Node param : child.children()) {
						parameters[parameterCount] = param.getString(); 
						parameterCount++;
					}
					
				}
				
			}
			
		}
		
		// Deal with the parent and find a name. Functions can be all over the place! Where is this function declared?
		//
		// RETURN - Probably anonymous, for the purpose of a call back.
		// CALL - Probably anonymous, for the purpose of a call back.
		// ASSIGN - Named, but could be assigned to a local variable or a property, which affects who can call it.
		// BLOCK - Named, and totally local, unless passed elsewhere.
		// OBJECTLIT - Named, and probably only called on objects of this type.
		// GETELEM- Probably an object function.
		// NAME - Probably a local.
		if(name == null || name.equals("")) {

			// Is this in an object literal? Use the property prior to it to guess.
			if(parent != null) {
			
				if(parent.getType() == Token.OBJECTLIT) {
				
					// What property name was this function assigned to?
					Node property = parent.getChildBefore(functionNode);
					if(property != null && property.getType() == Token.STRING)
						name = property.getString();
					
					scope = Scope.OBJECT;
					
				}
				// Is this an assignment?
				else if(parent.getType() == Token.ASSIGN) {

					// Was this assigned to a property or a local?
					Node assignee = parent.getChildAtIndex(0);
					if(assignee.getType() == Token.GETPROP) {

						Node property = assignee.getLastChild();
						if(property != null && property.getType() == Token.STRING) {
							name = property.getString();							
						}
						
						scope = Scope.OBJECT;
							
					}
					else if(assignee.getType() == Token.NAME) {
						name = assignee.getString();
						scope = Scope.LOCAL;
					}
					else if(assignee.getType() == Token.GETELEM) {
						name = "callback";
						scope = Scope.OBJECT;
					}
					else {
						scope = Scope.GLOBAL;
					}
					
				}
				else if(parent.getType() == Token.CALL || parent.getType() == Token.RETURN) {
					
					scope = Scope.CALLBACK;
					name = "callback";
					
				}
				else {
					if(parent.getType() == Token.NAME)
						name = parent.getString();
					scope = Scope.GLOBAL;
				}
				
			}
			// If this is a script, just call it script
			else if(isScript) {
	
				name = "script";
				scope = Scope.GLOBAL;
				
			}
			else
				scope = Scope.GLOBAL;
			
		}
		else {
			if(parent != null) {
			
				if(parent.getType() == Token.BLOCK) {
					scope = Scope.LOCAL;
				}
				else {
					scope = Scope.GLOBAL;
				}
				
			}
			else {
				scope = Scope.GLOBAL;
			}
		}
		
		// Hope we found a name!
		this.name = name;		
		
		enter = new CFGEnterNode(functionNode, this);
		exit = new CFGExitNode(functionNode, this);
	
		enter.computeGraph();
		enter.addEdgeToLeaves(exit, "exit", Assumption.NONE);
		
	}
	
	protected void guessTypes(Hashtable<String,GetPropertyInfo> localTypeInfo) {

		final int NUMBER_OF_PROPERTY_NAMES_REQUIRED_FOR_MATCH = 2;
		
		for(String variableName : localTypeInfo.keySet()) {
			
			GetPropertyInfo info = localTypeInfo.get(variableName);
			if(info.objectNames.size() > 0) {
			
//				Feedlack.out("Couldn't find a type for " + variableName + " " + info.objectNames.get(0).getDescriptionOfLocation() + ", which accesses " + info.propertyNames);
				
				// Go through all of the primitives and see if any of them appear to have all of these functions.
				JavaScriptType match = null;
				int matchSize = 0;
				for(JavaScriptType type : JavaScriptDictionary.PRIMITIVES.values()) {

					int currentMatchSize = computeMatchSize(type, info.propertyNames);					
					if(currentMatchSize >= NUMBER_OF_PROPERTY_NAMES_REQUIRED_FOR_MATCH && currentMatchSize > matchSize) {
						match = type;
						matchSize = currentMatchSize;
					}
					
				}

				// Go through all of the user defined types and see if they match.
				for(JavaScriptType type : feedlack.getObjectLiteralTypes()) {
					
					int currentMatchSize = computeMatchSize(type, info.propertyNames);					
					// We don't worry about matching all of the names, we just worry about matching more than one.
					if(currentMatchSize >= NUMBER_OF_PROPERTY_NAMES_REQUIRED_FOR_MATCH && currentMatchSize > matchSize) {
						match = type;
						matchSize = currentMatchSize;
					}
					
				}

				if(match != null) {

//					Feedlack.out("\tGuessing that it's a " + match.getName());
					
					for(CFGNameNode node : info.objectNames)
						node.setTypeInfo(new TypeInfo(node, match));
					
				}
				else {
//					Feedlack.out("\tCouldn't find a match.");
				}
								
			}
			
		}

	}
	
	private int computeMatchSize(JavaScriptType type, Set<String> names) {
		
		int currentMatchSize = 0;
		for(String name : names) {
			
			if(type.hasMemberNamed(name))
				currentMatchSize++;
			
		}
		return currentMatchSize;

	}
	
	public int getStatementCount() { return getStatementCount(functionNode, 0); }
	
	private int getStatementCount(Node current, int count) {
		
		if(current.getType() == Token.BLOCK) {
			
			count += current.getChildCount();
			
		}
		for(Node node : current.children()) {
			count = getStatementCount(node, count);
		}
		return count;
		
	}

	public CFGFunction getParentFunction() {
		
		if(parent != null) {
			
			CFGNode parentNode = feedlack.getCFGNode(parent);
			if(parentNode != null) {

				return parentNode.getFunction();
				
			}
			
		}
		return null;
		
	}
	
	public Scope getScope() { return scope; }
	
	public Feedlack getFeedlack() { return feedlack; }
	
	public CFGNode getCFGNodeFor(Node node) {
		
		if(feedlack.getCFGNode(node) != null) {
			CFGNode result = feedlack.getCFGNode(node);
			// Add it in case it hasn't been added yet. This is because we may have multiple instances of functions (haven't debugged this yet).
			children.add(result);
			return result;
		}
		
		final CFGNode newNode;
		switch(node.getType()) {
		
		case Token.CALL: 
			newNode = new CFGCallNode(node, this);
			calls.add((CFGCallNode) newNode);
			break;
		case Token.IF: newNode = new CFGIfNode(node, this); break;
		case Token.BLOCK: newNode = new CFGBlockNode(node, this); break;
		case Token.WITH: newNode = new CFGBlockNode(node, this); break;
		case Token.FUNCTION: newNode = new CFGFunctionNode(node, this); break;
		case Token.EXPR_VOID: newNode = new CFGExpressionVoidNode(node, this); break;
		case Token.EXPR_RESULT: newNode = new CFGExpressionVoidNode(node, this); break;
		case Token.VAR: 
			
			newNode = new CFGVarNode(node, this); 
			vars.add((CFGVarNode)newNode);
			break;

		case Token.ASSIGN:
		case Token.ASSIGN_BITOR:
		case Token.ASSIGN_BITXOR:
		case Token.ASSIGN_BITAND:
		case Token.ASSIGN_LSH:
		case Token.ASSIGN_RSH:
		case Token.ASSIGN_URSH:
		case Token.ASSIGN_ADD:
		case Token.ASSIGN_SUB:
		case Token.ASSIGN_MUL:
		case Token.ASSIGN_DIV:
		case Token.ASSIGN_MOD:
			newNode = new CFGAssignNode(node, this); 
			assignments.add((CFGAssignNode) newNode);
			break;
		case Token.NUMBER:
		case Token.STRING:
		case Token.TRUE:
		case Token.FALSE:
		case Token.THIS:
		case Token.NULL:
			newNode = new CFGBasicNode(node, this); 
			break;
		case Token.GETPROP: 
		case Token.GETELEM: 
			newNode = new CFGGetPropertyNode(node, this); 
			break;
		case Token.DIV:
			newNode = new CFGDivideNode(node, this);
			break;
		case Token.OR:
		case Token.AND:
		case Token.EQ:
		case Token.NE:
		case Token.LT:
		case Token.LE:
		case Token.GT:
		case Token.GE:
		case Token.LSH:
		case Token.RSH:
		case Token.URSH:
		case Token.ADD:
		case Token.SUB:
		case Token.MUL:
		case Token.MOD:
		case Token.SHEQ:
		case Token.SHNE:
		case Token.IFEQ:
		case Token.IFNE:
		case Token.BITAND:
		case Token.BITXOR:
		case Token.INSTANCEOF:
		case Token.IN:
		case Token.BITOR:
			newNode = new CFGBinaryOperatorNode(node, this);
			break;
		case Token.NOT:
		case Token.NEG:
		case Token.TYPEOF:
		case Token.POS:	// Array index?
		case Token.BITNOT:
			newNode = new CFGUnaryOperatorNode(node, this);
			break;
		case Token.NAME:
			if(node.getParent().getType() == Token.CALL && node.getParent().getChildAtIndex(0) == node) {
				newNode = new CFGInvocationNode(node, this);
			}
			else newNode = new CFGNameNode(node, this);
			break;
		case Token.RETURN:
			newNode = new CFGReturnNode(node, this);
			returns.add((CFGReturnNode) newNode);
			break;
		case Token.HOOK:
			newNode = new CFGHookNode(node, this);
			break;
		case Token.NEW:
			newNode = new CFGNewNode(node, this);
			break;
		case Token.OBJECTLIT:
			newNode = new CFGObjectLiteralNode(node, this);
			break;
		case Token.ARRAYLIT:
			newNode = new CFGArrayLiteralNode(node, this);
			break;
		case Token.WHILE:
			newNode = new CFGWhileNode(node, this);
			break;
		case Token.DO:
			newNode = new CFGDoWhileNode(node, this);
			break;
		case Token.FOR:
			newNode = new CFGForNode(node, this);
			break;
		case Token.DEC:
		case Token.INC:
			newNode = new CFGBasicNode(node, this);
			break;
		case Token.LP:
		case Token.RP:
		case Token.EMPTY:
		case Token.DELPROP:
		case Token.COMMA:
		case Token.VOID:
		case Token.REGEXP:
			newNode = new CFGBasicNode(node, this);
			break;
		case Token.SWITCH:

			newNode = new CFGSwitchNode(node, this);
			break;

		case Token.CASE:
		case Token.DEFAULT:

			newNode = new CFGCaseNode(node, this);
			break;

		case Token.TRY:
			
			newNode = new CFGTryNode(node, this);
			break;

		case Token.CATCH:
		case Token.FINALLY:
			
			newNode = new CFGCatchNode(node, this);
			break;

		// TODO Control flow nodes to handle
		case Token.THROW:
		case Token.CONTINUE:
		case Token.BREAK:
		case Token.LABEL:
			newNode = new CFGBasicNode(node, this);
			break;
		default: 
			Feedlack.err("Didn't explicitly handle AST node " + node);
			newNode = new CFGBasicNode(node, this);
		}
		
		assert node != null;
		assert newNode != null;

		feedlack.putCFGNode(node, newNode);
		children.add(newNode);
		
		// Now that we've created the node, construct it's edges. Before attaching anything to its leaves, remember it's leaves.
		newNode.computeGraph();
	
//		FunctionNode fn = null;
//		for(Node n : newNode.getASTNode().getAncestors()) {
//			if(n instanceof FunctionNode) {
//				fn = (FunctionNode) n;
//				break;
//			}
//		}
//		if(fn != null)
//			assert newNode.getFunction().getFunctionNode() == fn;
		
		return newNode;
		
	}
	
	public void markTrusted() {
		
		this.isTrusted = true;
		
	}

	public boolean isTrusted() { 
		
		return this.isTrusted; 
		
	}
	
	/**
	 * Checks all calls and assignments to find input handlers.
	 */
	public Collection<JavaScriptInputHandler> getInputHandlers() {
		
		handlers.clear();
		
		for(CFGAssignNode assignment : assignments) {
			JavaScriptInputHandler handler = assignment.getInputHandler(feedlack);
			if(handler != null)
				handlers.add(handler);
		}

		for(CFGCallNode call : calls) {
			JavaScriptInputHandler handler = call.getInputHandler(feedlack);
			if(handler != null)
				handlers.add(handler);
		}
		
		return handlers;

	}
	
	public String getFunctionName() { return name; }

	public String getDescriptionOfLocation() { 
		
		return ((new java.io.File(functionNode.getSourceName())).getName()) + ":" + feedlack.getLineNumberOfFunction(this); 
		
	} 
	
	public String getDescriptionOfFunction() {
		
		StringBuilder sb = new StringBuilder();
		
		if(name == null || name.equals("")) sb.append("function");
		else sb.append(name);
		sb.append("(");
		if(parameters != null) {
			for(int i = 0; i < parameters.length; i++) {
				sb.append(parameters[i]);
				sb.append(i < parameters.length - 1 ? ", " : "");
			}
		}
		sb.append(")");
		return sb.toString();
		
	}
	
	public String toString() {
		
		return getDescriptionOfFunction() + ":" + getDescriptionOfLocation();
		
	}

	public CFGExitNode getExit() { return exit; }
	public CFGEnterNode getEnter() { return enter; }

	public ScriptOrFnNode getFunctionNode() { return functionNode; }

	public Collection<CFGPath> getOutputAffectingPaths() { 
		
		if(paths == null) computeOutputAffectingPaths();
		return paths;
		
	}
	
	public static class TooManyPathsException extends Exception {
		
		public TooManyPathsException() {
			
			super("There were too many paths to compute, so I stopped");
			
		}
		
	}
	
	public Collection<CFGPath> getPathsWithExpandedOutputAffectingInvocations() throws TooManyPathsException {
		
		return getPathsWithExpandedOutputAffectingInvocations(new Stack<CFGFunction>());
		
	}
		
	private Collection<CFGPath> expandedPaths = null;
	
	protected Collection<CFGPath> getPathsWithExpandedOutputAffectingInvocations(Stack<CFGFunction> callstack) throws TooManyPathsException {
		
		// If the stack contains this, then the function may be recursive. In this case, we return an empty set
		// to avoid recursion.
		if(callstack.contains(this)) {
			
			return new Vector<CFGPath>();
			
		}
		
		// If we've already cached the paths, return them.
		if(expandedPaths == null) { 
	
			// Add this function to the call stack to avoid recursive cycles.
			callstack.push(this);
			
			// Get all of the paths through this function.
			Collection<CFGPath> outputAffectingPaths = getOutputAffectingPaths();
			
			Vector<CFGPath> tempExpandedPaths = new Vector<CFGPath>();
	
			// For each path potentially lacking output, expand the invocations to include their paths lacking output
			for(CFGPath path : outputAffectingPaths) {
				Collection<CFGPath> pathsExpandedPaths = path.getPathsWithExpandedOutputProducingInvocations(feedlack, callstack);
				assert pathsExpandedPaths.size() > 0;
				tempExpandedPaths.addAll(pathsExpandedPaths);
			}
	
			callstack.pop();
			
			assert tempExpandedPaths.size() > 0;
	
			expandedPaths = tempExpandedPaths;
			
		}
		
		return expandedPaths;
		
	}
	
	public boolean canAffectOutput(Feedlack feedlack) {
		
		Stack<CFGFunction> callstack = new Stack<CFGFunction>();
		callstack.push(this);
		return canAffectOutput(feedlack, callstack);
		
	}
	
	protected boolean canAffectOutput(Feedlack feedlack, Stack<CFGFunction> callstack) {
		
		return computeCanAffectOutput(feedlack, callstack);
		
	}
	
	private boolean computeCanAffectOutput(Feedlack feedlack, Stack<CFGFunction> callstack) {

		if(!checkedIfCanProduceOutput) {
			
			checkedIfCanProduceOutput = true;
			canProduceOutput = false;
			
			// Go through all of the nodes in this function (the one's Feedlack is interested in anyway)
			// and find out if they produce output.
			for(CFGNode node : children) {
				if(node.canAffectOutput(feedlack, callstack)) {
					
					reasonFunctionCanProduceOutput = node;

					canProduceOutput = true;
					break;
				}
			}

		}
		
		return canProduceOutput;
		
	}
	
	private boolean computedAlwaysAffectsOutput = false;
	private boolean alwaysAffectsOutput = false;
	
	// Returns whether all paths in this function produce output.
	public boolean alwaysAffectsOutput(Feedlack feedlack, Stack<CFGFunction> callstack) {		
		
		// If we've already computed this, then return the answer we got.
		if(computedAlwaysAffectsOutput) return alwaysAffectsOutput;

		// Remember that we computed it before we enter any potentially recursive loops.
		computedAlwaysAffectsOutput = true;

		// Add this function to the stack.
		callstack.push(this);
		
		// innocent until proven guilty
		alwaysAffectsOutput = true;
		boolean sometimesAffectsOutput = false;

		// Get all of the paths through this function
		Collection<CFGPath> paths = getOutputAffectingPaths();
		
		// If we haven't computed them yet (likely meaning this function is recursive?), then return now.
		if(paths == null) { return false; }
		
		// Go through all of the paths and see if they produce output.
		for(CFGPath path : paths) {

			if(path.alwaysProducesOutput(feedlack, callstack)) {
				sometimesAffectsOutput = true;
			}
			else {
				alwaysAffectsOutput = false;
			}
			
			
		}
		
		callstack.pop();
		
		// If this function can produce output and it's trusted, then assume it *always* produce output.
		// This way, we reduce false positives about trusted code.
		if(sometimesAffectsOutput && isTrusted) {
			alwaysAffectsOutput = true;
		}
		
		return alwaysAffectsOutput;
		
	}
	
	public CFGNode getReasonFunctionCanProduceOutput() { return reasonFunctionCanProduceOutput; }
	
	public boolean nodeOrChildrenCanProduceOutput(CFGNode node, Stack<CFGFunction> callstack) {

		if(node.canAffectOutput(feedlack, callstack)) return true;
		for(Node child : node.getASTNode().children()) {

			if(!(child instanceof ScriptOrFnNode)) {
				CFGNode cfgChild = getCFGNodeFor(child);
				if(nodeOrChildrenCanProduceOutput(cfgChild, callstack)) {
					return true;
				}
			}
			
		}
		
		return false;
		
	}

	public static class GetPropertyInfo {
		TypeInfo type = null;
		List<CFGNameNode> objectNames = new Vector<CFGNameNode>();
		Set<String> propertyNames = new HashSet<String>();			
	}

	private boolean computedLocalTypeInformation = false;
	
	public void computeLocalTypeInformation() {
		
		if(computedLocalTypeInformation)
			return;
		
		computedLocalTypeInformation = true;

		// This table will store type information about local variables.
		Hashtable<String,GetPropertyInfo> localTypeInfo = new Hashtable<String,GetPropertyInfo>();
		
		// This walks through the function and propagates the type info.
		computeLocalDefinitionsAndUses(functionNode, localTypeInfo);
		
		// This handles any names for which types weren't found, guessing their types.
		guessTypes(localTypeInfo);
				
	}
	
	private void computeLocalDefinitionsAndUses(Node parent, Map<String, GetPropertyInfo> mostRecentTypeInfoForName) {
		
		for(Node child : parent.children()) {

			// TODO We don't go into functions, because they have their own scope (but we should, right? Because they share scope?)
			if(!(child instanceof FunctionNode))
				computeLocalDefinitionsAndUses(child, mostRecentTypeInfoForName);
			
			CFGNode node = getCFGNodeFor(child);

			if(node instanceof CFGVarNode) {
				
				List<CFGNode> locals = ((CFGVarNode)node).getLocals();
				for(CFGNode local : locals) {
				
					String name = local.getASTNode().getString();

					if(local.getASTNode().getFirstChild() != null) {
						CFGNode value = getCFGNodeFor(local.getASTNode().getFirstChild());
						JavaScriptType type = value.getTypeInfo().getType();
						TypeInfo info = new TypeInfo(node, type);
						
						GetPropertyInfo typeInfo = mostRecentTypeInfoForName.get(name);
						if(typeInfo == null) {
							typeInfo = new GetPropertyInfo();
							mostRecentTypeInfoForName.put(name, typeInfo);
						}
						typeInfo.type = info;

					}

				}
				
			}
			else if(node instanceof CFGAssignNode && ((CFGAssignNode)node).getAssignee() instanceof CFGNameNode) {
				
				String name = ((CFGNameNode)((CFGAssignNode)node).getAssignee()).getName();
				CFGNode value = ((CFGAssignNode)node).getValue();
				JavaScriptType type = value.getTypeInfo().getType();
				TypeInfo info = new TypeInfo(node, type);
				
				GetPropertyInfo typeInfo = mostRecentTypeInfoForName.get(name);
				if(typeInfo == null) {
					typeInfo = new GetPropertyInfo();
					mostRecentTypeInfoForName.put(name, typeInfo);
				}
				typeInfo.type = info;
				
			}
			else if(node instanceof CFGNameNode) {

				String name = ((CFGNameNode)node).getName();					
				
				// We don't override the type information of a name that represents the function to call, because that should point to the return value of the function.
				if(child.getParent().getType() != Token.CALL || child != node.getASTNode().getParent().getFirstChild()) {
						
					TypeInfo info = mostRecentTypeInfoForName.containsKey(name) ? mostRecentTypeInfoForName.get(name).type : null;
					
					((CFGNameNode)node).setTypeInfo(info);
					
				}
				
				// If we didn't find any type information and it's a parameter, see if we can find the parameters type information.
				if(((CFGNameNode)node).getTypeInfo().getType() == JavaScriptDictionary.NONE && indexOfParameterNamed(name) >= 0) {
					
					List<CFGCallNode> calls = feedlack.findCallsToFunction(this);
					if(calls.isEmpty()) {
						
//						Feedlack.out("Couldn't find calls to " + this);
					}
					else {
						for(CFGCallNode call : calls) {
							
							CFGNode arg = call.getArgument(indexOfParameterNamed(name));
							if(arg != null) {
	
								// Before getting the type information of the argument, make sure the function's type information is propagated.
								arg.getFunction().computeLocalTypeInformation();
								
								// What's the argument's type?
								TypeInfo type = arg.getTypeInfo();
	
								if(type.getType() != JavaScriptDictionary.NONE) {
									((CFGNameNode)node).setTypeInfo(type);
//									Feedlack.out("" + toString() + ": found type information for parameter " + name + ": " + arg.getTypeInfo());
									break;
								}
								
							}
							
						}
					}
					
				}

				// If we couldn't find a type for this name and it's a child of a get property, save it for later.
				if(((CFGNameNode)node).getTypeInfo().getType() == JavaScriptDictionary.NONE &&
					node.getASTNode().getParent().getType() == Token.GETPROP &&
					node.getASTNode().getParent().getFirstChild() == child) {
				
					String propertyNameAccessed = ((CFGGetPropertyNode)getCFGNodeFor(child.getParent())).getPropertyName();
					
					GetPropertyInfo info = mostRecentTypeInfoForName.get(name);
					if(info == null) {
						info = new GetPropertyInfo();
						mostRecentTypeInfoForName.put(name, info);						
					}
					info.objectNames.add((CFGNameNode) node);
					info.propertyNames.add(propertyNameAccessed);
					
				}
				
			}
			
		}
		
	}
	
	private int indexOfParameterNamed(String name) {
		
		for(int i = 0; i < parameters.length; i++)
			if(name.equals(parameters[i]))
				return i;		
		return -1;
		
	}
	
	private boolean computingPaths = false; 
	
	private HashMap<CFGIfNode.CFGEndIfNode, List<CFGPath>> cachedPostConditionalPaths = null;;

	// Find all control flow paths through the function, ignoring loops.
	public Collection<CFGPath> computeOutputAffectingPaths() {
		
		// While computing paths we may encounter a recursive call to this function, which would demand the paths being computed.
		// We can't due this, so we return nothing.
		if(computingPaths) return null;
		computingPaths = true;
		
		cachedPostConditionalPaths = new HashMap<CFGIfNode.CFGEndIfNode, List<CFGPath>>();
		
		paths = computePathsFrom(new CFGEdge(null, enter, "enter", Assumption.NONE));

		cachedPostConditionalPaths = null;
		
		return paths;
		
	}
	
	private final List<CFGPath> computePathsFrom(CFGEdge startEdge) {
				
		// Keep a list of paths we're exploring
		List<CFGPath> pathsToExplore = new Vector<CFGPath>(20);

		CFGPath firstPath = new CFGPath();
		try {
			// No assumptions made here.
			firstPath.addEdge(startEdge);
		} catch (CycleException e) {
		}

		pathsToExplore.add(firstPath);
		
		List<CFGPath> trimmedPaths = new Vector<CFGPath>();
		
		// While we still have more paths to explore, explore the next path in the queue.
		while(!pathsToExplore.isEmpty()) {
									
			// What's the next path in the queue? Remove it and finish it.
			CFGPath path = pathsToExplore.remove(0);

			// Remember that we're on this function so that calls to can affect output don't get stuck in recursive loops.
			Stack<CFGFunction> callstack = new Stack<CFGFunction>();
			callstack.push(this);
			
			Set<CFGNode> visited = new HashSet<CFGNode>();
			
			while(true) {
				
				try {
				
					// Get the most recent node in this path and it's outgoing edges. We'll explore them.
					CFGNode currentNode = path.getLastNode();
					Collection<CFGEdge> outgoingEdges = currentNode.getOutgoingEdges();
					
					if(visited.contains(currentNode)) throw new RuntimeException("There shouldn't be a cycle at " + currentNode + " " + currentNode.getDescriptionOfLocation());
					visited.add(currentNode);
//					Feedlack.out("On " + currentNode + " " + currentNode.getClass().getName() + " " + currentNode.getDescriptionOfLocation());

					// Have we been to this end if node yet? If so, just reuse it's cache. Otherwise, start the cache.
					if(currentNode instanceof CFGIfNode.CFGEndIfNode && outgoingEdges.size() > 0) {
						
						List<CFGPath> postConditionalPaths;

						// If we've already cached these, then reuse the cache.
						if(cachedPostConditionalPaths.containsKey(currentNode)) {
							
							postConditionalPaths = cachedPostConditionalPaths.get(currentNode);
							
						}
						// If we haven't, then compute and cache them.
						else {
							
							postConditionalPaths = computePathsFrom(outgoingEdges.iterator().next());
							cachedPostConditionalPaths.put((CFGEndIfNode) currentNode, postConditionalPaths);
							
						}
						
						if(postConditionalPaths.size() > 100)
							Feedlack.out("Found " + postConditionalPaths.size() + " post conditional paths");
						
						// End this path with all possible endings following this end if.
						for(CFGPath postConditionalPath : postConditionalPaths) {
							CFGPath finishedPath = new CFGPath();
							finishedPath.addPath(path);
							finishedPath.addPath(postConditionalPath);
							trimmedPaths.add(CFGPath.deriveTrimmedPath(finishedPath));
						}
						path = null;
						
						break;
					}
					// If this node can produces output (and always does), then halt this path. No need to explore it any further.
					else if(currentNode.isNativeOutput() && currentNode.canAffectOutput(feedlack, callstack)) {
						if(currentNode.alwaysAffectsOutput(feedlack, callstack).getLikelihood() == Likelihood.YES) {					
							CFGEdge edgeToAdd = outgoingEdges.iterator().next();
							edgeToAdd.setAssumption(Assumption.PRODUCES_OUTPUT);
							path.addEdge(edgeToAdd);
							break;
						}
					}
					
					// If there are no outgoing edges, this path is done.
					if(outgoingEdges.size() == 0) {
						break;							
					}
					// If there's one edge, follow it.
					else if(outgoingEdges.size() == 1) {
						
						CFGEdge edgeToAdd = outgoingEdges.iterator().next();
						path.addEdge(edgeToAdd);
					}
					// If there are multiple edges, make a duplicate path for each one, each following one of the edges.
					else {
						
						// If this isn't a conditional block or it is and it's capable of affecting output, then we follow its paths.
						if(!currentNode.isConditionalBlock() || nodeOrChildrenCanProduceOutput(currentNode, callstack)) {

							// Duplicate this path, remembering it's beginning and end as as, so the diverging paths can reference it.
							CFGPath pathToDuplicate = new CFGPath();
							pathToDuplicate.addPath(path);
							boolean first = true;
							for(CFGEdge edge : outgoingEdges) {
								
								// Reuse the path we were already on.
								if(first) {
									first = false;
									path.addEdge(edge);
								}
								// Make a new path.
								else {
									CFGPath newPath = new CFGPath();
									newPath.addPath(pathToDuplicate);
									newPath.addEdge(edge);
									pathsToExplore.add(newPath);
								}
							}
							
						}
						// Nothing in this conditional's subtree is capable of producing output, so we skip over it.
						// Now we just need to determine where to skip *to*.
						else {
								
							if(currentNode instanceof CFGHookNode.CFGIfDecisionNode) {
								
								Collection<CFGEdge> hookOutgoing = ((CFGHookNode.CFGIfDecisionNode)currentNode).getEndIfNode().getOutgoingEdges();
								if(hookOutgoing.size() > 0) 
									path.addEdge(new CFGEdge(currentNode, hookOutgoing.iterator().next().getTo(), "skip", Assumption.BLOCK_CANNOT_AFFECT_OUTPUT));
								else
									break;
								
							}
							else {
								
								// Keep looking for the sibling nodes, unless we run out, and then find the parent.
								// This will help us find the end of the block.
								Node current = currentNode.getASTNode();
								Node next = current.getNext();
								while(next == null && current.getParent() != null) {
									current = current.getParent();
									next = current.getNext();
								}
		
								// We don't do functions.
								CFGNode to = next == null ? null : next instanceof ScriptOrFnNode ? null : getCFGNodeFor(next);
	
								// If there appears to be no next node, jump to the exit.
								if(to == null)
									to = exit;
	
								path.addEdge(new CFGEdge(currentNode, to, "skip", Assumption.BLOCK_CANNOT_AFFECT_OUTPUT));
								
							}
							
						}
						
					}
				
				} catch(CFGPath.CycleException ex) {
					
					break;
					
				}
				
			}
			
			// We're done exploring this path! Trim it and add it to the list. This would be null if we'd used a set of cached paths above.
			if(path != null)
				trimmedPaths.add(CFGPath.deriveTrimmedPath(path));
			
		}

		return trimmedPaths;

	}

	public void markOutputAffectingDataDependencies() {
		
		for(CFGNode child : children)
			if(child.isNativeOutput())
				child.markOutputAffectingDataDependencies();		
		
	}
	

}