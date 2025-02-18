/* 
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Mike Ang
 *   Igor Bukanov
 *   Yuh-Ruey Chen
 *   Ethan Hugg
 *   Bob Jervis
 *   Terry Lucas
 *   Mike McCabe
 *   Milen Nankov
 *   Pascal-Louis Perez
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package com.google.javascript.rhino;

import com.google.javascript.rhino.jstype.JSTypeRegistry;

import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;
import java.util.List;

/**
 * This class implements the JavaScript parser.
 *
 * It is based on the C source files jsparse.c and jsparse.h
 * in the jsref package.
 *
 * @see TokenStream
 *
*
*
 */

public class Parser
{
    // TokenInformation flags : currentFlaggedToken stores them together
    // with token type
    final static int
        CLEAR_TI_MASK  = 0xFFFF,   // mask to clear token information bits
        TI_AFTER_EOL   = 1 << 16,  // first token of the source line
        TI_CHECK_LABEL = 1 << 17;  // indicates to check for label

    CompilerEnvirons compilerEnv;
    private JSTypeRegistry typeRegistry;
    private ErrorReporter errorReporter;
    private String sourceURI;
    boolean calledByCompileFunction;

    private TokenStream ts;
    private int currentFlaggedToken;
    private int syntaxErrorCount;

    private IRFactory nf;

    private int nestingOfFunction;

    private Decompiler decompiler;

    // The following are per function variables and should be saved/restored
    // during function parsing.
    // XXX Move to separated class?
    ScriptOrFnNode currentScriptOrFn;
    private int nestingOfWith;
    private Hashtable<String, Node> labelSet; // map of label names into nodes
    private ObjArray loopSet;
    private ObjArray loopAndSwitchSet;
    private boolean hasReturnValue;
    private int functionEndFlags;
// end of per function variables

    // Exception to unwind
    private static class ParserException extends RuntimeException
    {
        static final long serialVersionUID = 5882582646773765630L;
    }

    /**
     * Parses JavaScript source in a new {@link Context} with the default
     * {@link CompilerEnvirons}. Ignores parse warnings.
     *
     * @param sourceString JavaScript source code
     * @param sourceURI the source code URI or file name
     * @return a {@link Node} representing the parsed program (never null)
     * @throws RhinoException if there are any errors during the parse
     */
    public static ScriptOrFnNode parse(String sourceString, String sourceURI)
    {
        return parse(sourceString, sourceURI, false);
    }

    /**
     * Parses JavaScript source plus JSDoc annotations in a new {@link Context}.
     * Ignores parse warnings. Use {@link #parse(String,String)} to ignore
     * JSDoc.
     *
     * @param sourceString JavaScript source code
     * @param sourceURI the source code URI or file name
     * @return a {@link Node} representing the parsed program (never null)
     * @throws RhinoException if there are any errors during the parse
     */
    public static ScriptOrFnNode parseWithJSDoc(
        String sourceString, String sourceURI)
    {
        return parse(sourceString, sourceURI, true);
    }

    private static ScriptOrFnNode parse(String sourceString, String sourceURI,
                                        boolean parseJSDoc) {
        Context cx = Context.enter();
        SimpleErrorReporter errorReporter = new SimpleErrorReporter();
        cx.setErrorReporter(errorReporter);
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.initFromContext(cx);
        if (parseJSDoc) {
          compilerEnv.setParseJSDoc(true);
        }
        Parser p = new Parser(compilerEnv, errorReporter);
        ScriptOrFnNode root = null;
        try {
            root = p.parse(sourceString, sourceURI, 1);
        } catch (EvaluatorException e) {
            errorReporter.error(e.details(), e.sourceName(), e.lineNumber(),
                                e.lineSource(), e.lineNumber());
        } finally {
            Context.exit();
        }

        List<String> errors = errorReporter.errors();
        if (errors != null) {
            StringBuilder message = new StringBuilder();
            for (String error : errors) {
                if (message.length() > 0) {
                    message.append('\n');
                }
                message.append(error);
            }
            throw new RhinoException(message.toString());
        }

        return root;
    }

    public Parser(CompilerEnvirons compilerEnv, ErrorReporter errorReporter,
                  JSTypeRegistry typeRegistry)
    {
        this.compilerEnv = compilerEnv;
        this.errorReporter = errorReporter;
        this.typeRegistry = typeRegistry;
    }

    public Parser(CompilerEnvirons compilerEnv, ErrorReporter errorReporter)
    {
        this(compilerEnv, errorReporter, null);
    }

    Decompiler createDecompiler(CompilerEnvirons compilerEnv)
    {
        return new Decompiler();
    }

    void addStrictWarning(String messageId, String messageArg)
    {
        if (compilerEnv.isStrictMode())
            addWarning(messageId, messageArg);
    }

    void addWarning(String messageId)
    {
        reportWarning(ScriptRuntime.getMessage0(messageId),
                      ts.getLineno(), ts.getOffset(), ts.getLine());
    }

    void addWarning(String messageId, String messageArg)
    {
        reportWarning(ScriptRuntime.getMessage1(messageId, messageArg),
                      ts.getLineno(), ts.getOffset(), ts.getLine());
    }

    void addWarning(String messageId, int lineno, int charno)
    {
      reportWarning(ScriptRuntime.getMessage0(messageId), lineno, charno, null);
    }

    void addWarning(String messageId, String messageArg, int lineno, int charno)
    {
        reportWarning(ScriptRuntime.getMessage1(messageId, messageArg),
                      lineno, charno, null);
    }

    /**
     * Add a warning.
     * @param message the message id
     * @param lineno the line number at which the warning occurred
     * @param charno the character number at which the warning occurred
     * @param code the code excerpt, it may be {@code null}
     */
    private void reportWarning(
        String message, int lineno, int charno, String code) {
      if (compilerEnv.reportWarningAsError()) {
        ++syntaxErrorCount;
        errorReporter.error(message, sourceURI, lineno, code, charno);
      } else
        errorReporter.warning(message, sourceURI, lineno, code, charno);
    }

    void addError(String messageId)
    {
        ++syntaxErrorCount;
        String message = ScriptRuntime.getMessage0(messageId);
        errorReporter.error(message, sourceURI, ts.getLineno(),
                            ts.getLine(), ts.getOffset());
    }

    void addError(String messageId, String messageArg)
    {
        ++syntaxErrorCount;
        String message = ScriptRuntime.getMessage1(messageId, messageArg);
        errorReporter.error(message, sourceURI, ts.getLineno(),
                            ts.getLine(), ts.getOffset());
    }

    RuntimeException reportError(String messageId)
    {
        addError(messageId);

        // Throw a ParserException exception to unwind the recursive descent
        // parse.
        throw new ParserException();
    }

    private int peekToken()
        throws IOException
    {
        int tt = currentFlaggedToken;
        if (tt == Token.EOF) {
            tt = ts.getToken();
            if (tt == Token.EOL) {
                do {
                    tt = ts.getToken();
                } while (tt == Token.EOL);
                tt |= TI_AFTER_EOL;
            }
            currentFlaggedToken = tt;
        }
        return tt & CLEAR_TI_MASK;
    }

    private int peekFlaggedToken()
        throws IOException
    {
        peekToken();
        return currentFlaggedToken;
    }

    private void consumeToken()
    {
        currentFlaggedToken = Token.EOF;
    }

    private int nextToken()
        throws IOException
    {
        int tt = peekToken();
        consumeToken();
        return tt;
    }

    private int nextFlaggedToken()
        throws IOException
    {
        peekToken();
        int ttFlagged = currentFlaggedToken;
        consumeToken();
        return ttFlagged;
    }

    private boolean matchToken(int toMatch)
        throws IOException
    {
        int tt = peekToken();
        if (tt != toMatch) {
            return false;
        }
        consumeToken();
        return true;
    }

    private int peekTokenOrEOL()
        throws IOException
    {
        int tt = peekToken();
        // Check for last peeked token flags
        if ((currentFlaggedToken & TI_AFTER_EOL) != 0) {
            tt = Token.EOL;
        }
        return tt;
    }

    private void setCheckForLabel()
    {
        if ((currentFlaggedToken & CLEAR_TI_MASK) != Token.NAME)
            throw Kit.codeBug();
        currentFlaggedToken |= TI_CHECK_LABEL;
    }

    private void mustMatchToken(int toMatch, String messageId)
        throws IOException, ParserException
    {
        if (!matchToken(toMatch)) {
            reportError(messageId);
        }
    }

    private void mustHaveXML()
    {
        if (!compilerEnv.isXmlAvailable()) {
            reportError("msg.XML.not.available");
        }
    }

    boolean insideFunction()
    {
        return nestingOfFunction != 0;
    }

    private Node enterSwitch(Node switchSelector, int lineno, int charno)
    {
        Node switchNode = nf.createSwitch(lineno, charno);
        switchNode.addChildToBack(switchSelector);
        if (loopAndSwitchSet == null) {
            loopAndSwitchSet = new ObjArray();
        }
        loopAndSwitchSet.push(switchNode);
        return switchNode;
    }

    private void exitSwitch()
    {
        loopAndSwitchSet.pop();
    }

    public TokenStream initForUnitTest(Reader sourceReader,
                                       String sourceURI, int lineno,
                                       boolean parseJSDoc)
    {
      this.sourceURI = sourceURI;
      this.ts = new TokenStream(this, sourceReader, null, lineno);
      return ts;
    }

    /*
     * Build a parse tree from the given sourceString.
     *
     * @return an Object representing the parsed
     * program.  If the parse fails, null will be returned.  (The
     * parse failure will result in a call to the ErrorReporter from
     * CompilerEnvirons.)
     */
    public ScriptOrFnNode parse(String sourceString,
                                String sourceURI, int lineno)
    {
        this.sourceURI = sourceURI;
        this.ts = new TokenStream(this, null, sourceString, lineno);
        try {
            return parse();
        } catch (IOException ex) {
            // Should never happen
            throw new IllegalStateException();
        }
    }

    /*
     * Gets the type registry, initializing it on the first call.
     * @return a type registry
     */
    public JSTypeRegistry getTypeRegistry()
    {
        if (typeRegistry == null) {
            typeRegistry = new JSTypeRegistry(errorReporter);
        }
        return typeRegistry;
    }

    /*
     * Build a parse tree from the given sourceString.
     *
     * @return an Object representing the parsed
     * program.  If the parse fails, null will be returned.  (The
     * parse failure will result in a call to the ErrorReporter from
     * CompilerEnvirons.)
     */
    public ScriptOrFnNode parse(Reader sourceReader,
                                String sourceURI, int lineno)
        throws IOException
    {
        this.sourceURI = sourceURI;
        this.ts = new TokenStream(this, sourceReader, null, lineno);
        return parse();
    }

    private ScriptOrFnNode parse()
        throws IOException
    {
        this.decompiler = createDecompiler(compilerEnv);
        this.nf = new IRFactory(this);
        currentScriptOrFn = nf.createScript();
        int sourceStartOffset = decompiler.getCurrentOffset();
        decompiler.addToken(Token.SCRIPT);

        this.currentFlaggedToken = Token.EOF;
        this.syntaxErrorCount = 0;
        ts.setFileLevelJsDocBuilder(currentScriptOrFn.getJsDocBuilderForNode());

        int baseLineno = ts.getLineno();  // line number where source starts
        int baseCharno = ts.getCharno();

        /* so we have something to add nodes to until
         * we've collected all the source */
        Node pn = nf.createLeaf(Token.BLOCK, baseLineno, baseCharno);

        try {
            for (;;) {
                int tt = peekToken();

                if (tt <= Token.EOF) {
                    break;
                }

                Node n;
                if (tt == Token.FUNCTION) {
                    consumeToken();
                    try {
                        n = function(calledByCompileFunction
                                     ? FunctionNode.FUNCTION_EXPRESSION
                                     : FunctionNode.FUNCTION_STATEMENT);
                    } catch (ParserException e) {
                        break;
                    }
                } else {
                    n = statement();
                }
                nf.addChildToBack(pn, n);
            }
        } catch (StackOverflowError ex) {
            String msg = ScriptRuntime.getMessage0(
                "msg.too.deep.parser.recursion");
            throw Context.reportRuntimeError(msg, sourceURI,
                                             ts.getLineno(), null, 0);
        }

        if (this.syntaxErrorCount != 0) {
            String msg = String.valueOf(this.syntaxErrorCount);
            msg = ScriptRuntime.getMessage1("msg.got.syntax.errors", msg);
            throw errorReporter.runtimeError(msg, sourceURI, baseLineno,
                                             null, 0);
        }

        currentScriptOrFn.setSourceName(sourceURI);
        currentScriptOrFn.setBaseLineno(baseLineno);
        currentScriptOrFn.setEndLineno(ts.getLineno());

        // Attach the @fileoverview jsdoc info if @preserve info
        // hasn't already been attached.
        if (currentScriptOrFn.getJSDocInfo() == null) {
            currentScriptOrFn.setJSDocInfo(ts.getFileOverviewJSDocInfo());
        }

        int sourceEndOffset = decompiler.getCurrentOffset();
        currentScriptOrFn.setEncodedSourceBounds(sourceStartOffset,
                                                 sourceEndOffset);

        nf.initScript(currentScriptOrFn, pn);
        currentScriptOrFn.setIsSyntheticBlock(true);

        this.decompiler = null; // It helps GC

        return currentScriptOrFn;
    }

    /*
     * The C version of this function takes an argument list,
     * which doesn't seem to be needed for tree generation...
     * it'd only be useful for checking argument hiding, which
     * I'm not doing anyway...
     */
    private Node parseFunctionBody()
        throws IOException
    {
        ++nestingOfFunction;
        Node pn = nf.createBlock(ts.getLineno(), ts.getCharno());
        try {
            bodyLoop: for (;;) {
                Node n;
                int tt = peekToken();
                switch (tt) {
                  case Token.ERROR:
                  case Token.EOF:
                  case Token.RC:
                    break bodyLoop;

                  case Token.FUNCTION:
                    consumeToken();
                    n = function(FunctionNode.FUNCTION_STATEMENT);
                    break;
                  default:
                    n = statement();
                    break;
                }
                nf.addChildToBack(pn, n);
            }
        } catch (ParserException e) {
            // Ignore it
        } finally {
            --nestingOfFunction;
        }

        return pn;
    }

    private Node function(int functionType)
        throws IOException, ParserException
    {
        int syntheticType = functionType;
        int baseLineno = ts.getLineno();  // line number where source starts
        int baseCharno = ts.getCharno();  // char number where source starts

        int functionSourceStart = decompiler.markFunctionStart(functionType);
        String name;
        Node memberExprNode = null;
        int nameLineno, nameCharno;  // line/char where function name starts
        if (matchToken(Token.NAME)) {
            name = ts.getString();
            nameLineno = ts.getLineno();
            nameCharno = ts.getCharno();
            decompiler.addName(name);
            if (!matchToken(Token.LP)) {
                if (compilerEnv.isAllowMemberExprAsFunctionName()) {
                    // Extension to ECMA: if 'function <name>' does not follow
                    // by '(', assume <name> starts memberExpr
                    Node memberExprHead =
                        nf.createName(name, nameLineno, nameCharno);
                    name = "";
                    memberExprNode = memberExprTail(false, memberExprHead);
                }
                mustMatchToken(Token.LP, "msg.no.paren.parms");
            }
        } else {
            // Anonymous function
            name = "";
            nameLineno = ts.getLineno();
            nameCharno = ts.getCharno();
            if (!matchToken(Token.LP)) {
                if (compilerEnv.isAllowMemberExprAsFunctionName()) {
                    // Note that memberExpr can not start with '(' like
                    // in function (1+2).toString(), because 'function ('
                    // is already processed as anonymous function
                    memberExprNode = memberExpr(false);
                }
                mustMatchToken(Token.LP, "msg.no.paren.parms");
            }
        }

        if (memberExprNode != null) {
            syntheticType = FunctionNode.FUNCTION_EXPRESSION;
        }

        boolean nested = insideFunction();

        FunctionNode fnNode =
            nf.createFunction(name, nameLineno, nameCharno);
        if (nested || nestingOfWith > 0) {
            // 1. Nested functions are not affected by the dynamic scope flag
            // as dynamic scope is already a parent of their scope.
            // 2. Functions defined under the with statement also immune to
            // this setup, in which case dynamic scope is ignored in favor
            // of with object.
            fnNode.itsIgnoreDynamicScope = true;
        }

        int functionIndex = currentScriptOrFn.addFunction(fnNode);

        int functionSourceEnd;

        ScriptOrFnNode savedScriptOrFn = currentScriptOrFn;
        currentScriptOrFn = fnNode;
        int savedNestingOfWith = nestingOfWith;
        nestingOfWith = 0;
        Hashtable<String, Node> savedLabelSet = labelSet;
        labelSet = null;
        ObjArray savedLoopSet = loopSet;
        loopSet = null;
        ObjArray savedLoopAndSwitchSet = loopAndSwitchSet;
        loopAndSwitchSet = null;
        boolean savedHasReturnValue = hasReturnValue;
        int savedFunctionEndFlags = functionEndFlags;

        Node args;
        Node body;
        JSDocInfo info = ts.getAndResetJSDocInfo();

        try {
            decompiler.addToken(Token.LP);
            args = nf.createLeaf(Token.LP, ts.getLineno(), ts.getCharno());
            if (!matchToken(Token.RP)) {
                boolean first = true;
                do {
                    if (!first)
                        decompiler.addToken(Token.COMMA);
                    first = false;
                    mustMatchToken(Token.NAME, "msg.no.parm");
                    String s = ts.getString();
                    nf.addChildToBack(args,
                        nf.createName(s, ts.getLineno(), ts.getCharno()));
                    decompiler.addName(s);
                } while (matchToken(Token.COMMA));

                mustMatchToken(Token.RP, "msg.no.paren.after.parms");
            }
            decompiler.addToken(Token.RP);

            mustMatchToken(Token.LC, "msg.no.brace.body");
            decompiler.addEOL(Token.LC);
            body = parseFunctionBody();
            mustMatchToken(Token.RC, "msg.no.brace.after.body");

            decompiler.addToken(Token.RC);
            functionSourceEnd = decompiler.markFunctionEnd(functionSourceStart);
            if (functionType != FunctionNode.FUNCTION_EXPRESSION) {
                 if (compilerEnv.getLanguageVersion() >= Context.VERSION_1_2) {
                    // function f() {} function g() {} is not allowed in 1.2
                    // or later but for compatibility with old scripts
                    // the check is done only if language is
                    // explicitly set.
                    //  XXX warning needed if version == VERSION_DEFAULT ?
                    int tt = peekTokenOrEOL();
                    if (tt == Token.FUNCTION) {
                         reportError("msg.no.semi.stmt");
                    }
                 }
                // Add EOL only if function is not part of expression
                // since it gets SEMI + EOL from Statement in that case
                decompiler.addToken(Token.EOL);
            }

        }
        finally {
            hasReturnValue = savedHasReturnValue;
            functionEndFlags = savedFunctionEndFlags;
            loopAndSwitchSet = savedLoopAndSwitchSet;
            loopSet = savedLoopSet;
            labelSet = savedLabelSet;
            nestingOfWith = savedNestingOfWith;
            currentScriptOrFn = savedScriptOrFn;
        }

        fnNode.setEncodedSourceBounds(functionSourceStart, functionSourceEnd);
        fnNode.setSourceName(sourceURI);
        fnNode.setBaseLineno(baseLineno);
        fnNode.setEndLineno(ts.getLineno());

        Node pn = nf.initFunction(fnNode, functionIndex, args, info,
                                  body, syntheticType);
        if (memberExprNode != null) {
            pn = nf.createAssignment(Token.ASSIGN, memberExprNode, pn,
                baseLineno, baseCharno);
            if (functionType != FunctionNode.FUNCTION_EXPRESSION) {
                // XXX check JScript behavior: should it be createExprStatement?
                pn = nf.createExprStatementNoReturn(pn, baseLineno, baseCharno);
            }
        }
        return pn;
    }

    private Node statements()
        throws IOException
    {
        Node pn = nf.createBlock(ts.getLineno(), ts.getCharno());

        int tt;
        while((tt = peekToken()) > Token.EOF && tt != Token.RC) {
            nf.addChildToBack(pn, statement());
        }

        return pn;
    }

    private Node condition()
        throws IOException, ParserException
    {
        mustMatchToken(Token.LP, "msg.no.paren.cond");
        decompiler.addToken(Token.LP);
        Node pn = expr(false);
        mustMatchToken(Token.RP, "msg.no.paren.after.cond");
        decompiler.addToken(Token.RP);

        // Report strict warning on code like "if (a = 7) ...". Suppress the
        // warning if the condition is parenthesized, like "if ((a = 7)) ...".
        if (pn.getProp(Node.PARENTHESIZED_PROP) == null &&
            (pn.getType() == Token.SETNAME || pn.getType() == Token.SETPROP ||
             pn.getType() == Token.SETELEM))
        {
            addStrictWarning("msg.equal.as.assign", "");
        }
        return pn;
    }

    // match a NAME; return null if no match.
    private String matchLabel()
        throws IOException, JavaScriptException
    {
        int lineno = ts.getLineno();

        String label = null;
        int tt = peekTokenOrEOL();
        if (tt == Token.NAME) {
            consumeToken();
            label = ts.getString();
            decompiler.addName(label);
            Node n = null;
            if (labelSet != null) {
                n = labelSet.get(label);
            }
            if (n == null) {
                reportError("msg.undef.label");
            }
        }
        return label;
    }

    private Node statement()
        throws IOException
    {
        try {
            Node pn = statementHelper(null);
            if (pn != null) {
                if (compilerEnv.isStrictMode() && !pn.hasSideEffects())
                    addStrictWarning("msg.no.side.effects", "");
                return pn;
            }
        } catch (ParserException e) { }

        // skip to end of statement
        int lineno = ts.getLineno();
        int charno = ts.getCharno();
        guessingStatementEnd: for (;;) {
            int tt = peekTokenOrEOL();
            consumeToken();
            switch (tt) {
              case Token.ERROR:
              case Token.EOF:
              case Token.EOL:
              case Token.SEMI:
                break guessingStatementEnd;
            }
        }
        return nf.createExprStatement(nf.createErrorName(), lineno, charno);
    }

    /**
     * Whether the "catch (e: e instanceof Exception) { ... }" syntax
     * is implemented.
     */

    private Node statementHelper(Node statementLabel)
        throws IOException, ParserException
    {
        Node pn = null;

        int tt;

        tt = peekToken();

        switch(tt) {
          case Token.IF: {
            consumeToken();

            decompiler.addToken(Token.IF);
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            Node cond = condition();
            decompiler.addEOL(Token.LC);
            Node ifTrue = statement();
            Node ifFalse = null;
            if (matchToken(Token.ELSE)) {
                decompiler.addToken(Token.RC);
                decompiler.addToken(Token.ELSE);
                decompiler.addEOL(Token.LC);
                ifFalse = statement();
            }
            decompiler.addEOL(Token.RC);
            pn = nf.createIf(cond, ifTrue, ifFalse, lineno, charno);
            return pn;
        }

          case Token.SWITCH: {
            consumeToken();

            decompiler.addToken(Token.SWITCH);
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            mustMatchToken(Token.LP, "msg.no.paren.switch");
            decompiler.addToken(Token.LP);
            pn = enterSwitch(expr(false), lineno, charno);
            try {
                mustMatchToken(Token.RP, "msg.no.paren.after.switch");
                decompiler.addToken(Token.RP);
                mustMatchToken(Token.LC, "msg.no.brace.switch");
                decompiler.addEOL(Token.LC);

                boolean hasDefault = false;
                switchLoop: for (;;) {
                    tt = nextToken();
                    lineno = ts.getLineno();
                    charno = ts.getCharno();
                    Node caseExpression;
                    switch (tt) {
                      case Token.RC:
                        break switchLoop;

                      case Token.CASE:
                        decompiler.addToken(Token.CASE);
                        caseExpression = expr(false);
                        mustMatchToken(Token.COLON, "msg.no.colon.case");
                        decompiler.addEOL(Token.COLON);
                        break;

                      case Token.DEFAULT:
                        if (hasDefault) {
                            reportError("msg.double.switch.default");
                        }
                        decompiler.addToken(Token.DEFAULT);
                        hasDefault = true;
                        caseExpression = null;
                        mustMatchToken(Token.COLON, "msg.no.colon.case");
                        decompiler.addEOL(Token.COLON);
                        break;

                      default:
                        reportError("msg.bad.switch");
                        break switchLoop;
                    }

                    Node block = nf.createLeaf(Token.BLOCK, lineno, charno);
                    block.setIsSyntheticBlock(true);
                    while ((tt = peekToken()) != Token.RC
                           && tt != Token.CASE
                           && tt != Token.DEFAULT
                           && tt != Token.EOF)
                    {
                        nf.addChildToBack(block, statement());
                    }

                    // caseExpression == null => add default lable
                    nf.addSwitchCase(pn, caseExpression, block, lineno, charno);
                }
                decompiler.addEOL(Token.RC);
                nf.closeSwitch(pn);
            } finally {
                exitSwitch();
            }
            return pn;
          }

          case Token.WHILE: {
            consumeToken();
            decompiler.addToken(Token.WHILE);

            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            Node cond = condition();
            decompiler.addEOL(Token.LC);
            Node body = statement();
            decompiler.addEOL(Token.RC);
            pn = nf.createWhile(cond, body, lineno, charno);
            return pn;
          }

          case Token.DO: {
            consumeToken();
            decompiler.addToken(Token.DO);
            decompiler.addEOL(Token.LC);

            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            Node body = statement();
            decompiler.addToken(Token.RC);
            mustMatchToken(Token.WHILE, "msg.no.while.do");
            decompiler.addToken(Token.WHILE);
            Node cond = condition();

            pn = nf.createDoWhile(body, cond, lineno, charno);
            // Always auto-insert semicon to follow SpiderMonkey:
            // It is required by ECMAScript but is ignored by the rest of
            // world, see bug 238945
            matchToken(Token.SEMI);
            decompiler.addEOL(Token.SEMI);
            return pn;
          }

          case Token.FOR: {
            consumeToken();
            boolean isForEach = false;
            decompiler.addToken(Token.FOR);
            int lineno = ts.getLineno();
            int charno = ts.getCharno();

            Node init;  // Node init is also foo in 'foo in Object'
            Node cond;  // Node cond is also object in 'foo in Object'
            Node incr = null; // to kill warning
            Node body;

            // See if this is a for each () instead of just a for ()
            if (matchToken(Token.NAME)) {
                decompiler.addName(ts.getString());
                if (ts.getString().equals("each")) {
                    isForEach = true;
                } else {
                    reportError("msg.no.paren.for");
                }
            }

            mustMatchToken(Token.LP, "msg.no.paren.for");
            decompiler.addToken(Token.LP);
            tt = peekToken();
            if (tt == Token.SEMI) {
                init = nf.createLeaf(Token.EMPTY,
                        ts.getLineno(), ts.getCharno());
            } else {
                if (tt == Token.VAR) {
                    // set init to a var list or initial
                    consumeToken();    // consume the 'var' token
                    init = variables(Token.FOR);
                }
                else {
                    init = expr(true);
                }
            }

            if (matchToken(Token.IN)) {
                decompiler.addToken(Token.IN);
                // 'cond' is the object over which we're iterating
                cond = expr(false);
            } else {  // ordinary for loop
                mustMatchToken(Token.SEMI, "msg.no.semi.for");
                decompiler.addToken(Token.SEMI);
                if (peekToken() == Token.SEMI) {
                    // no loop condition
                    cond = nf.createLeaf(Token.EMPTY,
                            ts.getLineno(), ts.getCharno());
                } else {
                    cond = expr(false);
                }

                mustMatchToken(Token.SEMI, "msg.no.semi.for.cond");
                decompiler.addToken(Token.SEMI);
                if (peekToken() == Token.RP) {
                    incr = nf.createLeaf(Token.EMPTY,
                            ts.getLineno(), ts.getCharno());
                } else {
                    incr = expr(false);
                }
            }

            mustMatchToken(Token.RP, "msg.no.paren.for.ctrl");
            decompiler.addToken(Token.RP);
            decompiler.addEOL(Token.LC);
            body = statement();
            decompiler.addEOL(Token.RC);

            if (incr == null) {
                // cond could be null if 'in obj' got eaten by the init node.
                pn = nf.createForIn(init, cond, body, lineno, charno);
            } else {
                pn = nf.createFor(init, cond, incr, body, lineno, charno);
            }
            return pn;
          }

          case Token.TRY: {
            consumeToken();
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            Node tryblock;
            Node catchblocks = null;
            Node finallyblock = null;

            // Pull out JSDoc info and reset it before recursing.
            JSDocInfo info = ts.getAndResetJSDocInfo();


            decompiler.addToken(Token.TRY);
            decompiler.addEOL(Token.LC);
            tryblock = statement();
            decompiler.addEOL(Token.RC);

            catchblocks = nf.createLeaf(Token.BLOCK,
                    ts.getLineno(), ts.getCharno());

            boolean sawDefaultCatch = false;
            int peek = peekToken();
            if (peek == Token.CATCH) {
                while (matchToken(Token.CATCH)) {
                    int catchLineno = ts.getLineno();
                    int catchCharno = ts.getCharno();

                    if (sawDefaultCatch) {
                        reportError("msg.catch.unreachable");
                    }
                    decompiler.addToken(Token.CATCH);
                    mustMatchToken(Token.LP, "msg.no.paren.catch");
                    decompiler.addToken(Token.LP);

                    mustMatchToken(Token.NAME, "msg.bad.catchcond");
                    String varName = ts.getString();
                    int nameLineno = ts.getLineno();
                    int nameCharno = ts.getCharno();
                    decompiler.addName(varName);

                    Node catchCond = null;
                    if (matchToken(Token.IF)) {
                        decompiler.addToken(Token.IF);
                        catchCond = expr(false);
                    } else {
                        sawDefaultCatch = true;
                    }

                    mustMatchToken(Token.RP, "msg.bad.catchcond");
                    decompiler.addToken(Token.RP);
                    mustMatchToken(Token.LC, "msg.no.brace.catchblock");
                    decompiler.addEOL(Token.LC);

                    nf.addChildToBack(catchblocks,
                        nf.createCatch(varName, nameLineno, nameCharno, catchCond,
                             statements(), catchLineno, catchCharno));

                    mustMatchToken(Token.RC, "msg.no.brace.after.body");
                    decompiler.addEOL(Token.RC);
                }
            } else if (peek != Token.FINALLY) {
                mustMatchToken(Token.FINALLY, "msg.try.no.catchfinally");
            }

            if (matchToken(Token.FINALLY)) {
                decompiler.addToken(Token.FINALLY);
                decompiler.addEOL(Token.LC);
                finallyblock = statement();
                decompiler.addEOL(Token.RC);
            }

            pn = nf.createTryCatchFinally(tryblock, catchblocks,
                                          finallyblock, lineno, charno);
            if (info != null) {
                pn.setJSDocInfo(info);
            }

            return pn;
          }

          case Token.THROW: {
            consumeToken();
            if (peekTokenOrEOL() == Token.EOL) {
                // ECMAScript does not allow new lines before throw expression,
                // see bug 256617
                reportError("msg.bad.throw.eol");
            }

            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addToken(Token.THROW);
            pn = nf.createThrow(expr(false), lineno, charno);
            break;
          }

          case Token.BREAK: {
            consumeToken();
            int lineno = ts.getLineno();
            int charno = ts.getCharno();

            decompiler.addToken(Token.BREAK);

            // matchLabel only matches if there is one
            String label = matchLabel();
            if (label != null) {
                decompiler.addToken(Token.NAME);
                decompiler.addName(label);
            }
            pn = nf.createBreak(label, lineno, charno);
            break;
          }

          case Token.CONTINUE: {
            consumeToken();
            int lineno = ts.getLineno();
            int charno = ts.getCharno();

            decompiler.addToken(Token.CONTINUE);

            // matchLabel only matches if there is one
            String label = matchLabel();
            if (label != null) {
                decompiler.addToken(Token.NAME);
                decompiler.addName(label);
            }
            pn = nf.createContinue(label, lineno, charno);
            break;
          }

          case Token.DEBUGGER: {
            consumeToken();
            int lineno = ts.getLineno();
            int charno = ts.getCharno();

            decompiler.addToken(Token.DEBUGGER);

            pn = nf.createDebugger(lineno, charno);
            break;
          }

          case Token.WITH: {
            consumeToken();

            decompiler.addToken(Token.WITH);
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            mustMatchToken(Token.LP, "msg.no.paren.with");
            decompiler.addToken(Token.LP);
            Node obj = expr(false);
            mustMatchToken(Token.RP, "msg.no.paren.after.with");
            decompiler.addToken(Token.RP);
            decompiler.addEOL(Token.LC);

            ++nestingOfWith;
            Node body;
            try {
                body = statement();
            } finally {
                --nestingOfWith;
            }

            decompiler.addEOL(Token.RC);

            pn = nf.createWith(obj, body, lineno, charno);
            return pn;
          }

          case Token.CONST:
          case Token.VAR: {
            consumeToken();
            pn = variables(tt);
            break;
          }

          case Token.RETURN: {
            if (!insideFunction()) {
            	// TODO Andy did this.
//                reportError("msg.bad.return");
            }
            consumeToken();
            decompiler.addToken(Token.RETURN);
            int lineno = ts.getLineno();
            int charno = ts.getCharno();

            Node retExpr;
            /* This is ugly, but we don't want to require a semicolon. */
            tt = peekTokenOrEOL();
            switch (tt) {
              case Token.SEMI:
              case Token.RC:
              case Token.EOF:
              case Token.EOL:
              case Token.ERROR:
                retExpr = null;
                break;
              default:
                retExpr = expr(false);
                hasReturnValue = true;
            }
            pn = nf.createReturn(retExpr, lineno, charno);
            break;
          }

          case Token.LC:
            consumeToken();
            if (statementLabel != null) {
                decompiler.addToken(Token.LC);
            }
            pn = statements();
            mustMatchToken(Token.RC, "msg.no.brace.block");
            if (statementLabel != null) {
                decompiler.addEOL(Token.RC);
            }
            return pn;

          case Token.ERROR:
            // Fall thru, to have a node for error recovery to work on
          case Token.SEMI:
            consumeToken();
            pn = nf.createLeaf(Token.EMPTY, ts.getLineno(), ts.getCharno());
            return pn;

          case Token.FUNCTION: {
            consumeToken();
            pn = function(FunctionNode.FUNCTION_EXPRESSION_STATEMENT);
            return pn;
          }

          case Token.DEFAULT : {
            consumeToken();
            mustHaveXML();

            decompiler.addToken(Token.DEFAULT);
            int lineno = ts.getLineno();
            int charno = ts.getCharno();

            if (!(matchToken(Token.NAME)
                  && ts.getString().equals("xml")))
            {
                reportError("msg.bad.namespace");
            }
            decompiler.addName(" xml");

            if (!(matchToken(Token.NAME)
                  && ts.getString().equals("namespace")))
            {
                reportError("msg.bad.namespace");
            }
            decompiler.addName(" namespace");

            if (!matchToken(Token.ASSIGN)) {
                reportError("msg.bad.namespace");
            }
            decompiler.addToken(Token.ASSIGN);

            Node expr = expr(false);
            pn = nf.createDefaultNamespace(expr, lineno, charno);
            break;
          }

          case Token.NAME: {
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            String name = ts.getString();
            setCheckForLabel();
            pn = expr(false);
            if (pn.getType() != Token.LABEL) {
                pn = nf.createExprStatement(pn, lineno, charno);
            } else {
                // Parsed the label: push back token should be
                // colon that primaryExpr left untouched.
                if (peekToken() != Token.COLON) Kit.codeBug();
                consumeToken();
                // depend on decompiling lookahead to guess that that
                // last name was a label.
                decompiler.addName(name);
                decompiler.addEOL(Token.COLON);

                if (labelSet == null) {
                    labelSet = new Hashtable<String, Node>();
                } else if (labelSet.containsKey(name)) {
                    reportError("msg.dup.label");
                }

                labelSet.put(name, pn);
                pn = nf.createLabel(name, lineno, charno);
                try {
                    nf.addChildToBack(pn, statementHelper(pn));
                } finally {
                    labelSet.remove(name);
                }
                return pn;
            }
            break;
          }

        default: {
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            pn = expr(false);
            pn = nf.createExprStatement(pn, lineno, charno);
            break;
          }
        }

        int ttFlagged = peekFlaggedToken();
        switch (ttFlagged & CLEAR_TI_MASK) {
          case Token.SEMI:
            // Consume ';' as a part of expression
            consumeToken();
            break;
          case Token.ERROR:
          case Token.EOF:
          case Token.RC:
            // Autoinsert ;
            break;
          default:
            if ((ttFlagged & TI_AFTER_EOL) == 0) {
                // Report error if no EOL or autoinsert ; otherwise
// TODO This was me, not Google.
//                reportError("msg.no.semi.stmt");
            }
            break;
        }
        decompiler.addEOL(Token.SEMI);

        return pn;
    }

    /**
     * Parse a 'var' or 'const' statement, or a 'var' init list in a for
     * statement.
     * @param context A token value: either VAR, CONST or FOR depending on
     * context.
     * @return The parsed statement
     * @throws IOException
     * @throws ParserException
     */
    private Node variables(int context)
        throws IOException, ParserException
    {
        Node pn;
        boolean first = true;
        JSDocInfo varInfo = null;

        if (context == Token.CONST){
            pn = nf.createVariables(Token.CONST, ts.getLineno(),
                ts.getCharno());
            decompiler.addToken(Token.CONST);
        } else {
            pn = nf.createVariables(Token.VAR, ts.getLineno(), ts.getCharno());
            varInfo = ts.getAndResetJSDocInfo();
            if (varInfo != null) {
              pn.setJSDocInfo(varInfo);
            }
            decompiler.addToken(Token.VAR);
        }

        for (;;) {
            Node name;
            Node init;
            mustMatchToken(Token.NAME, "msg.bad.var");
            String s = ts.getString();
            int lineno = ts.getLineno();
            int charno = ts.getCharno();

            JSDocInfo info = ts.getAndResetJSDocInfo();

            if (!first)
                decompiler.addToken(Token.COMMA);
            first = false;

            decompiler.addName(s);

            if (context == Token.CONST) {
                if (!currentScriptOrFn.addConst(s)) {
                    // We know it's already defined, since addConst passes if
                    // it's not defined at all.  The addVar call just confirms
                    // what it is.
                    if (currentScriptOrFn.addVar(s) !=
                            ScriptOrFnNode.DUPLICATE_CONST)
                        addError("msg.var.redecl", s);
                    else
                        addError("msg.const.redecl", s);
                }
            } else {
                int dupState = currentScriptOrFn.addVar(s);
                if (dupState == ScriptOrFnNode.DUPLICATE_CONST)
                    addError("msg.const.redecl", s);
                else if (dupState == ScriptOrFnNode.DUPLICATE_PARAMETER)
                    addStrictWarning("msg.var.hides.arg", s);
                else if (dupState == ScriptOrFnNode.DUPLICATE_VAR)
                    addStrictWarning("msg.var.redecl", s);
            }
            name = nf.createTaggedName(s, info, lineno, charno);
            if (info != null && info.hasEnumParameterType()) {
                typeRegistry.identifyEnumName(s);
            }

            // omitted check for argument hiding

            if (matchToken(Token.ASSIGN)) {
                decompiler.addToken(Token.ASSIGN);

                init = assignExpr(context == Token.FOR);
                nf.addChildToBack(name, init);
            }
            nf.addChildToBack(pn, name);
            if (!matchToken(Token.COMMA))
                break;
        }
        if (varInfo != null && varInfo.hasEnumParameterType() &&
            pn.getChildCount() == 1) {
            typeRegistry.identifyEnumName(pn.getFirstChild().getString());
        }
        return pn;
    }

    private Node expr(boolean inForInit)
        throws IOException, ParserException
    {
        Node pn = assignExpr(inForInit);
        int lineno = ts.getLineno();
        int charno = ts.getCharno();
        while (matchToken(Token.COMMA)) {
            decompiler.addToken(Token.COMMA);
            if (compilerEnv.isStrictMode() && !pn.hasSideEffects())
                addStrictWarning("msg.no.side.effects", "");
            pn = nf.createBinary(Token.COMMA, pn, assignExpr(inForInit),
                lineno, charno);
        }
        return pn;
    }

    private Node assignExpr(boolean inForInit)
        throws IOException, ParserException
    {
        Node pn = condExpr(inForInit);

        int tt = peekToken();
        int lineno = ts.getLineno();
        int charno = ts.getCharno();
        if (Token.FIRST_ASSIGN <= tt && tt <= Token.LAST_ASSIGN) {
            consumeToken();

            // Pull out JSDoc info and reset it before recursing.
            JSDocInfo info = ts.getAndResetJSDocInfo();

            // omitted: "invalid assignment left-hand side" check.
            decompiler.addToken(tt);
            Node right = assignExpr(inForInit);
            pn = nf.createBinary(tt, pn, right, lineno, charno);
            if (info != null) {
                pn.setJSDocInfo(info);
                if (info.hasEnumParameterType() &&
                    pn.getFirstChild().isUnscopedQualifiedName()) {
                    typeRegistry.identifyEnumName(
                            pn.getFirstChild().getQualifiedName());
                }
            }
        } else if (tt == Token.SEMI && pn.getType() == Token.GETPROP) {
          // This may be dead code added intentionally, for JSDoc purposes.
          // For example: /** @type Number */ C.prototype.x;
          if (ts.isPopulated()) {
              pn.setJSDocInfo(ts.getAndResetJSDocInfo());
          }
        }

        return pn;
    }

    private Node condExpr(boolean inForInit)
        throws IOException, ParserException
    {
        Node pn = orExpr(inForInit);

        if (matchToken(Token.HOOK)) {
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addToken(Token.HOOK);
            Node ifTrue = assignExpr(false);
            mustMatchToken(Token.COLON, "msg.no.colon.cond");
            decompiler.addToken(Token.COLON);
            Node ifFalse = assignExpr(inForInit);
            return nf.createCondExpr(pn, ifTrue, ifFalse, lineno, charno);
        }

        return pn;
    }

    private Node orExpr(boolean inForInit)
        throws IOException, ParserException
    {
        Node pn = andExpr(inForInit);
        if (matchToken(Token.OR)) {
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addToken(Token.OR);
            pn = nf.createBinary(Token.OR, pn, orExpr(inForInit), lineno,
                charno);
        }

        return pn;
    }

    private Node andExpr(boolean inForInit)
        throws IOException, ParserException
    {
        Node pn = bitOrExpr(inForInit);
        if (matchToken(Token.AND)) {
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addToken(Token.AND);
            pn = nf.createBinary(Token.AND, pn, andExpr(inForInit), lineno,
                charno);
        }

        return pn;
    }

    private Node bitOrExpr(boolean inForInit)
        throws IOException, ParserException
    {
        Node pn = bitXorExpr(inForInit);
        while (matchToken(Token.BITOR)) {
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addToken(Token.BITOR);
            pn = nf.createBinary(Token.BITOR, pn, bitXorExpr(inForInit), lineno,
                charno);
        }
        return pn;
    }

    private Node bitXorExpr(boolean inForInit)
        throws IOException, ParserException
    {
        Node pn = bitAndExpr(inForInit);
        while (matchToken(Token.BITXOR)) {
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addToken(Token.BITXOR);
            pn = nf.createBinary(Token.BITXOR, pn, bitAndExpr(inForInit), lineno,
                charno);
        }
        return pn;
    }

    private Node bitAndExpr(boolean inForInit)
        throws IOException, JavaScriptException
    {
        Node pn = eqExpr(inForInit);
        while (matchToken(Token.BITAND)) {
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addToken(Token.BITAND);
            pn = nf.createBinary(Token.BITAND, pn, eqExpr(inForInit), lineno,
                charno);
        }
        return pn;
    }

    private Node eqExpr(boolean inForInit)
        throws IOException, ParserException
    {
        Node pn = relExpr(inForInit);
        int lineno;
        int charno;
        for (;;) {
            int tt = peekToken();
            lineno = ts.getLineno();
            charno = ts.getCharno();
            switch (tt) {
              case Token.EQ:
              case Token.NE:
              case Token.SHEQ:
              case Token.SHNE:
                consumeToken();
                int decompilerToken = tt;
                int parseToken = tt;
                if (compilerEnv.getLanguageVersion() == Context.VERSION_1_2) {
                    // JavaScript 1.2 uses shallow equality for == and != .
                    // In addition, convert === and !== for decompiler into
                    // == and != since the decompiler is supposed to show
                    // canonical source and in 1.2 ===, !== are allowed
                    // only as an alias to ==, !=.
                    switch (tt) {
                      case Token.EQ:
                        parseToken = Token.SHEQ;
                        break;
                      case Token.NE:
                        parseToken = Token.SHNE;
                        break;
                      case Token.SHEQ:
                        decompilerToken = Token.EQ;
                        break;
                      case Token.SHNE:
                        decompilerToken = Token.NE;
                        break;
                    }
                }
                decompiler.addToken(decompilerToken);
                pn = nf.createBinary(parseToken, pn, relExpr(inForInit), lineno,
                    charno);
                continue;
            }
            break;
        }
        return pn;
    }

    @SuppressWarnings("fallthrough")
    private Node relExpr(boolean inForInit)
        throws IOException, ParserException
    {
        Node pn = shiftExpr();
        int lineno;
        int charno;
        for (;;) {
            int tt = peekToken();
            lineno = ts.getLineno();
            charno = ts.getCharno();
            switch (tt) {
              case Token.IN:
                if (inForInit)
                    break;
                // fall through
              case Token.INSTANCEOF:
              case Token.LE:
              case Token.LT:
              case Token.GE:
              case Token.GT:
                consumeToken();
                decompiler.addToken(tt);
                pn = nf.createBinary(tt, pn, shiftExpr(), lineno, charno);
                continue;
            }
            break;
        }
        return pn;
    }

    private Node shiftExpr()
        throws IOException, ParserException
    {
        Node pn = addExpr();
        int lineno;
        int charno;
        for (;;) {
            int tt = peekToken();
            lineno = ts.getLineno();
            charno = ts.getCharno();
            switch (tt) {
              case Token.LSH:
              case Token.URSH:
              case Token.RSH:
                consumeToken();
                decompiler.addToken(tt);
                pn = nf.createBinary(tt, pn, addExpr(), lineno, charno);
                continue;
            }
            break;
        }
        return pn;
    }

    private Node addExpr()
        throws IOException, ParserException
    {
        Node pn = mulExpr();
        int lineno;
        int charno;
        for (;;) {
            int tt = peekToken();
            lineno = ts.getLineno();
            charno = ts.getCharno();
            if (tt == Token.ADD || tt == Token.SUB) {
                consumeToken();
                decompiler.addToken(tt);
                // flushNewLines
                pn = nf.createBinary(tt, pn, mulExpr(), lineno, charno);
                continue;
            }
            break;
        }

        return pn;
    }

    private Node mulExpr()
        throws IOException, ParserException
    {
        Node pn = unaryExpr();
        int lineno;
        int charno;
        for (;;) {
            int tt = peekToken();
            lineno = ts.getLineno();
            charno = ts.getCharno();
            switch (tt) {
              case Token.MUL:
              case Token.DIV:
              case Token.MOD:
                consumeToken();
                decompiler.addToken(tt);
                pn = nf.createBinary(tt, pn, unaryExpr(), lineno, charno);
                continue;
            }
            break;
        }

        return pn;
    }

    @SuppressWarnings("fallthrough")
    private Node unaryExpr()
        throws IOException, ParserException
    {
        int tt;

        tt = peekToken();
        int lineno = ts.getLineno();
        int charno = ts.getCharno();

        switch(tt) {
        case Token.VOID:
        case Token.NOT:
        case Token.BITNOT:
        case Token.TYPEOF:
            consumeToken();
            decompiler.addToken(tt);
            return nf.createUnary(tt, unaryExpr(), lineno, charno);

        case Token.ADD:
            consumeToken();
            // Convert to special POS token in decompiler and parse tree
            decompiler.addToken(Token.POS);
            return nf.createUnary(Token.POS, unaryExpr(), lineno, charno);

        case Token.SUB:
            consumeToken();
            // Convert to special NEG token in decompiler and parse tree
            decompiler.addToken(Token.NEG);
            return nf.createUnary(Token.NEG, unaryExpr(), lineno, charno);

        case Token.INC:
        case Token.DEC:
            consumeToken();
            decompiler.addToken(tt);
            return nf.createIncDec(tt, false, memberExpr(true), lineno, charno);

        case Token.DELPROP:
            consumeToken();
            decompiler.addToken(Token.DELPROP);
            return nf.createUnary(Token.DELPROP, unaryExpr(), lineno, charno);

        case Token.ERROR:
            consumeToken();
            break;

        // XML stream encountered in expression.
        case Token.LT:
            if (compilerEnv.isXmlAvailable()) {
                consumeToken();
                Node pn = xmlInitializer();
                return memberExprTail(true, pn);
            }
            // Fall thru to the default handling of RELOP

        default:
            Node pn = memberExpr(true);

            // Don't look across a newline boundary for a postfix incop.
            tt = peekTokenOrEOL();
            if (tt == Token.INC || tt == Token.DEC) {
                consumeToken();
                decompiler.addToken(tt);
                return nf.createIncDec(tt, true, pn, lineno, charno);
            }
            return pn;
        }
        return nf.createErrorName(); // Only reached on error.  Try to continue.

    }

    private Node xmlInitializer() throws IOException
    {
        int tt = ts.getFirstXMLToken();
        int lineno = ts.getLineno();
        int charno = ts.getCharno();
        if (tt != Token.XML && tt != Token.XMLEND) {
            reportError("msg.syntax");
            return null;
        }

        /* Make a NEW node to append to. */
        Node pnXML = nf.createLeaf(Token.NEW, lineno, charno);

        String xml = ts.getString();
        lineno = ts.getLineno();
        charno = ts.getCharno();
        boolean fAnonymous = xml.trim().startsWith("<>");

        Node pn = nf.createName(fAnonymous ? "XMLList" : "XML", lineno, charno);
        nf.addChildToBack(pnXML, pn);

        pn = null;
        Node expr;
        for (;;tt = ts.getNextXMLToken()) {
            switch (tt) {
            case Token.XML:
                xml = ts.getString();
                lineno = ts.getLineno();
                charno = ts.getCharno();
                decompiler.addName(xml);
                mustMatchToken(Token.LC, "msg.syntax");
                decompiler.addToken(Token.LC);
                expr = (peekToken() == Token.RC)
                    ? nf.createString("", lineno, charno)
                    : expr(false);
                mustMatchToken(Token.RC, "msg.syntax");
                decompiler.addToken(Token.RC);
                if (pn == null) {
                    pn = nf.createString(xml, lineno, charno);
                } else {
                    pn = nf.createBinary(Token.ADD, pn,
                        nf.createString(xml, lineno, charno), lineno, charno);
                }
                int nodeType;
                if (ts.isXMLAttribute()) {
                    nodeType = Token.ESCXMLATTR;
                } else {
                    nodeType = Token.ESCXMLTEXT;
                }
                expr = nf.createUnary(nodeType, expr, lineno, charno);
                pn = nf.createBinary(Token.ADD, pn, expr, lineno, charno);
                break;
            case Token.XMLEND:
                xml = ts.getString();
                lineno = ts.getLineno();
                charno = ts.getCharno();
                decompiler.addName(xml);
                if (pn == null) {
                    pn = nf.createString(xml, lineno, charno);
                } else {
                    pn = nf.createBinary(Token.ADD, pn,
                        nf.createString(xml, lineno, charno), lineno, charno);
                }

                nf.addChildToBack(pnXML, pn);
                return pnXML;
            default:
                reportError("msg.syntax");
                return null;
            }
        }
    }

    private void argumentList(Node listNode)
        throws IOException, ParserException
    {
        boolean matched;
        matched = matchToken(Token.RP);
        if (!matched) {
            boolean first = true;
            do {
                if (!first)
                    decompiler.addToken(Token.COMMA);
                first = false;
                nf.addChildToBack(listNode, assignExpr(false));
            } while (matchToken(Token.COMMA));

            mustMatchToken(Token.RP, "msg.no.paren.arg");
        }
        decompiler.addToken(Token.RP);
    }

    private Node memberExpr(boolean allowCallSyntax)
        throws IOException, ParserException
    {
        int tt;

        Node pn;

        /* Check for new expressions. */
        tt = peekToken();
        int lineno = ts.getLineno();
        int charno = ts.getCharno();
        if (tt == Token.NEW) {
            /* Eat the NEW token. */
            consumeToken();
            decompiler.addToken(Token.NEW);

            /* Make a NEW node to append to. */
            pn = nf.createLeaf(Token.NEW, lineno, charno);
            nf.addChildToBack(pn, memberExpr(false));

            if (matchToken(Token.LP)) {
                decompiler.addToken(Token.LP);
                /* Add the arguments to pn, if any are supplied. */
                argumentList(pn);
            }

            /* XXX there's a check in the C source against
             * "too many constructor arguments" - how many
             * do we claim to support?
             */

            /* Experimental syntax:  allow an object literal to follow a new expression,
             * which will mean a kind of anonymous class built with the JavaAdapter.
             * the object literal will be passed as an additional argument to the constructor.
             */
            tt = peekToken();
            if (tt == Token.LC) {
                nf.addChildToBack(pn, primaryExpr());
            }
        } else {
            pn = primaryExpr();
        }

        return memberExprTail(allowCallSyntax, pn);
    }

    private Node memberExprTail(boolean allowCallSyntax, Node pn)
        throws IOException, ParserException
    {
      tailLoop:
        for (;;) {
            int tt = peekToken();
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            switch (tt) {

              case Token.DOT:
              case Token.DOTDOT:
                {
                    int memberTypeFlags;
                    String s;

                    consumeToken();
                    decompiler.addToken(tt);
                    memberTypeFlags = 0;
                    if (tt == Token.DOTDOT) {
                        mustHaveXML();
                        memberTypeFlags = Node.DESCENDANTS_FLAG;
                    }
                    if (!compilerEnv.isXmlAvailable()) {
                        mustMatchToken(Token.NAME, "msg.no.name.after.dot");
                        s = ts.getString();

                        decompiler.addName(s);
                        pn = nf.createPropertyGet(pn, null, s, memberTypeFlags,
                            // Dot's position
                            lineno, charno,
                            // Name's position
                            ts.getLineno(), ts.getCharno());
                        break;
                    }

                    tt = nextToken();
                    switch (tt) {
                      // handles: name, ns::name, ns::*, ns::[expr]
                      case Token.NAME:
                        s = ts.getString();
                        decompiler.addName(s);
                        pn = propertyName(pn, s, memberTypeFlags,
                            // Dot's position
                            lineno, charno,
                            // Name's position
                            ts.getLineno(), ts.getCharno());                        break;

                      // handles: *, *::name, *::*, *::[expr]
                      case Token.MUL:
                        decompiler.addName("*");
                        pn = propertyName(pn, "*", memberTypeFlags,
                            ts.getLineno(), ts.getCharno());
                        break;

                      // handles: '@attr', '@ns::attr', '@ns::*', '@ns::*',
                      //          '@::attr', '@::*', '@*', '@*::attr', '@*::*'
                      case Token.XMLATTR:
                        decompiler.addToken(Token.XMLATTR);
                        pn = attributeAccess(pn, memberTypeFlags);
                        break;

                      default:
                        reportError("msg.no.name.after.dot");
                    }
                }
                break;

              case Token.DOTQUERY:
                consumeToken();
                mustHaveXML();
                decompiler.addToken(Token.DOTQUERY);
                pn = nf.createDotQuery(pn, expr(false),
                    ts.getLineno(), ts.getCharno());
                mustMatchToken(Token.RP, "msg.no.paren");
                decompiler.addToken(Token.RP);
                break;

              case Token.LB:
                consumeToken();
                decompiler.addToken(Token.LB);
                pn = nf.createElementGet(
                    pn, null, expr(false), 0, lineno, charno);
                mustMatchToken(Token.RB, "msg.no.bracket.index");
                decompiler.addToken(Token.RB);
                break;

              case Token.LP:
                if (!allowCallSyntax) {
                    break tailLoop;
                }
                consumeToken();
                decompiler.addToken(Token.LP);
                pn = nf.createCallOrNew(Token.CALL, pn, ts.getLineno(),
                    ts.getCharno());
                /* Add the arguments to pn, if any are supplied. */
                argumentList(pn);
                break;

              default:
                break tailLoop;
            }
        }
        return pn;
    }

    /*
     * Xml attribute expression:
     *   '@attr', '@ns::attr', '@ns::*', '@ns::*', '@*', '@*::attr', '@*::*'
     */
    private Node attributeAccess(Node pn, int memberTypeFlags)
        throws IOException
    {
        memberTypeFlags |= Node.ATTRIBUTE_FLAG;
        int tt = nextToken();

        switch (tt) {
          // handles: @name, @ns::name, @ns::*, @ns::[expr]
          case Token.NAME:
            {
                String s = ts.getString();
                int lineno = ts.getLineno();
                int charno = ts.getCharno();
                decompiler.addName(s);
                pn = propertyName(pn, s, memberTypeFlags, lineno, charno);
            }
            break;

          // handles: @*, @*::name, @*::*, @*::[expr]
          case Token.MUL:
            decompiler.addName("*");
            pn = propertyName(pn, "*", memberTypeFlags, ts.getLineno(),
                ts.getCharno());
            break;

          // handles @[expr]
          case Token.LB:
            decompiler.addToken(Token.LB);
            pn = nf.createElementGet(pn, null, expr(false), memberTypeFlags,
                ts.getLineno(), ts.getCharno());
            mustMatchToken(Token.RB, "msg.no.bracket.index");
            decompiler.addToken(Token.RB);
            break;

          default:
            reportError("msg.no.name.after.xmlAttr");
            pn = nf.createPropertyGet(pn, null, "?", memberTypeFlags,
                ts.getLineno(), ts.getCharno(),
                ts.getLineno(), ts.getCharno());
            break;
        }

        return pn;
    }

    /**
     * Check if :: follows name in which case it becomes qualified name
     */
    private Node propertyName(Node pn, String name, int memberTypeFlags,
        int dotLineno, int dotCharno,
        int nameLineno, int nameCharno) throws IOException, ParserException
    {
        String namespace = null;
        if (matchToken(Token.COLONCOLON)) {
            decompiler.addToken(Token.COLONCOLON);
            namespace = name;

            int tt = nextToken();
            switch (tt) {
              // handles name::name
              case Token.NAME:
                name = ts.getString();
                decompiler.addName(name);
                break;

              // handles name::*
              case Token.MUL:
                decompiler.addName("*");
                name = "*";
                break;

              // handles name::[expr]
              case Token.LB:
                decompiler.addToken(Token.LB);
                pn = nf.createElementGet(pn, namespace, expr(false),
                    memberTypeFlags, nameLineno, nameCharno);
                mustMatchToken(Token.RB, "msg.no.bracket.index");
                decompiler.addToken(Token.RB);
                return pn;

              default:
                reportError("msg.no.name.after.coloncolon");
                name = "?";
            }
        }

        pn = nf.createPropertyGet(pn, namespace, name,
            memberTypeFlags, dotLineno, dotCharno, nameLineno, nameCharno);
        return pn;
    }

    /**
     * Variant of {@link #propertyName} where the separator and name source
     * positions are the same.
     */
    private Node propertyName(Node pn, String name, int memberTypeFlags,
        int lineno, int charno) throws IOException, ParserException
    {
        return propertyName(
            pn, name, memberTypeFlags,
            lineno, charno,
            lineno, charno);
    }

    @SuppressWarnings("fallthrough")
    private Node primaryExpr()
        throws IOException, ParserException
    {
        Node pn;

        int ttFlagged = nextFlaggedToken();
        int tt = ttFlagged & CLEAR_TI_MASK;

        switch(tt) {

          case Token.FUNCTION:
            return function(FunctionNode.FUNCTION_EXPRESSION);

          case Token.LB: {
            ObjArray elems = new ObjArray();
            int skipCount = 0;
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addToken(Token.LB);
            boolean after_lb_or_comma = true;
            for (;;) {
                tt = peekToken();

                if (tt == Token.COMMA) {
                    consumeToken();
                    decompiler.addToken(Token.COMMA);
                    if (!after_lb_or_comma) {
                        after_lb_or_comma = true;
                    } else {
                        elems.add(null);
                        ++skipCount;
                    }
                } else if (tt == Token.RB) {
                    if (after_lb_or_comma && elems.size() > 0) {
                      addWarning("msg.trailing.comma");
                    }
                    consumeToken();
                    decompiler.addToken(Token.RB);
                    break;
                } else {
                    if (!after_lb_or_comma) {
                        reportError("msg.no.bracket.arg");
                    }
                    elems.add(assignExpr(false));
                    after_lb_or_comma = false;
                }
            }
            return nf.createArrayLiteral(elems, skipCount, lineno, charno);
          }

          case Token.LC: {
            ObjArray elems = new ObjArray();
            decompiler.addToken(Token.LC);
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            if (!matchToken(Token.RC)) {

                boolean first = true;
            commaloop:
                do {
                    Node property;

                    if (!first)
                        decompiler.addToken(Token.COMMA);
                    else
                        first = false;

                    tt = peekToken();
                    switch(tt) {
                    case Token.NAME:
                    case Token.STRING: {
                        consumeToken();
                        // map NAMEs to STRINGs in object literal context
                        // but tell the decompiler the proper type
                        String s = ts.getString();
                        int linenoName = ts.getLineno();
                        int charnoName = ts.getCharno();
                        if (tt == Token.NAME) {
                            if (s.equals("get") &&
                                peekToken() == Token.NAME) {
                                decompiler.addToken(Token.GET);
                                consumeToken();
                                s = ts.getString();
                                decompiler.addName(s);
                                if (!getterSetterProperty(elems, s, true,
                                    linenoName, charnoName)) {
                                    break commaloop;
                                }
                                break;
                            } else if (s.equals("set") &&
                                       peekToken() == Token.NAME) {
                                decompiler.addToken(Token.SET);
                                consumeToken();
                                s = ts.getString();
                                decompiler.addName(s);
                                if (!getterSetterProperty(elems, s, false,
                                    linenoName, charnoName)) {
                                    break commaloop;
                                }
                                break;
                            }
                            decompiler.addName(s);
                            property =
                                nf.createString(s, linenoName, charnoName);
                        } else {
                            decompiler.addString(s);
                            property =
                                nf.createString(s, linenoName, charnoName);
                            property.setQuotedString();
                        }
                        plainProperty(elems, property);
                        break;
                      }

                    case Token.NUMBER: {
                        consumeToken();
                        double n = ts.getNumber();
                        int linenoNumber = ts.getLineno();
                        int charnoNumber = ts.getCharno();
                        decompiler.addNumber(n);
                        property =
                            nf.createNumber(n, linenoNumber, charnoNumber);
                        plainProperty(elems, property);
                        break;
                      }

                    case Token.COMMA:
                        consumeToken();
                    case Token.RC:
                        addWarning("msg.trailing.comma");
                        break commaloop;

                    default:
                        reportError("msg.bad.prop");                    	
                        break commaloop;
                    }
                } while (matchToken(Token.COMMA));

                mustMatchToken(Token.RC, "msg.no.brace.prop");
            }
            decompiler.addToken(Token.RC);
            return nf.createObjectLiteral(elems, lineno, charno);
        }

        case Token.LP:

            JSDocInfo info = ts.getAndResetJSDocInfo();

            /* Brendan's IR-jsparse.c makes a new node tagged with
             * TOK_LP here... I'm not sure I understand why.  Isn't
             * the grouping already implicit in the structure of the
             * parse tree?  also TOK_LP is already overloaded (I
             * think) in the C IR as 'function call.'  */
            decompiler.addToken(Token.LP);
            pn = expr(false);
            pn.putProp(Node.PARENTHESIZED_PROP, Boolean.TRUE);
            decompiler.addToken(Token.RP);
            if (info == null) {
              info = ts.getAndResetJSDocInfo();
            }
            if (info != null && info.hasType()) {
                pn.setJSDocInfo(info);
            }
            mustMatchToken(Token.RP, "msg.no.paren");
            return pn;

        case Token.XMLATTR:
            mustHaveXML();
            decompiler.addToken(Token.XMLATTR);
            pn = attributeAccess(null, 0);
            return pn;

        case Token.NAME: {
            String name = ts.getString();
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            if ((ttFlagged & TI_CHECK_LABEL) != 0) {
                if (peekToken() == Token.COLON) {
                    // Do not consume colon, it is used as unwind indicator
                    // to return to statementHelper.
                    // XXX Better way?
                    return nf.createLabel(name, lineno, charno);
                }
            }

            decompiler.addName(name);
            if (compilerEnv.isXmlAvailable()) {
                pn = propertyName(null, name, 0, lineno, charno);
            } else {
                pn = nf.createName(name, lineno, charno);
            }
            return pn;
          }

        case Token.NUMBER: {
            double n = ts.getNumber();
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addNumber(n);
            return nf.createNumber(n, lineno, charno);
        }

        case Token.STRING: {
            String s = ts.getString();
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addString(s);
            return nf.createString(s, lineno, charno);
        }

        case Token.DIV:
        case Token.ASSIGN_DIV: {
            // Got / or /= which should be treated as regexp in fact
            ts.readRegExp(tt);
            String flags = ts.regExpFlags;
            ts.regExpFlags = null;
            String re = ts.getString();
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addRegexp(re, flags);
            return nf.createRegExp(re, flags, lineno, charno);
        }

        case Token.NULL:
        case Token.THIS:
        case Token.FALSE:
        case Token.TRUE: {
            int lineno = ts.getLineno();
            int charno = ts.getCharno();
            decompiler.addToken(tt);
            return nf.createLeaf(tt, lineno, charno);
        }

        case Token.RESERVED:
            reportError("msg.reserved.id");
            break;

        case Token.ERROR:
            /* the scanner or one of its subroutines reported the error. */
            break;

        case Token.EOF:
            reportError("msg.unexpected.eof");
            break;

        default:
            reportError("msg.syntax");
            break;
        }
        return null;    // should never reach here
    }

    String getSourceName()
    {
        return sourceURI;
    }

    private void plainProperty(ObjArray elems, Object property)
            throws IOException {
        mustMatchToken(Token.COLON, "msg.no.colon.prop");

        // OBJLIT is used as ':' in object literal for
        // decompilation to solve spacing ambiguity.
        decompiler.addToken(Token.OBJECTLIT);
        elems.add(property);
        elems.add(assignExpr(false));
    }

    private boolean getterSetterProperty(ObjArray elems, String property,
            boolean isGetter, int lineno, int charno) throws IOException {
        Node f = function(FunctionNode.FUNCTION_EXPRESSION);
        if (f.getType() != Token.FUNCTION) {
            reportError("msg.bad.prop");
            return false;
        }
        int fnIndex = f.getExistingIntProp(Node.FUNCTION_PROP);
        FunctionNode fn = currentScriptOrFn.getFunctionNode(fnIndex);
        if (fn.getFunctionName().length() != 0) {
            reportError("msg.bad.prop");
            return false;
        }
        elems.add(nf.createName(property, lineno, charno));
        if (isGetter) {
            elems.add(nf.createUnary(Token.GET, f, lineno, charno));
        } else {
            elems.add(nf.createUnary(Token.SET, f, lineno, charno));
        }
        return true;
    }
}
