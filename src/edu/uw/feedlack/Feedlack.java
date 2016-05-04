package edu.uw.feedlack;

import java.io.*;
import java.util.*;

import org.htmlparser.Attribute;
import org.htmlparser.Tag;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;

import sun.org.mozilla.javascript.internal.Token;

import com.google.javascript.rhino.ErrorReporter;
import com.google.javascript.rhino.EvaluatorException;
import com.google.javascript.rhino.FunctionNode;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.RhinoException;
import com.google.javascript.rhino.ScriptOrFnNode;
import com.google.javascript.rhino.Parser;
import com.google.javascript.rhino.CompilerEnvirons;

import edu.uw.feedlack.CFGFunction.TooManyPathsException;

/*
 * 
 * We want to generate a control flow graph for each function
 * We want to generate two edges for each conditional
 * We want to generate multiple edges for each function call
 * For every token that could lead to an exception, generate an edge out of the function
 * We want edges from each call of a function that has exception edges coming out
 * 
 * We want to ensure that all paths through the control flow graph
 * contain a DOM change.
 * 
 * 
 * 
 * 
 * So should the nodes be responsible for building their own edges recursively, or just
 * one step at a time? If one step at a time, something else has to build the graph.
 * 
 * 
 */



public final class Feedlack {

	private static final char LINE_PATH_SEPARATOR = '-';
	
	public static void main(String[] args) throws IOException {
		
		String[] paths = convertMainArgumentsIntoPathList(args);

		// We search for the place holder string in the HTML template and replace it with the name of the JavaScript results file. 
		final String htmlResultsTemplate = loadFile(new File("feedlack.html"));
		final String placeholder = "REPLACEDWITHFEEDLACKRESULTSDONOTREMOVE";

		String launchPath = System.getProperty("user.dir"); 

		File resultsFolder = new File("results" + File.separatorChar);
		resultsFolder.mkdir();
		
		for(String projectPath : paths) {

			Feedlack.out("Freeing memory for analysis...");
			System.gc();

			String projectName = projectPath.substring(projectPath.lastIndexOf(File.separatorChar) + 1);
			Feedlack.out("\n\n\n\nANALYZING " + projectName + "\n\n\n\n");
			
			Feedlack feedlack = new Feedlack();
			
			String results = feedlack.analyze(new File(projectPath + File.separatorChar), projectName);

			int indexOfPlaceholder = htmlResultsTemplate.indexOf(placeholder);

			String jsResultsName = projectName + ".js";
			
			{
				File resultsFile = new File(resultsFolder, jsResultsName);
				if(resultsFile.exists()) resultsFile.delete();
				else resultsFile.createNewFile();
	
				OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(resultsFile), "UTF8");
				fos.write(results);
				fos.close();
			}

			{
				String modifiedResultsSource = htmlResultsTemplate.substring(0, indexOfPlaceholder) + jsResultsName + htmlResultsTemplate.substring(indexOfPlaceholder + placeholder.length());
	
				File resultsFile = new File(resultsFolder, projectName + ".html");
				if(resultsFile.exists()) resultsFile.delete();
				else resultsFile.createNewFile();
	
				OutputStreamWriter fos = new OutputStreamWriter(new FileOutputStream(resultsFile), "UTF8");
				fos.write(modifiedResultsSource);
				fos.close();
				
			}
						
		}
				
	}
	
	private static String[] convertMainArgumentsIntoPathList(String[] args) {
		
		if(args.length == 0) {
			
			System.err.println("Need to supply either a path to a folder full of source files (1 argument), or 2 arguments in the form -multiple, where the second argument is a file full of paths.");
			System.exit(0);
			
		}
		
		if(args.length == 1) {
			
			return new String[] { args[0] };
			
		}
		
		if(args.length == 2 && args[0].equals("-projectsfolder")) {
			
			File folder = new File(args[1]);
			if(folder.exists() && folder.isDirectory()) {
				
				Vector<String> paths = new Vector<String>();
				for(File f : folder.listFiles()) {
					
					if(f.isDirectory()) {
						String path = f.getAbsolutePath(); 
						paths.add(path);
					}
					
				}
				
				return paths.toArray(new String[] {});
				
			}
			else {
				System.err.println("You said " + folder + " was a folder, but it's not");
				System.exit(0);				
			}
				
		}
		
		
		System.err.println("I don't know what you're trying to do.");
		System.exit(0);
		
		return null;
		
	}
	
	private int htmlFileCount = 0;
	private int jsFileCount = 0;
	private int jsFunctionCount = 0;
	private int htmlHandlerCount = 0;
	private int jsHandlerCount = 0;
	private int jsStatementCount = 0;
	
	private final Hashtable<Node, CFGNode> CFGNODES_BY_AST_NODES = new Hashtable<Node, CFGNode>();

	protected CFGNode getCFGNode(Node n) { return CFGNODES_BY_AST_NODES.get(n); }
	protected void putCFGNode(Node n, CFGNode cn) { CFGNODES_BY_AST_NODES.put(n, cn); }
	
	private final TableOfListsByName<String, CFGFunction> globalFunctions = new TableOfListsByName<String, CFGFunction>();
	private final Map<CFGFunction,TableOfListsByName<String, CFGFunction>> localFunctions = new Hashtable<CFGFunction,TableOfListsByName<String, CFGFunction>>();
	private final TableOfListsByName<String, CFGFunction> callbackFunctions = new TableOfListsByName<String, CFGFunction>();
	private final TableOfListsByName<String, CFGFunction> objectFunctions = new TableOfListsByName<String, CFGFunction>();
	
	private final Map<ScriptOrFnNode, CFGFunction> functionsByNode = new Hashtable<ScriptOrFnNode, CFGFunction>();

	private final Map<String,List<CFGCallNode>> callsByName = new HashMap<String,List<CFGCallNode>>();
	
	private final Set<String> functionsNotFound = new HashSet<String>();
	
	private final Map<String,String> sourceByPath = new Hashtable<String,String>();

	private final Map<ScriptOrFnNode, Integer> htmlScriptLineNumbers = new Hashtable<ScriptOrFnNode, Integer>();
	
	private final Map<Tag,List<HTMLInputHandler>> htmlHandlers = new LinkedHashMap<Tag,List<HTMLInputHandler>>();
	private final List<JavaScriptInputHandler> jsHandlers = new Vector<JavaScriptInputHandler>();
	
	private final List<String> parsingErrors = new Vector<String>();
	
	private final Map<Node, JavaScriptType> objectLiteralTypes = new HashMap<Node, JavaScriptType>();
	private final Map<String, JavaScriptType> namedObjectLiteralTypes = new HashMap<String, JavaScriptType>();
	
	public static void out(String message) {
		
		message = message.replaceAll("\\n", "\n[feedlack]   ");
		System.out.println("[feedlack]   " + message);
		
	}

	public static void err(String message) {
		
		message = message.replaceAll("\\n", "\n[feedlack]   ");
		System.err.println("[feedlack]\tERROR: " + message);
		
	}

	public CFGFunction getFunctionByNode(ScriptOrFnNode node) { return functionsByNode.get(node); }

	private CFGFunction addFunction(Node parent, FunctionNode function) {

		CFGFunction cfgFunction = new CFGFunction(Feedlack.this, function, parent, false);

		jsStatementCount += cfgFunction.getStatementCount();
		
		String name = cfgFunction.getFunctionName();
		
//		out("\tFound " + cfgFunction.getScope() + " " + cfgFunction.getDescriptionOfFunction());

		switch(cfgFunction.getScope()) {
		
		case GLOBAL:
					
			globalFunctions.put(name, cfgFunction);
			break;
			
		case LOCAL:

			CFGFunction parentFunction = cfgFunction.getParentFunction();

			if(parentFunction != null) {
				TableOfListsByName<String, CFGFunction> functions = localFunctions.get(parentFunction);
				if(functions == null) {
					functions = new TableOfListsByName<String, CFGFunction>();
					localFunctions.put(parentFunction, functions);
				}
				functions.put(name, cfgFunction);
			}
			else
				globalFunctions.put(name, cfgFunction);

			break;
			
		case CALLBACK:
			
			callbackFunctions.put(name, cfgFunction);
			break;
			
		case OBJECT:
			
			objectFunctions.put(name, cfgFunction);
			break;
			
		default:		
		
		}
		
		// Remember this mapping so we can reuse functions we create.
		functionsByNode.put(function, cfgFunction);
	
		jsFunctionCount++;
		
		return cfgFunction;
		
	}

	private final Map<String,Set<CFGAssignNode>> propertyAssignments = new HashMap<String,Set<CFGAssignNode>>();
	
	public Set<CFGAssignNode> getAssignmentsToPropertyName(String propertyName) { return propertyAssignments.get(propertyName); }
	
	private Set<CFGFunction> gatherFunctionsAndMarkTrusted(ScriptOrFnNode source) {
		
		Set<CFGFunction> gathered = new HashSet<CFGFunction>();
		gatherFunctions(source, gathered);
		
		/*
		 *  
		 * prototype.js (var Prototype = ...)
		 * jQuery (jQuery = ...)
		 * Google Analytics (function _trackPageview())
		 * YUI (YUI =)
		 * 
		 */
		
		// Go through each function and look for the above.
		boolean trusted = false;
		for(CFGFunction f : gathered) {
			
			if(f.getFunctionName().equals("_trackPageview")) {
				trusted = true;
			}
			else {
				for(CFGAssignNode ass : f.getAssignments()) {

					if(ass.getAssignee() instanceof CFGGetPropertyNode) {
						
						String propertyName = ((CFGGetPropertyNode)ass.getAssignee()).getPropertyName();
						Set<CFGAssignNode> assignments = propertyAssignments.get(propertyName);
						if(assignments == null) {
							assignments = new HashSet<CFGAssignNode>();
							propertyAssignments.put(propertyName, assignments);
						}
						assignments.add(ass);
						
					}
										
					if(ass.getNameAssigned().equals("jQuery"))
						trusted = true;
					
				}
			}			
			
		}

		// If we found a signifying function, than mark all functions as trusted.
		if(trusted) {
			Feedlack.out("Marking functions in " + source.getSourceName() + " as trusted");
			for(CFGFunction f : gathered) {
				f.markTrusted();
			}				
		}
		
		return gathered;
	
	}
	
	/**
	 * Find all functions in this parse tree by recursively traversing it's children.
	 */
	private void gatherFunctions(Node parent, Set<CFGFunction> gathered) {

		JavaScriptType objectLiteralType = null;
		if(parent.getType() == Token.OBJECTLIT) {
		
			String name = "";

			Node parentParent = parent.getParent();
			if(parentParent != null && parentParent.getType() == Token.ASSIGN) {
				// Was this assigned to a property or a local?
				Node assignee = parentParent.getChildAtIndex(0);
				if(assignee.getType() == Token.GETPROP) {
					Node property = assignee.getLastChild();
					if(property != null && property.getType() == Token.STRING) {
						name = property.getString();							
					}
				}
				else if(assignee.getType() == Token.NAME) {
					name = assignee.getString();
				}
				else if(assignee.getType() == Token.GETELEM) {
				}
				else {
				}
				
			}
			else if(parentParent.getType() == Token.NAME)
				name = parentParent.getString();

			objectLiteralType = new JavaScriptType(name, true);
			objectLiteralTypes.put(parent, objectLiteralType);
			if(!name.equals("")) namedObjectLiteralTypes.put(name, objectLiteralType);			
			
//			Feedlack.out("Found object literal named " + name + " on line " + parent.getLineno());
			
		}
		
		for(Node child : parent.children()) {

//			Feedlack.out("On ");

			if(child instanceof FunctionNode) {
				
				CFGFunction fun = addFunction(parent, (FunctionNode)child);
				gathered.add(fun);
				
				if(objectLiteralType != null) {
					
					objectLiteralType.addFunction(fun);
//					Feedlack.out("Found function " + fun.getFunctionName() + " for type " + objectLiteralType.getName());
					
				}
				
			}

			gatherFunctions(child, gathered);
		}
		
	}
	
	public Collection<JavaScriptType> getObjectLiteralTypes() {
		
		return objectLiteralTypes.values();
		
	}
	
	public JavaScriptType getObjectLiteralType(Node objectLiteral) {
		
		return objectLiteralTypes.get(objectLiteral);
		
	}
	
	public JavaScriptType getNamedObjectLiteralType(String name) {
		
		return namedObjectLiteralTypes.get(name);
		
	}
	
	public List<CFGCallNode> findCallsToFunction(CFGFunction function) {
		
		List<CFGCallNode> calls = callsByName.get(function.getFunctionName());
		
		if(calls == null) return new Vector<CFGCallNode>(0);

		List<CFGCallNode> winnowed = new Vector<CFGCallNode>(10);
		for(CFGCallNode call : calls) {
			
			// Lazy way of seeing if the call matches by using the logic already written to resolve a call from it's context.
			if(resolveFunctionReference(call).contains(function))
				winnowed.add(call);
			
		}
		
		return winnowed;
		
	}
	
	public Set<CFGFunction> resolveFunctionReference(CFGNode functionReference) {
		
		Set<CFGFunction> functions = new HashSet<CFGFunction>();
		Collection<CFGFunction> matches = null;
		String name = null;
		
		int type = functionReference.getASTNode().getType();
		switch(type) {
		
		case Token.CALL:
			
			// What is this called on?
			CFGNode invocation = ((CFGCallNode)functionReference).getInvocationNode();
			name = ((CFGCallNode)functionReference).getFunctionCalled();
			
			if(invocation instanceof CFGGetPropertyNode) {
				matches = objectFunctions.get(name);
			}
			else {
				CFGFunction containingFunction = functionReference.getFunction();
				while(containingFunction != null && matches == null) {
					TableOfListsByName<String, CFGFunction> table = localFunctions.get(containingFunction);
					if(table != null)
						matches = table.get(name);
					
					containingFunction = containingFunction.getParentFunction();
				}
				
				// If matches is still null, look for global functions
				if(matches == null)			
					matches = globalFunctions.get(name);
			}

			// Filter these functions by argument/parameter count mismatch. In particular, only allow through functions
			// that have at least as many parameters as are sent by the call.
			if(matches != null) {
				int argCount = ((CFGCallNode)functionReference).getArgumentCount();
				Iterator<CFGFunction> matchIterator = matches.iterator();
				while(matchIterator.hasNext()) {
					
					CFGFunction match = matchIterator.next();
					if(argCount > match.getParameterCount()) {
						matchIterator.remove();
//						Feedlack.out("Removing " + match.getDescriptionOfFunction() + " " + match.getDescriptionOfLocation() + " from consideration for " + functionReference.getDescriptionOfLocation() + " resolution.");
					}
					
				}			
			}
				
			break;
		
		// If the node passed in is a function itself, return the function that represents this function node.
		case Token.FUNCTION:

			CFGFunction function = getFunctionByNode((com.google.javascript.rhino.FunctionNode)((CFGFunctionNode)functionReference).getASTNode());
			functions.add(function);

			break;

		case Token.NAME:
			
			matches = resolve((CFGNameNode)functionReference);
			
			break;

		case Token.GETPROP:
			
			name = ((CFGGetPropertyNode)functionReference).getPropertyNode().getCodeString();
			matches = objectFunctions.get(name);
			
			break;
			
		case Token.NULL:
			
			break;
			
		default:
			
			// Have to resolve by guessing.
			Feedlack.err("Don't know resolve as a function: " + functionReference.getASTNode() + " " + functionReference.getDescriptionOfLocation() );
			
		}

		if(matches != null)
			functions.addAll(matches);
		
		if(functions.isEmpty() && name != null)
			functionsNotFound.add(name);
		
		return functions;		
		
	}
	
	private Collection<CFGFunction> resolve(CFGNameNode node) {
		
		Collection<CFGFunction> matches = null;

		String name = node.getName();
		
		// If this is a local variable, look in the locals for this function.
		CFGFunction containingFunction = node.getFunction();
		if(containingFunction != null) {
			TableOfListsByName<String, CFGFunction> table = localFunctions.get(containingFunction);
			if(table != null)
				matches = table.get(name);
		}
		if(matches == null)			
			matches = globalFunctions.get(name);

		return matches;

	}

	
	/**
	 * Expects a directory of HTML and JavaScript files.
	 */
	private void loadLocalProject(File location) {
		
		if(!location.exists() || !location.isDirectory()) {
			err("Couldn't read folder "+ location);
			System.exit(-1);
		}
		
		for(final File file : location.listFiles()) {

			if(file.isDirectory())
				loadLocalProject(file);
			else {
			
				if(file.getName().endsWith(".html") || file.getName().endsWith(".htm")) {
					out("Loading " + file.getAbsolutePath() + "...");
					try {
						handleHTMLSource(file, loadFileSource(file));
					} catch (ParserException e) {
						err("There was a problem parsing " + location + " " + e.getMessage());
						
						String[] errors = e.getMessage().split("\n");
						for(String error : errors) {
							error = error.replaceAll("\"", "");
							error = error.replaceAll("'", "");
							parsingErrors.add(string(error));
						}
					}
				}
				else if(file.getName().endsWith(".js")) {
					out("Loading " + file.getAbsolutePath() + "...");
					handleJavaScriptSource(file.getAbsolutePath(), loadFileSource(file));
				}
				else {
//					out("Ignoring " + file.getAbsolutePath());
				}
				
			}
			
		}

	}
	
	private static String loadFile(File file) {
		
		BufferedReader in;
		String fileText = "";
		try {
			in = new BufferedReader(new FileReader(file));
			StringBuilder text = new StringBuilder();
			String str;
			while ((str = in.readLine()) != null) {
				text.append(str + "\n");
			}
			fileText = text.toString();
	   
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileText;
		
	}
	
	private static String prepareFilenameForUI(String filename) {
		
		return filename.replace(LINE_PATH_SEPARATOR, '_').replace('!', '_');
		
	}
	
	private String loadFileSource(File file) {
		
		String fileText = loadFile(file);
/*		
		if(file.getAbsolutePath().endsWith("htm") || file.getAbsolutePath().endsWith("html")) {
			
			Tidy tidy = new Tidy();
			tidy.setErrout(new PrintWriter(new StringWriter()));
			tidy.setIndentAttributes(true);
			tidy.setIndentContent(true);
			tidy.setShowWarnings(false);
			tidy.setShowErrors(0);
			StringWriter s = new StringWriter();
			tidy.parse(new StringReader(fileText), s);
			String newFileText = s.toString();

			// Sometimes jTidy returns nothing. I don't know why!
			if(!newFileText.equals(""))
				fileText = newFileText;

		}
*/
		sourceByPath.put(prepareFilenameForUI(file.getAbsolutePath()), fileText);
		
		return fileText;
		
	}
	
	private void handleHTMLSource(final File file, String source) throws ParserException {
				
		htmlFileCount++;
		
		// http://java-source.net/open-source/html-parsers/html-parser
		org.htmlparser.Parser parser;
		parser = new org.htmlparser.Parser(source);
		
		parser.visitAllNodesWith(new NodeVisitor() {
			
			private int parsingTestErrorCount = 0; 
			
		     public void visitTag (final Tag tag) {
		    	 
		    	 // Is this a script tag?
		    	 if(tag.getTagName().equalsIgnoreCase("script")) {
		    		 
		    		 boolean isJavaScript = false;
			    	 for(Object obj : tag.getAttributesEx()) {
			    		 if(obj instanceof Attribute) {
			    			 Attribute attr = (Attribute)obj;
			    			 if(attr.getName() != null && attr.getName().equalsIgnoreCase("type")) {
			    				 String code = attr.getValue();
			    				 if(code.contains("javascript") || code.contains("ecmascript")) {
			    					 isJavaScript = true;
			    					 break;
			    				 }
			    			 }
			    			 else if(attr.getName() != null && attr.getName().equalsIgnoreCase("language")) {
			    				 String code = attr.getValue();
			    				 if(code.contains("javascript") || code.contains("ecmascript")) {
			    					 isJavaScript = true;
			    					 break;
			    				 }
			    			 }
			    		 }
			    	 }
		    		 
			    	 // If we didn't find a declared type, try parsing it.
			    	 if(!isJavaScript) {

			    		 parsingTestErrorCount = 0;
						 Parser parser = new Parser(new CompilerEnvirons(), new ErrorReporter() {
						    public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
						    }
						    public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
						    	parsingTestErrorCount++;
						    }
						    public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
						    	return new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
						    }
						 });
	    				 try {
	    					 parser.parse(tag.toPlainTextString(), "", 1);
							 if(parsingTestErrorCount == 0)
								 isJavaScript = true;
	    				 } catch(RhinoException ex) {}
	    				 
			    	 }
			    	 
			    	 if(isJavaScript) {
			    		
			    		 String code = tag.toPlainTextString();
			    		 
	    				 try {
	    					 
	    					 if(code.toLowerCase().startsWith("javascript:")) {
	    						 code = code.substring(11);
	    					 }
    						 code = code.replaceAll("&quot;", "'");
    						 code = code.replaceAll("&lt;", "<");
    						 code = code.replaceAll("&gt;", ">");
    						 code = code.replaceAll("&amp;", "&");
    						 code = code.replaceAll("%20", " ");
	    					 
    						 Parser parser = new Parser(new CompilerEnvirons(), new ErrorReporter() {
    								
    							    public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
    							    }

    							    public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
    							    	String err = sourceName + ":" + (line + tag.getStartingLineNumber()) + " " + message;
    							    	out("Syntax error " + err + " on code " + lineSource);
    							    	parsingErrors.add(string(err));
    							    }

    							    public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
    							    	return new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
    							    }
    							});
    						 
	    					 ScriptOrFnNode script = parser.parse(code, prepareFilenameForUI(file.getAbsolutePath()), 1);
	    					 gatherFunctionsAndMarkTrusted(script);																	
	    					 htmlScriptLineNumbers.put(script, tag.getEndingLineNumber());
	    					 
	    				 } catch(RhinoException ex) {
	    				 }

			    	 }
		    		 
		    	 }
		    	 
		    	 // Does this tag have an event attached to it? If so, we're going to assume it handles output and require that it produces output.
		    	 boolean handlesInput = false;
		    	 String type = null;
		    	 for(Object obj : tag.getAttributesEx()) {
		    		 if(obj instanceof Attribute) {
		    			 Attribute attr = (Attribute)obj;
		    			 // Strip the "on" from the event name, if it has it, and see if it's in our list of input events requiring output.
		    			 
		    			 if(attr.getName() != null && 
		    					((attr.getName().startsWith("on") && JavaScriptDictionary.isInputEventNameRequiringOutput(attr.getName().substring(2))) ||
		    						(attr.getName().equalsIgnoreCase("href") && tag.getTagName().equalsIgnoreCase("a") && attr.getValue().toLowerCase().startsWith("javascript:")))) {
		    				 
		    				 handlesInput = true;
		    				 String code = attr.getValue();
		    				 try {
		    					 
		    					 // In case ASP is involved.
		    					 if(code.toLowerCase().startsWith("javascript:")) {
		    						 code = code.substring(11);
		    					
		    						 // People format void in some funky ways.
		    						 if(code.startsWith("void%20"))
		    							 code = code.substring(7);
		    						 else if(code.startsWith("void(") && (code.endsWith(")") || code.endsWith(");")))
		    							 code = code.substring(5, code.lastIndexOf(')'));
		    					 
		    					 }
	    						 code = code.replaceAll("&quot;", "'");
	    						 code = code.replaceAll("&lt;", "<");
	    						 code = code.replaceAll("&gt;", ">");
	    						 code = code.replaceAll("&amp;", "&");
	    						 code = code.replaceAll("%20", " ");
	    						 
		    					 ScriptOrFnNode script = PARSER.parse(code, prepareFilenameForUI(file.getAbsolutePath()), 1);
		    					 gatherFunctionsAndMarkTrusted(script);
		    					 
		    					 String tagText = tag.getText();
		    					 int locationOfAttribute = tagText.indexOf(attr.getName());
		    					 int additionalLines = 0;
		    					 if(locationOfAttribute >= 0) {
		    						 String textBeforeAttribute = tagText.substring(0, locationOfAttribute);
		    						 for(int i = 0; i < textBeforeAttribute.length(); i++)
		    							 if(textBeforeAttribute.charAt(i) == '\n') additionalLines++;
		    					 }
		    					 
		    					 int lineNumber = tag.getStartingLineNumber() + additionalLines;
		    					 
		    					 htmlScriptLineNumbers.put(script, lineNumber);
		    					 
		    					 Set<CFGFunction> scripts = new HashSet<CFGFunction>();
		    					 scripts.add(new CFGFunction(Feedlack.this, script, null, true));
		    					 // Yes, there's only one script, but this isn't true for all input handlers. Some name a handler, requiring resolution.
		    					 HTMLInputHandler handler = new HTMLInputHandler(attr, tag, lineNumber, file, scripts);
		    					 
		    					 List<HTMLInputHandler> tagHandlers = htmlHandlers.get(tag);
		    					 if(tagHandlers == null) {
		    						 tagHandlers = new Vector<HTMLInputHandler>();
		    						 htmlHandlers.put(tag, tagHandlers);		    						 
		    					 }
		    					 tagHandlers.add(handler);		    					 
		    					 
		    					 htmlHandlerCount++;
		    					 
		    				 } catch(RhinoException ex) {
		    					 
		    					 // Code must not have been parseable?
		    					 
		    				 }
		    			 }
		    			 else if(attr.getName() != null && attr.getName().equalsIgnoreCase("type"))
		    				 type = attr.getValue().toLowerCase();
		    		 }
		    	 }
		    	 
		    	 // Is this a button or slider that doesn't handle input? If so, it should explicitly produce feedback, since they don't by default.
		    	 if(!handlesInput) {
		    		 
		    		 if(tag.getTagName().equalsIgnoreCase("button")) {
		    			 
		    			 if(type == null || type.equals("button")) {
	
		    				 // TODO Want to do something about these?
//		    				 Feedlack.out("No handler for button");
		    				 
		    			 }
		    			 
		    		 }
		    		 else if(tag.getTagName().equalsIgnoreCase("input")) {
		    			 
		    			 if(type == null || type.equals("button") || type.equals("range")) {
		    				 
		    				 // TODO Want to do something about these?
//		    				 Feedlack.out("No handler for button");
		    				 
		    			 }
		    			 
		    		 }
		    		 
		    	 }
		    	 
		     }
		});
		
	}
		
	private final Parser PARSER = new Parser(new CompilerEnvirons(), new ErrorReporter() {
		
	    public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
	    }

	    public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
	    	String err = sourceName + ":" + line + " " + message;
	    	out("Syntax error " + err + " on code " + lineSource);
	    	parsingErrors.add(string(err));
	    }

	    public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
	    	return new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
	    }
	});
	
	private void handleJavaScriptSource(String fileName, String source) {
			
		try {
			
			ScriptOrFnNode script = PARSER.parse(source, prepareFilenameForUI(fileName), 1);
			gatherFunctionsAndMarkTrusted(script);

			jsFileCount++;

		} catch(EvaluatorException ex) {
			
		}

	}
		
	private String analyze(File projectPath, String projectName) throws IOException {
		
		loadLocalProject(projectPath);
		
		out("");
		out("SEARCHING FOR INPUT HANDLERS ...");
		out("");
		
		// Go through each function and see if they handle input. Gather them together in a nice big list.
		// Also, if a function can produce output, generate all of its paths.
		Collection<CFGFunction> allFunctions = globalFunctions.getAll();
		for(TableOfListsByName<String, CFGFunction> table : localFunctions.values())
			allFunctions.addAll(table.getAll());
		allFunctions.addAll(callbackFunctions.getAll());
		allFunctions.addAll(objectFunctions.getAll());

		// Now that we have all the functions loaded, find each of their calls to make a global call database
		for(CFGFunction f : allFunctions) {
			
			Collection<CFGCallNode> calls = f.getCalls();
			for(CFGCallNode call : calls) {
				String functionCalled = call.getFunctionCalled();
				List<CFGCallNode> matchingCalls = callsByName.get(functionCalled);
				if(matchingCalls == null) {
					matchingCalls = new Vector<CFGCallNode>(10);
					callsByName.put(functionCalled, matchingCalls);
				}
				matchingCalls.add(call);
			}
			
		}
		
		// Compute type information for local variables in each. Note that we analyze calls in this process,
		// so we have to do it after we have the call information above. Moreover, this call is recursive.
		for(CFGFunction f : allFunctions) {

			f.computeLocalTypeInformation();

		}
		

		// TODO We'll get to this later, if necessary.
//		for(CFGFunction f : allFunctions) {
//
//			f.markOutputAffectingDataDependencies();
//			
//		}
		
		for(CFGFunction f : allFunctions) {

			Collection<JavaScriptInputHandler> newHandlers = f.getInputHandlers();
			jsHandlers.addAll(newHandlers);

			jsHandlerCount += newHandlers.size();
			
			String fun = f.getDescriptionOfFunction() + " " + f.getDescriptionOfLocation();
			
			if(f.canAffectOutput(this)) {

				out("\tComputing paths of " + fun);
				
				f.getOutputAffectingPaths();
			}

		}
		
		// Release the list, to save memory.
		allFunctions = null;
				
		// Start by getting a set of functions pointing to handlers, so that we have a nice tidy
		// table of input handling functions and all of the input events that call them.
		Map<CFGFunction,Set<InputHandler>> handlersByFunction = new LinkedHashMap<CFGFunction,Set<InputHandler>>();
		
		for(InputHandler handler : jsHandlers) {

			for(CFGFunction function : handler.getFunctions()) {
			
				Set<InputHandler> functionsHandlers = handlersByFunction.get(function);
				if(functionsHandlers == null) {
					functionsHandlers = new HashSet<InputHandler>();
					handlersByFunction.put(function, functionsHandlers);
				}
				functionsHandlers.add(handler);
				
			}
			
		}

		for(List<HTMLInputHandler> handlerSet : htmlHandlers.values()) {
			
			for(HTMLInputHandler handler : handlerSet) {

				for(CFGFunction function : handler.getFunctions()) {
				
					Set<InputHandler> functionsHandlers = handlersByFunction.get(function);
					if(functionsHandlers == null) {
						functionsHandlers = new HashSet<InputHandler>();
						handlersByFunction.put(function, functionsHandlers);
					}
					functionsHandlers.add(handler);
					
				}
				
			}
			
		}		
		
		// This list will keep track of all of the paths missing feedback across all handlers. 
		List<Scenario> scenariosLacking = new Vector<Scenario>();
		
		List<InputHandler> okayHandlers = new Vector<InputHandler>();
		
		out("");
		out("ANALYZING " + htmlHandlerCount + " HTML INPUT HANDLERS");
		out("");

		// Go through each tag
		for(Tag tag : htmlHandlers.keySet()) {

			boolean hasLink = false;
			
			// Does this tag have a non-javascript href tag? If so, none if it's handlers need to produce output.
			for(Object obj : tag.getAttributesEx()) {
				if(obj instanceof Attribute) {
					Attribute attr = (Attribute)obj;
					if(attr.getName() != null && attr.getName().equals("href") && tag.getTagName().equalsIgnoreCase("a")) {
						
						String value = attr.getValue();
						if(!value.toLowerCase().startsWith("javascript:") &&
								!value.equals("#") &&
								!value.equals("")) {
							hasLink = true;			
						}
					}
				}
			}
			
			boolean atLeastOneProducesOutput = false;
			boolean allProduceOutput = true;

			List<HTMLInputHandler> handlers = htmlHandlers.get(tag);
			
			List<Scenario> scenariosLackingInThisTag = new Vector<Scenario>();
						
			// Go through each handler on this tag
			for(HTMLInputHandler handler : handlers) {
			
				// Go through all of the functions on this handler on this tag (should just be one, the inline script)
				for(CFGFunction function : handler.getFunctions()) {
					
					Feedlack.out("On " + function.getDescriptionOfFunction() + " " + function.getDescriptionOfLocation());

					List<Scenario> scenarios = identifyOutputLackingPathsOf(function, handlersByFunction.get(function));

					if(scenarios.isEmpty()) {
						atLeastOneProducesOutput = true;
					}
					else
						allProduceOutput = false;

					for(Scenario s : scenarios)
						scenariosLackingInThisTag.add(s);
					
				}

			}

			if(allProduceOutput)
				okayHandlers.addAll(handlers);
			
			// If this is an anchor tag with a link, no need to report lacking paths.
			if(hasLink) {
				Feedlack.out("\tThis is an anchor tag with a link, so none if it's " + htmlHandlers.get(tag).size() + " handlers need to produce output.");
			}
			// If there's at least one handler producing output, then this tag is fine. Otherwise, report all lacking paths.
			else if(atLeastOneProducesOutput) {
				Feedlack.out("\tAt least one of this tags " + handlers.size() + " handlers produces output, so not reporting lacking paths.");
			}
			// Otherwise, report the lacking paths.
			else {
				for(Scenario s : scenariosLackingInThisTag)
					scenariosLacking.add(s);
			}
			
		}
		
		out("");
		out("ANALYZING " + handlersByFunction.size() + " JAVASCRIPT INPUT HANDLERS");
		out("");

		for(InputHandler handler : jsHandlers) {
			
			boolean allProduceOutput = true;

			for(CFGFunction function : handler.getFunctions()) {

				Feedlack.out("On " + function.getDescriptionOfFunction() + " " + function.getDescriptionOfLocation());
	
				List<Scenario> scenarios = identifyOutputLackingPathsOf(function, handlersByFunction.get(function));

				if(!scenarios.isEmpty())
					allProduceOutput = false;
				
				for(Scenario s : scenarios)
					scenariosLacking.add(s);
				
			}

			if(allProduceOutput)
				okayHandlers.add(handler);
			
		}
		
		Feedlack.out("");
		Feedlack.out("CLUSTERING...");
		Feedlack.out("");
		
		///////////////////////////////
		// Split all of the scenarios into clusters and write out cluster data.
		///////////////////////////////
		
		List<ScenarioCluster> lackingClusters = splitScenariosIntoClusters(scenariosLacking);
		List<ScenarioCluster> clusters = new Vector<ScenarioCluster>();
		clusters.addAll(lackingClusters);
		
		/* Here's the data structure we want:
		
		clusters: [
			{
				intersection: [ decision, ... ],
				to: [ decision, ... ],
				from: [ decision, ... ],
			}
			...
		]
		*/
		
		List<String> clusterList = new Vector<String>();
		for(ScenarioCluster cluster : clusters) {

			List<String> decisions = new Vector<String>();
			
			CFGFunction functionOfFirstDecision = null;
			Decision lastDecision = null;
			
			for(Decision decision : cluster.getCommonSequence()) {
							
	    		decisions.add(
	    			decision(decision)
	    		);
	    	
				if(functionOfFirstDecision == null)
					functionOfFirstDecision = decision.getNode().getFunction();

	    		lastDecision = decision;
	    		
			}
			
			List<String> callers = new Vector<String>();
			for(InputHandler handler : cluster.getInputHandlers()) {

				String location = string(line(prepareFilenameForUI(handler.getPath()), handler.getLine(this)));
				
				callers.add(
					object(
						property("event", string(handler.getEventDescription())),
						property("location", location)
					)
				);
				
			}

			List<List<Decision>> uniqueOutgoingPaths = new Vector<List<Decision>>();
			
			// Add all decisions following the critical decisions, stopping when we leave the scope
			// of the first function of the critical decisions.
			List<String> outgoing = new Vector<String>();
			for(Scenario scenario : cluster) {
								
				// We use this flag to note when we've reached it.
				boolean afterLastDecision = false;
				boolean reachedFunctionOfFirstDecision = false;
				boolean passedFunctionOfFirstDecision = false;
				
				List<Decision> decisionsFollowingCriticalSequence = new Vector<Decision>();

				// Go through all of the decisions.
				for(Decision d : scenario.getDecisions()) {
					
					// If we found the last decision of the critical sequence, start including everything after.
					if(d.equals(lastDecision)) {
						afterLastDecision = true;
					}
					else if(afterLastDecision) {
						
						if(functionOfFirstDecision != null) {
							// If this function is in the function of the first decision, remember that we've reached it.
							if(d.getNode().getFunction() == functionOfFirstDecision)
								reachedFunctionOfFirstDecision = true;
							// If it's not, but we had reached it before, leave the loop before we add this decision.
							else if(reachedFunctionOfFirstDecision)
								break;
						}

						decisionsFollowingCriticalSequence.add(d);
						
					}
					
				}
				
				// Now that we've selected the decisions following the critical sequence, is it a new sequence?
				boolean shouldAdd = true;
				for(List<Decision> potentialOutgoing : uniqueOutgoingPaths) {

					if(potentialOutgoing.size() == decisionsFollowingCriticalSequence.size()) {

						boolean allEqual = true;
						for(int i = 0; i < potentialOutgoing.size(); i++) {
						
							if(!potentialOutgoing.get(i).equals(decisionsFollowingCriticalSequence.get(i))) {
								allEqual = false;
								break;
							}
							
						}
						
						// If all of the decisions were equal, then its redundant.
						if(allEqual)
							shouldAdd = false;
						
					}
					
				}
				
				if(shouldAdd) {
					
					uniqueOutgoingPaths.add(decisionsFollowingCriticalSequence);
					
					List<String> outgoingDecisions = new Vector<String>();
					for(Decision d : decisionsFollowingCriticalSequence) {
						outgoingDecisions.add(
								decision(d)								
						);
					}

					if(outgoingDecisions.size() > 0) {				
						outgoing.add(
							object(
								property("decisions", array(outgoingDecisions))
							)
						);
					}
					
				}
				
			}
			
			clusterList.add(
				object(
						property("isLackingOutput", Boolean.toString(cluster.isLackingOutput())),
						property("inputHandlers", array(callers)),
						property("criticalDecisions", array(decisions)),
						property("outgoing", array(outgoing))
				)
			);
			
		}
		
		
		///////////////////////////////
		// Now JSONify the source files.
		///////////////////////////////			
		
		List<String> fileList = new Vector<String>();

		for(String path : sourceByPath.keySet()) {					

			// Get the source and replace it's lts and gts with appropriate codes.
			String source = sourceByPath.get(path);
			
			source = source.replaceAll("\t", "  ");
			source = source.replaceAll("\"", "'");
			
			String[] lines = source.split("\n");
			int lineNumber = 1;
			List<String> lineList = new Vector<String>();
			for(String line : lines) {

				lineList.add(string(line));
				lineNumber++;
				
			}
			
			String file = 
				object(
					property("path", string(location(path))),
					property("lines", array(lineList))
				);

			fileList.add(file);
			
		}

		
		// Make a list of all handlers that successfully produce output
		List<String> okayHandlerList = new Vector<String>();
		for(InputHandler handler : okayHandlers) {
			
			String location = string(line(prepareFilenameForUI(handler.getPath()), handler.getLine(this)));
			
			okayHandlerList.add(
				object(
					property("event", string(handler.getEventDescription())),
					property("location", location)
				)
			);

			
			
		}
		
		// Now that we have JSON strings for all of the data structures, write the feedlack variable to the results file.
		String results =
			"var feedlack = " + 
			object(
				property("project", string(projectName)),
				property("htmlFileCount", Integer.toString(htmlFileCount)),
				property("jsFileCount", Integer.toString(jsFileCount)),
				property("jsFunctionCount", Integer.toString(jsFunctionCount)),
				property("htmlHandlerCount", Integer.toString(htmlHandlerCount)),
				property("jsHandlerCount", Integer.toString(jsHandlerCount)),
				property("jsStatementCount", Integer.toString(jsStatementCount)),
				property("clusters", array(clusterList)),
				property("files", array(fileList)),
				property("okayHandlers", array(okayHandlerList)),
				property("parsingerrors", array(parsingErrors))
			)
		;


		out("");
		out("FUNCTIONS NOT FOUND");
		out("");

		for(String f : functionsNotFound)
			Feedlack.out("\t" + f + "()");
			
		
		out("");
		out("DONE");
		
		return results;
		
	}
	
	private List<Scenario> identifyOutputLackingPathsOf(CFGFunction function, Set<InputHandler> calls) {
				
		String name = function.getDescriptionOfFunction();
		
		List<Scenario> scenariosLacking = new Vector<Scenario>();

		try {

			// Here are all of the paths through the input handler.
			Collection<CFGPath> functionPaths = function.getPathsWithExpandedOutputAffectingInvocations();

			assert functionPaths.size() > 0 : "" + function.getDescriptionOfLocation() + " returned 0 expanded paths, which shouldn't ever happen.";
			
			Collection<CFGPath> outputProducing = new Vector<CFGPath>(20);
			Collection<CFGPath> outputLacking = new Vector<CFGPath>(20);
			
			// For each path through this function that could potentially touch output, determine
			// whether the path always produces output and put it into the appropriate set. 
			for(CFGPath path : functionPaths) {
				if(path.alwaysProducesOutput(this))
					outputProducing.add(path);
				else
					outputLacking.add(path);
				
			}
					
			Feedlack.out(
					"\t" + 
					functionPaths.size() + " paths, " + 
					outputLacking.size() + " lacking and " + 
					outputProducing.size() + " producing.");
						
		    for(CFGPath path : outputLacking) {
			
		    	// Create a scenario for this path.
		    	Scenario scenario = path.getScenario(calls, function, true);
	    		scenariosLacking.add(scenario);
		    			    	
    		}

			return scenariosLacking;
			
		} catch(TooManyPathsException ex) {
			
			Feedlack.out("\tHad to skip handler because too many paths were generated.");
		
		}	

		return scenariosLacking;
		
	}
	
	/**
	 * Takes a set of scenarios (which each consist of a sequence of nodes) and splits the set into
	 * several sets such that each set has no nodes in common.
	 */
	private List<ScenarioCluster> splitScenariosIntoClusters(List<Scenario> scenarios) {

		// Start with an empty list of sets {} C.
		// For each scenario S
		//		determine the size of the intersection between S's node set and the node sets in C
		//		choose the set N from C such that min(N1...Nn intersect S)
		//		if no such set exists, create a new set N, add N to C
		//		add S to N
		// Return C
		
		// This is the list of scenario clusters. It starts off empty.
		List<ScenarioCluster> clusters = new Vector<ScenarioCluster>();

		// Go through each scenario...
		for(Scenario scenario : scenarios) {
			
			int maximumMinimumIntersection = 0;
			ScenarioCluster bestCluster = null;

			// Of all of the existing clusters, which cluster has the largest number of nodes in common?
			for(ScenarioCluster cluster : clusters) {

				// What is the smallest intersection of all of the scenarios in this cluster? This is a conservative measure
				// of how much this scenario has in common with this cluster.
				int minimumIntersection = Integer.MAX_VALUE;
				for(Scenario member : cluster) {
					
					// What's the intersection between
					Set<Decision> intersection = computeScenarioIntersection(member, scenario);
					int size = intersection.size();
					if(size < minimumIntersection)
						minimumIntersection = size;
					
				}

				// If this scenario had at least one node in common with at least one scenario in this cluster
				// and the size of the smallest intersection was greater than the size of the smallest of all of the
				// clusters we have checked so far, then this cluster is the new closest match.
				if(minimumIntersection > 0 && minimumIntersection > maximumMinimumIntersection) {
					bestCluster = cluster;
					maximumMinimumIntersection = minimumIntersection;
				}
				
			}

			// If none of the clusters had nodes in common with this scenario, create a new cluster that includes this scenario.
			// When there are no clusters, this will happen immediately, because there will be no clusters to match to.
			if(bestCluster == null) {
				
				bestCluster = new ScenarioCluster();
				clusters.add(bestCluster);
				
			}

			// Finally, add the scenario to the cluster that matched best and continue on to the next scenario.
			bestCluster.add(scenario);
			
		}

		// Sort the clusters by their size, from largest to smallest.
		Collections.sort(clusters, new Comparator<ScenarioCluster>() {
			@Override
			public int compare(ScenarioCluster o1, ScenarioCluster o2) {
				return o2.getScenarioCount() - o1.getScenarioCount();
			}
		});
		
		out("");
		out("CONVERTED " + scenarios.size() + " SCENARIOS into " + clusters.size() + " CLUSTERS");
		out("");

		int i = 1;	// This is just for debugging output purposes, to number clusters.

		
		
		// Go through each cluster and find out what node sequence all scenarios in the cluster have in common.
		for(ScenarioCluster cluster : clusters) {

			out("CLUSTER " + i + " (" + cluster.getScenarioCount() + " scenarios)");
			for(Scenario s : cluster) {

				out(s.toString());
				
			}
			
			out("intersection is");
			for(Decision d : cluster.getCommonSequence()) {
				out(d.getNode().getDescriptionOfLocation() + " " + d.getNode().getClass().getSimpleName());
			}
			
			out("");
			out("input handlers include");
			for(InputHandler h : cluster.getInputHandlers()) {
				out(h.getDescription());
			}
			
			out("");
			
			i++;
		
			// Move on to determining the intersection of the next cluster.
			
		}
		
		
		
		return clusters;
		
	}
	
	private Set<Decision> computeScenarioIntersection(Scenario one, Scenario two) {
		
		// Make a set with all of scenario 1's nodes
		Set<Decision> oneSet = new HashSet<Decision>();
		for(Decision d : one.getDecisions()) oneSet.add(d);
		
		// Make a set with all of scenario 2's nodes
		Set<Decision> twoSet = new HashSet<Decision>();
		for(Decision d : two.getDecisions()) twoSet.add(d);

		// Return the set containing the intersection between set 1 and set 2
		oneSet.retainAll(twoSet);
		return oneSet;
		
	}

	@Deprecated
	private void simplifyScenarios(List<Scenario> scenarios) {

//		int i = 1;
//		for(Scenario s : scenarios) {
//			
//			System.out.println("");
//			System.out.println("scenario " + i);
//			System.out.println("");
//			i++;
//	    	for(Scenario.Decision decision : s.getDecisions()) {
//
//	    		String file = decision.getNode().getFunction().getFunctionNode().getSourceName();
//	    		file = file.substring(file.lastIndexOf('/') + 1);
//	    		System.out.println(file + ":" + getLineNumber(decision.getNode().getASTNode()));
//
//	    	}
//			
//		}		

		
// Psuedocode for simplification algorithm.
//
//		scenariosByPivots<Scenario, List<Scenario>> = {}
//		// Compare each scenario to each other scenario
//		for each scenario S
//		     for each scenario T
//		          // If S is a subsequence of T, add S to the pivot list and include T in its list.
//		          if S is not T, and
//		           |S| <= |T|, and
//		           S appears at the end of T
//		               add T to scenariosByPivots[S]
//		               // If T was already a pivot, demote it and include it in the new shorter S pivot
//		               if T is key in scenariosByPivots
//		                    let Subsumed = scenariosByPivots[T]
//		                    remove T from scenariosByPivots
//		                    add T to scenariosByPivots[S]
//		                    add all in Subsumed to scenariosByPivots[S]

		Map<Scenario, Set<Scenario>> pivots = new Hashtable<Scenario, Set<Scenario>>();
		
		// Put the scenarios in order of decreasing length, so that smaller and smaller pivots are selected.
		Collections.sort(scenarios, new Comparator<Scenario>() {
			@Override
			public int compare(Scenario o1, Scenario o2) {
				return o2.getLength() - o1.getLength();
			}
		});
		
		for(Scenario potentialPivot : scenarios) {
			
//			System.out.println("Checking pivot \n " + potentialPivot);
			
			// All scenarios start off as a potential pivot, but are later removed if they subsume a pivot.
			pivots.put(potentialPivot, new HashSet<Scenario>());
			
			// Scenarios with no nodes can't be pivots.
			if(potentialPivot.getLength() > 0) {
			
				for(Scenario potentialMatch : scenarios) {
					
	//				System.out.println("Comparing to \n" + potentialMatch);
					
					if(potentialPivot != potentialMatch && potentialPivot.getLength() <= potentialMatch.getLength()) {
	
						// Starting from the end, go through each node and see if there's a match.
						// Innocent until provent guilty.
						boolean match = true;
						for(int index = 0; index < potentialPivot.getLength(); index++) {
	
							if(potentialPivot.getNthDecision(potentialPivot.getLength() - index - 1).getNode() != potentialMatch.getNthDecision(potentialMatch.getLength() - index - 1).getNode()) {
								match = false;
								break;
							}
	
						}
						
						// If the potential pivot appeared at the end of the potential match, then promote it to a pivot.
						if(match) {
							
							Set<Scenario> subsumers = pivots.get(potentialPivot);
							subsumers.add(potentialMatch);
	
							// Was the potential match already a pivot? Demote it and add its subsumers to the new pivot.
							if(pivots.containsKey(potentialMatch)) {
								
								Set<Scenario> subsumersSubsumers = pivots.get(potentialMatch);
								pivots.remove(potentialMatch);
								subsumers.addAll(subsumersSubsumers);
								
							}
							
						}
						
					}
					
				}
				
			}
						
		}
		
		int i = 1;
		for(Scenario pivot : pivots.keySet()) {
			
			System.out.println("pivot " + i + " (" + pivots.get(pivot).size() + " paths)\n");
			System.out.println(pivot.toString());
			
			i++;
						
		}
		
		System.out.println("Out of " + scenarios.size() + " scenarios, found " + pivots.size() + " pivots.");
		
	}

	public int getLineNumberOfFunction(CFGFunction fun) {
		
		return getLineNumberOfASTNode(fun.getFunctionNode());
		
	}
	
	public int getLineNumberOfNode(CFGNode cfgNode) {
		
		int lineNumber = getLineNumberOfASTNode(cfgNode.getASTNode());
		
		// Exceptional case for exit nodes, which should point to the closing brace, even though the AST node they 
		// point to is the function node, because there is no closing brace AST node.
		if(cfgNode instanceof CFGExitNode) {
			
			ScriptOrFnNode fun = (ScriptOrFnNode)cfgNode.getASTNode(); 
			lineNumber += fun.getEndLineno() - fun.getBaseLineno();
			
		}
		
		return lineNumber;
		
	}
	
	private int getLineNumberOfASTNode(Node node) {

		// Get the line number
		int lineNumber = node.getLineno();
		if(lineNumber < 0) lineNumber = 1;

		// Find the parent, to see if it's in a script
		Node parent = node;
		while(parent.getParent() != null) parent = parent.getParent();

		// If it's in an HTML file, add to the line number the offset of the script in the HTML file.
		if(parent instanceof ScriptOrFnNode) {
			
			if(htmlScriptLineNumbers.containsKey(parent))
				lineNumber += htmlScriptLineNumbers.get(parent);

		}
		
		return lineNumber;
		
	}
	
	@Deprecated
	private class HTML {
	
		private OutputStreamWriter fos; 
		
		public HTML(String location) throws IOException {

			File file= new File(location);
			if(file.exists()) file.delete();
			else file.createNewFile();

			fos = new OutputStreamWriter(new FileOutputStream(file), "UTF8");
			
		}
		
		public void close() throws IOException { fos.close(); }
		public void write(String s) throws IOException { fos.append(s + "\n"); }
		public void p(String s) throws IOException { out(s); fos.append("<p>" + s + "</p>"); }
		public void h1(String s) throws IOException { out(s); fos.append("<h1>" + s + "</h1>"); }
		public void h2(String s) throws IOException { out(s); fos.append("<h2>" + s + "</h2>"); }
		public void result(String s) throws IOException { out(s); fos.append("<p class=result>" + s + "</p>"); }
		public void a(String location, String label) throws IOException {
			
			fos.append("<a onclick='highlight(\"" + location + "\");' href='#" + location + "'>" + label + "</a>");
			
		}
		
	}

	private static String location(String path) {
		
		return path.replace(File.separatorChar, LINE_PATH_SEPARATOR).replace(".", Character.toString(LINE_PATH_SEPARATOR));
		
	}
	
	private static String line(CFGNode node) {
		
		return line(node.getFunction().getFunctionNode().getSourceName(), node.getFunction().getFeedlack().getLineNumberOfNode(node));		
		
	}
	
	private static String line(CFGFunction function) {
		
		return line(function.getFunctionNode().getSourceName(), function.getFeedlack().getLineNumberOfFunction(function));
		
	}
	
	private static String line(String path, int line) {
		
		return location(path) + LINE_PATH_SEPARATOR + line;
		
	}
	
	private static String object(String ... properties) {

		StringBuilder object = new StringBuilder();
		object.append("{\n");
		for(int i = 0; i < properties.length; i++) {
			object.append(properties[i] + (i == properties.length - 1 ? "\n" : ",\n"));
		}
		object.append("}\n");
		return object.toString();
		
	}
	
	private static String property(String name, String value) {
		
		return name + ": " + value;
		
	}
	
	private static String array(List<String> values) {
		
		StringBuilder object = new StringBuilder();
		object.append("[\n");
		for(int i = 0; i < values.size(); i++) {
			object.append(values.get(i) + (i == values.size() - 1 ? "\n" : ",\n"));
		}
		object.append("]\n");
		return object.toString();
		
	}
	
	private static String string(String string) { 
		
		string = string.replaceAll("\"", "\\\"");
		string = string.replaceAll("\\\\", "");
		
		return "\"" + string + "\""; 
		
	}
	
	private static String decision(Decision decision) {
		
		Assumption ass = decision.getAssumption();
		if(ass == null || ass == Assumption.NONE) ass = null;

		return object(
			property("kind", string(decision.getKind())),
			property("isNative", Boolean.toString(decision.isNative())),
			property("decision", string(decision.getDecision())),
			property("reason", string(decision.getReason())),
			property("assumption", assumption(ass)),
			property("likelihood", string(decision.getLikelihood().toString())),
			property("functionName", string(decision.getNode().getFunction().getDescriptionOfFunction())),
			property("location", string(line(decision.getNode())))
		);

	}
	
	private static String assumption(Assumption ass) {

		if(ass == null) return "null";
		
		List<String> locations = new Vector<String>();
		for(CFGNode node : ass.getLocations()) {
			
			// How do we describe this node? A call? An enter? Function names?
			String description = node.getCodeString();
			if(node instanceof CFGCallNode)
				description = ((CFGCallNode)node).getFunctionCalled() + "()";
			else if(node instanceof CFGAssignNode)
				description = "assignment to " + ((CFGAssignNode)node).getNameAssigned();
			else if(node instanceof CFGEnterNode)
				description = "" + ((CFGEnterNode)node).getFunction().getDescriptionOfFunction();
			
			locations.add(
				object(
					property("description", string(description)),
					property("location", string(line(node)))
				)
			);
		}
		
		return object(
			property("message", string(ass.getMessage())),
			property("locations", array(locations))
		);
		
	}
		
}