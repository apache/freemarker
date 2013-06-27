/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.Collator;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import freemarker.ext.beans.BeansWrapper;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleSequence;
import freemarker.template.Template;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.DateToISO8601CalendarFactory;
import freemarker.template.utility.NullWriter;
import freemarker.template.utility.StringUtil;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * Object that represents the runtime environment during template processing.
 * For every invocation of a <tt>Template.process()</tt> method, a new instance
 * of this object is created, and then discarded when <tt>process()</tt> returns.
 * This object stores the set of temporary variables created by the template,
 * the value of settings set by the template, the reference to the data model root,
 * etc. Everything that is needed to fulfill the template processing job.
 *
 * <p>Data models that need to access the <tt>Environment</tt>
 * object that represents the template processing on the current thread can use
 * the {@link #getCurrentEnvironment()} method.
 *
 * <p>If you need to modify or read this object before or after the <tt>process</tt>
 * call, use {@link Template#createProcessingEnvironment(Object rootMap, Writer out, ObjectWrapper wrapper)}
 *
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 * @author Attila Szegedi
 */
public final class Environment extends Configurable {

    static final String STACK_SECTION_SEPARATOR = "----------";

    private static final ThreadLocal threadEnv = new ThreadLocal();

    private static final Logger logger = Logger.getLogger("freemarker.runtime");
    private static final Logger attemptLogger = Logger.getLogger("freemarker.runtime.attempt");

    private static final Map localizedNumberFormats = new HashMap();
    private static final Map localizedDateFormats = new HashMap();

    // Do not use this object directly; clone it first! DecimalFormat isn't
    // thread-safe.
    private static final DecimalFormat C_NUMBER_FORMAT
            = new DecimalFormat(
                    "0.################",
                    new DecimalFormatSymbols(Locale.US));
    static {
        C_NUMBER_FORMAT.setGroupingUsed(false);
        C_NUMBER_FORMAT.setDecimalSeparatorAlwaysShown(false);
    }

    private final TemplateHashModel rootDataModel;
    private final ArrayList/*<TemplateElement>*/ instructionStack = new ArrayList();
    private final ArrayList recoveredErrorStack = new ArrayList();

    private NumberFormat numberFormat;
    private Map numberFormats;

    private DateFormat timeFormat, dateFormat, dateTimeFormat;
    private Map[] dateFormats;
    private NumberFormat cNumberFormat;
    
    /**
     * Used by the "iso_" built-ins to accelerate formatting.
     * @see #getISOBuiltInCalendar() 
     */
    private DateToISO8601CalendarFactory isoBuiltInCalendarFactory;

    private Collator collator;

    private Writer out;
    private Macro.Context currentMacroContext;
    private ArrayList localContextStack; 
    private Namespace mainNamespace, currentNamespace, globalNamespace;
    private HashMap loadedLibs;

    private boolean inAttemptBlock;
    private Throwable lastThrowable;
    
    private TemplateModel lastReturnValue;
    private HashMap macroToNamespaceLookup = new HashMap();

    private TemplateNodeModel currentVisitorNode;    
    private TemplateSequenceModel nodeNamespaces;
    // Things we keep track of for the fallback mechanism.
    private int nodeNamespaceIndex;
    private String currentNodeName, currentNodeNS;
    
    private String cachedURLEscapingCharset;
    private boolean urlEscapingCharsetCached;

    private boolean fastInvalidReferenceExceptions;
    
    /**
     * Retrieves the environment object associated with the current
     * thread. Data model implementations that need access to the
     * environment can call this method to obtain the environment object
     * that represents the template processing that is currently running
     * on the current thread.
     */
    public static Environment getCurrentEnvironment()
    {
        return (Environment)threadEnv.get();
    }

    public Environment(Template template, final TemplateHashModel rootDataModel, Writer out)
    {
        super(template);
        this.globalNamespace = new Namespace(null);
        this.currentNamespace = mainNamespace = new Namespace(template);
        this.out = out;
        this.rootDataModel = rootDataModel;
        importMacros(template);
    }

    /**
     * Retrieves the currently processed template.
     */
    public Template getTemplate()
    {
        return (Template)getParent();
    }

    /**
     * Deletes cached values that meant to be valid only during a single
     * template execution. 
     */
    private void clearCachedValues() {
        numberFormats = null;
        numberFormat = null;
        dateFormats = null;
        collator = null;
        cachedURLEscapingCharset = null;
        urlEscapingCharsetCached = false;
    }
    
    /**
     * Processes the template to which this environment belongs.
     */
    public void process() throws TemplateException, IOException {
        Object savedEnv = threadEnv.get();
        threadEnv.set(this);
        try {
            // Cached values from a previous execution are possibly outdated.
            clearCachedValues();
            try {
                doAutoImportsAndIncludes(this);
                visit(getTemplate().getRootTreeNode());
                // It's here as we must not flush if there was an exception.
                if (getAutoFlush()) {
                    out.flush();
                }
            } finally {
                // It's just to allow the GC to free memory...
                clearCachedValues();
            }
        } finally {
            threadEnv.set(savedEnv);
        }
    }
    
    /**
     * "Visit" the template element.
     */
    void visit(TemplateElement element)
    throws TemplateException, IOException
    {
        pushElement(element);
        try {
            element.accept(this);
        }
        catch (TemplateException te) {
            handleTemplateException(te);
        }
        finally {
            popElement();
        }
    }
    
    /**
     * Instead of pushing into the element stack, we replace the top element for the time the parameter element is
     * visited, and then we restore the top element. The main purpose of this is to get rid of elements in the error
     * stack trace that from user perspective shouldn't have a stack frame. These typical example is
     * {@code [#if foo]...[@failsHere/]...[/#if]}, where the #if call shouldn't be in the stack trace. (Simply marking
     * #if as hidden in stack traces would be wrong, because we still want to show #if when its test expression fails.)    
     */
    void visitByHiddingParent(TemplateElement element)
    throws TemplateException, IOException {
        TemplateElement parent = replaceTopElement(element);
        try {
            element.accept(this);
        } catch (TemplateException te) {
            handleTemplateException(te);
        } finally {
            replaceTopElement(parent);
        }
    }

    private TemplateElement replaceTopElement(TemplateElement element) {
        return (TemplateElement) instructionStack.set(instructionStack.size() - 1, element);
    }

    private static final TemplateModel[] NO_OUT_ARGS = new TemplateModel[0];
    
    public void visit(final TemplateElement element,
            TemplateDirectiveModel directiveModel, Map args, 
            final List bodyParameterNames) throws TemplateException, IOException {
        TemplateDirectiveBody nested;
        if(element == null) {
            nested = null;
        }
        else {
            nested = new TemplateDirectiveBody() {
                public void render(Writer newOut) throws TemplateException, IOException {
                    Writer prevOut = out;
                    out = newOut;
                    try {
                        Environment.this.visit(element);
                    }
                    finally {
                        out = prevOut;
                    }
                }
            };
        }
        final TemplateModel[] outArgs;
        if(bodyParameterNames == null || bodyParameterNames.isEmpty()) {
            outArgs = NO_OUT_ARGS;
        }
        else {
            outArgs = new TemplateModel[bodyParameterNames.size()];
        }
        if(outArgs.length > 0) {
            pushLocalContext(new LocalContext() {
                public TemplateModel getLocalVariable(String name) {
                    int index = bodyParameterNames.indexOf(name);
                    return index != -1 ? outArgs[index] : null;
                }

                public Collection getLocalVariableNames() {
                    return bodyParameterNames;
                }
            });
        }
        try {
            directiveModel.execute(this, args, outArgs, nested);
        }
        finally {
            if(outArgs.length > 0) {
                popLocalContext();
            }
        }
    }
    
    /**
     * "Visit" the template element, passing the output
     * through a TemplateTransformModel
     * @param element the element to visit through a transform
     * @param transform the transform to pass the element output
     * through
     * @param args optional arguments fed to the transform
     */
    void visitAndTransform(TemplateElement element,
               TemplateTransformModel transform,
               Map args)
    throws TemplateException, IOException
    {
        try {
            Writer tw = transform.getWriter(out, args);
            if (tw == null) tw = EMPTY_BODY_WRITER;
            TransformControl tc =
                tw instanceof TransformControl
                ? (TransformControl)tw
                : null;

            Writer prevOut = out;
            out = tw;
            try {
                if(tc == null || tc.onStart() != TransformControl.SKIP_BODY) {
                    do {
                        if(element != null) {
                            visitByHiddingParent(element);
                        }
                    } while(tc != null && tc.afterBody() == TransformControl.REPEAT_EVALUATION);
                }
            }
            catch(Throwable t) {
                try {
                    if(tc != null) {
                        tc.onError(t);
                    }
                    else {
                        throw t;
                    }
                }
                catch(TemplateException e) {
                    throw e;
                }
                catch(IOException e) {
                    throw e;
                }
                catch(RuntimeException e) {
                    throw e;
                }
                catch(Error e) {
                    throw e;
                }
                catch(Throwable e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
            finally {
                out = prevOut;
                tw.close();
            }
        }
        catch(TemplateException te) {
            handleTemplateException(te);
        }
    }
    
    /**
     * Visit a block using buffering/recovery
     */
     void visitAttemptRecover(TemplateElement attemptBlock, RecoveryBlock recoveryBlock) 
     throws TemplateException, IOException {
         Writer prevOut = this.out;
         StringWriter sw = new StringWriter();
         this.out = sw;
         TemplateException thrownException = null;
         boolean lastFIRE = setFastInvalidReferenceExceptions(false);
         boolean lastInAttemptBlock = inAttemptBlock; 
         try {
             inAttemptBlock = true;
             visitByHiddingParent(attemptBlock);
         } catch (TemplateException te) {
             thrownException = te;
         } finally {
             inAttemptBlock = lastInAttemptBlock;
             setFastInvalidReferenceExceptions(lastFIRE);
             this.out = prevOut;
         }
         if (thrownException != null) {
             if (attemptLogger.isDebugEnabled()) {
                 attemptLogger.debug("Error in attempt block " + 
                         attemptBlock.getStartLocationQuoted(), thrownException);
             }
             try {
                 recoveredErrorStack.add(thrownException);
                 visit(recoveryBlock);
             } finally {
                 recoveredErrorStack.remove(recoveredErrorStack.size() -1);
             }
         } else {
             out.write(sw.toString());
         }
     }
     
     String getCurrentRecoveredErrorMessage() throws TemplateException {
         if(recoveredErrorStack.isEmpty()) {
             throw new _MiscTemplateException(this, ".error is not available outside of a #recover block");
         }
         return ((Throwable) recoveredErrorStack.get(recoveredErrorStack.size() -1)).getMessage();
     }
     
     /**
      * Tells if we are inside an <tt>#attempt</tt> block (but before <tt>#recover</tt>). This can be useful for
      * {@link TemplateExceptionHandler}-s, as then they may don't want to print the error to the output, as
      * <tt>#attempt</tt> will roll it back anyway. 
      * 
      * @since 2.3.20
      */
     public boolean isInAttemptBlock() {
         return inAttemptBlock;
     }


    void visit(BodyInstruction.Context bctxt) throws TemplateException, IOException {
        Macro.Context invokingMacroContext = getCurrentMacroContext();
        ArrayList prevLocalContextStack = localContextStack;
        TemplateElement body = invokingMacroContext.body;
        if (body != null) {
            this.currentMacroContext = invokingMacroContext.prevMacroContext;
            currentNamespace = invokingMacroContext.bodyNamespace;
            Configurable prevParent = getParent();
            setParent(currentNamespace.getTemplate());
            this.localContextStack = invokingMacroContext.prevLocalContextStack;
            if (invokingMacroContext.bodyParameterNames != null) {
                pushLocalContext(bctxt);
            }
            try {
                visit(body);
            }
            finally {
                if (invokingMacroContext.bodyParameterNames != null) {
                    popLocalContext();
                }
                this.currentMacroContext = invokingMacroContext;
                currentNamespace = getMacroNamespace(invokingMacroContext.getMacro());
                setParent(prevParent);
                this.localContextStack = prevLocalContextStack;
            }
        }
    }

    /**
     * "visit" an IteratorBlock
     */
    void visitIteratorBlock(IteratorBlock.Context ictxt)
    throws TemplateException, IOException
    {
        pushLocalContext(ictxt);
        try {
            ictxt.runLoop(this);
        }
        catch (BreakInstruction.Break br) {
        }
        catch (TemplateException te) {
            handleTemplateException(te);
        }
        finally {
            popLocalContext();
        }
    }
    
    /**
     * "Visit" A TemplateNodeModel
     */
    
    void visit(TemplateNodeModel node, TemplateSequenceModel namespaces) 
    throws TemplateException, IOException 
    {
        if (nodeNamespaces == null) {
            SimpleSequence ss = new SimpleSequence(1);
            ss.add(currentNamespace);
            nodeNamespaces = ss;
        }
        int prevNodeNamespaceIndex = this.nodeNamespaceIndex;
        String prevNodeName = this.currentNodeName;
        String prevNodeNS = this.currentNodeNS;
        TemplateSequenceModel prevNodeNamespaces = nodeNamespaces;
        TemplateNodeModel prevVisitorNode = currentVisitorNode;
        currentVisitorNode = node;
        if (namespaces != null) {
            this.nodeNamespaces = namespaces;
        }
        try {
            TemplateModel macroOrTransform = getNodeProcessor(node);
            if (macroOrTransform instanceof Macro) {
                visit((Macro) macroOrTransform, null, null, null, null);
            }
            else if (macroOrTransform instanceof TemplateTransformModel) {
                visitAndTransform(null, (TemplateTransformModel) macroOrTransform, null); 
            }
            else {
                String nodeType = node.getNodeType();
                if (nodeType != null) {
                    // If the node's type is 'text', we just output it.
                    if ((nodeType.equals("text") && node instanceof TemplateScalarModel)) 
                    {
                           out.write(((TemplateScalarModel) node).getAsString());
                    }
                    else if (nodeType.equals("document")) {
                        recurse(node, namespaces);
                    }
                    // We complain here, unless the node's type is 'pi', or "comment" or "document_type", in which case
                    // we just ignore it.
                    else if (!nodeType.equals("pi") 
                         && !nodeType.equals("comment") 
                         && !nodeType.equals("document_type")) 
                    {
                        throw new _MiscTemplateException(
                                this, noNodeHandlerDefinedDescription(node, node.getNodeNamespace(), nodeType));
                    }
                }
                else {
                    throw new _MiscTemplateException(
                            this, noNodeHandlerDefinedDescription(node, node.getNodeNamespace(), "default"));
                }
            }
        } 
        finally {
            this.currentVisitorNode = prevVisitorNode;
            this.nodeNamespaceIndex = prevNodeNamespaceIndex;
            this.currentNodeName = prevNodeName;
            this.currentNodeNS = prevNodeNS;
            this.nodeNamespaces = prevNodeNamespaces;
        }
    }

    private Object[] noNodeHandlerDefinedDescription(
            TemplateNodeModel node, String ns, String nodeType)
    throws TemplateModelException {
        String nsPrefix;
        if (ns != null) {
            if (ns.length() > 0) {
                nsPrefix = " and namespace ";
            } else {
                nsPrefix = " and no namespace";
            }
        } else {
            nsPrefix = "";
            ns = "";
        }
        return new Object[] { "No macro or directive is defined for node named ",  
                new _DelayedJQuote(node.getNodeName()), nsPrefix, ns,
                ", and there is no fallback handler called @", nodeType, " either." };
    }
    
    void fallback() throws TemplateException, IOException {
        TemplateModel macroOrTransform = getNodeProcessor(currentNodeName, currentNodeNS, nodeNamespaceIndex);
        if (macroOrTransform instanceof Macro) {
            visit((Macro) macroOrTransform, null, null, null, null);
        }
        else if (macroOrTransform instanceof TemplateTransformModel) {
            visitAndTransform(null, (TemplateTransformModel) macroOrTransform, null); 
        }
    }
    
    /**
     * "visit" a macro.
     */
    
    void visit(Macro macro, 
               Map namedArgs, 
               List positionalArgs, 
               List bodyParameterNames,
               TemplateElement nestedBlock) 
       throws TemplateException, IOException 
    {
        if (macro == Macro.DO_NOTHING_MACRO) {
            return;
        }
        pushElement(macro);
        try {
            Macro.Context previousMacroContext = currentMacroContext;
            Macro.Context mc = macro.new Context(this, nestedBlock, bodyParameterNames);

            String catchAll = macro.getCatchAll();
            TemplateModel unknownVars = null;
            
            if (namedArgs != null) {
                if (catchAll != null)
                    unknownVars = new SimpleHash();
                for (Iterator it = namedArgs.entrySet().iterator(); it.hasNext();) {
                    Map.Entry entry = (Map.Entry) it.next();
                    String varName = (String) entry.getKey();
                    boolean hasVar = macro.hasArgNamed(varName);
                    if (hasVar || catchAll != null) {
                        Expression arg = (Expression) entry.getValue();
                        TemplateModel value = arg.eval(this);
                        if (hasVar) {
                            mc.setLocalVar(varName, value);
                        } else {
                            ((SimpleHash)unknownVars).put(varName, value);
                        }
                    } else {
                        throw new _MiscTemplateException(this, new Object[] {
                                "Macro ", new _DelayedJQuote(macro.getName()), " has no such argument: ",
                                varName });
                    }
                }
            }
            else if (positionalArgs != null) {
                if (catchAll != null)
                    unknownVars = new SimpleSequence();
                String[] argumentNames = macro.getArgumentNamesInternal();
                int size = positionalArgs.size();
                if (argumentNames.length < size && catchAll == null) {
                    throw new _MiscTemplateException(this, new Object[] { 
                            "Macro " + StringUtil.jQuote(macro.getName()) + " only accepts "
                            + argumentNames.length + " parameters." });
                }
                for (int i = 0; i < size; i++) {
                    Expression argExp = (Expression) positionalArgs.get(i);
                    TemplateModel argModel = argExp.eval(this);
                    try {
                        if (i < argumentNames.length) {
                            String argName = argumentNames[i];
                            mc.setLocalVar(argName, argModel);
                        } else {
                            ((SimpleSequence)unknownVars).add(argModel);
                        }
                    } catch (RuntimeException re) {
                        throw new _MiscTemplateException(re, this);
                    }
                }
            }
            if (catchAll != null) {
                mc.setLocalVar(catchAll, unknownVars);
            }
            ArrayList prevLocalContextStack = localContextStack;
            localContextStack = null;
            Namespace prevNamespace = currentNamespace;
            Configurable prevParent = getParent();
            currentNamespace = (Namespace) macroToNamespaceLookup.get(macro);
            currentMacroContext = mc;
            try {
                mc.runMacro(this);
            }
            catch (ReturnInstruction.Return re) {
            }
            catch (TemplateException te) {
                handleTemplateException(te);
            } finally {
                currentMacroContext = previousMacroContext;
                localContextStack = prevLocalContextStack;
                currentNamespace = prevNamespace;
                setParent(prevParent);
            }
        } finally {
            popElement();
        }
    }
    
    void visitMacroDef(Macro macro) {
        macroToNamespaceLookup.put(macro, currentNamespace);
        currentNamespace.put(macro.getName(), macro);
    }
    
    Namespace getMacroNamespace(Macro macro) {
        return (Namespace) macroToNamespaceLookup.get(macro);
    }
    
    void recurse(TemplateNodeModel node, TemplateSequenceModel namespaces)
    throws TemplateException, IOException 
    {
        if (node == null) {
            node = this.getCurrentVisitorNode();
            if (node == null) {
                throw new _TemplateModelException(
                        "The target node of recursion is missing or null.");
            }
        }
        TemplateSequenceModel children = node.getChildNodes();
        if (children == null) return;
        for (int i=0; i<children.size(); i++) {
            TemplateNodeModel child = (TemplateNodeModel) children.get(i);
            if (child != null) {
                visit(child, namespaces);
            }
        }
    }

    Macro.Context getCurrentMacroContext() {
        return currentMacroContext;
    }
    
    private void handleTemplateException(TemplateException te)
        throws TemplateException
    {
        // Logic to prevent double-handling of the exception in
        // nested visit() calls.
        if(lastThrowable == te) {
            throw te;
        }
        lastThrowable = te;

        // Log the exception
        if(logger.isErrorEnabled()) {
            logger.error("Error executing FreeMarker template", te);
        }

        // Stop exception is not passed to the handler, but
        // explicitly rethrown.
        if(te instanceof StopException) {
            throw te;
        }

        // Finally, pass the exception to the handler
        getTemplateExceptionHandler().handleTemplateException(te, this, out);
    }

    public void setTemplateExceptionHandler(TemplateExceptionHandler templateExceptionHandler) {
        super.setTemplateExceptionHandler(templateExceptionHandler);
        lastThrowable = null;
    }
    
    public void setLocale(Locale locale) {
        super.setLocale(locale);
        // Clear local format cache
        numberFormats = null;
        numberFormat = null;

        dateFormats = null;
        timeFormat = dateFormat = dateTimeFormat = null;

        collator = null;
    }

    public void setTimeZone(TimeZone timeZone) {
        super.setTimeZone(timeZone);
        // Clear local date format cache
        dateFormats = null;
        timeFormat = dateFormat = dateTimeFormat = null;
    }
    
    public void setURLEscapingCharset(String urlEscapingCharset) {
        urlEscapingCharsetCached = false;
        super.setURLEscapingCharset(urlEscapingCharset);
    }
    
    /*
     * Note that altough it's not allowed to set this setting with the
     * <tt>setting</tt> directive, it still must be allowed to set it from Java
     * code while the template executes, since some frameworks allow templates
     * to actually change the output encoding on-the-fly.
     */
    public void setOutputEncoding(String outputEncoding) {
        urlEscapingCharsetCached = false;
        super.setOutputEncoding(outputEncoding);
    }
    
    /**
     * Returns the name of the charset that should be used for URL encoding.
     * This will be <code>null</code> if the information is not available.
     * The function caches the return value, so it's quick to call it
     * repeately. 
     */
    String getEffectiveURLEscapingCharset() {
        if (!urlEscapingCharsetCached) {
            cachedURLEscapingCharset = getURLEscapingCharset();
            if (cachedURLEscapingCharset == null) {
                cachedURLEscapingCharset = getOutputEncoding();
            }
            urlEscapingCharsetCached = true;
        }
        return cachedURLEscapingCharset;
    }

    Collator getCollator() {
        if(collator == null) {
            collator = Collator.getInstance(getLocale());
        }
        return collator;
    }
    
    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "==" operator.
     * 
     * @since 2.3.20
     */
    public boolean applyEqualsOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compare(leftValue, EvalUtil.CMP_OP_EQUALS, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "==" operator, except that if the two types
     *     are incompatible, they are treated as non-equal instead of throwing an exception. Comparing dates of
     *     different types (date-only VS time-only VS date-time) will still throw an exception, however.
     * 
     * @since 2.3.20
     */
    public boolean applyEqualsOperatorLenient(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compareLenient(leftValue, EvalUtil.CMP_OP_EQUALS, rightValue, this);
    }
    
    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "<" operator.
     * 
     * @since 2.3.20
     */
    public boolean applyLessThanOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compare(leftValue, EvalUtil.CMP_OP_LESS_THAN, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL "<" operator.
     * 
     * @since 2.3.20
     */
    public boolean applyLessThanOrEqualsOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compare(leftValue, EvalUtil.CMP_OP_LESS_THAN_EQUALS, rightValue, this);
    }
    
    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL ">" operator.
     * 
     * @since 2.3.20
     */
    public boolean applyGreaterThanOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compare(leftValue, EvalUtil.CMP_OP_GREATER_THAN, rightValue, this);
    }

    /**
     * Compares two {@link TemplateModel}-s according the rules of the FTL ">=" operator.
     * 
     * @since 2.3.20
     */
    public boolean applyWithGreaterThanOrEqualsOperator(TemplateModel leftValue, TemplateModel rightValue)
            throws TemplateException {
        return EvalUtil.compare(leftValue, EvalUtil.CMP_OP_GREATER_THAN_EQUALS, rightValue, this);
    }

    public void setOut(Writer out) {
        this.out = out;
    }

    public Writer getOut() {
        return out;
    }

    String formatNumber(Number number) {
        if(numberFormat == null) {
            numberFormat = getNumberFormatObject(getNumberFormat());
        }
        return numberFormat.format(number);
    }

    public void setNumberFormat(String formatName) {
        super.setNumberFormat(formatName);
        numberFormat = null;
    }

    String formatDate(Date date, int type) throws TemplateModelException {
        DateFormat df = getDateFormatObject(type);
        if(df == null) {
            throw new _TemplateModelException(new _ErrorDescriptionBuilder(
                    "Can't convert the date to string, because it's not known which parts of the date variable are "
                    + "in use.")
                    .tips(MessageUtil.UNKNOWN_DATE_TYPE_ERROR_TIPS));
        }
        return df.format(date);
    }

    public void setTimeFormat(String formatName) {
        super.setTimeFormat(formatName);
        timeFormat = null;
    }

    public void setDateFormat(String formatName) {
        super.setDateFormat(formatName);
        dateFormat = null;
    }

    public void setDateTimeFormat(String formatName) {
        super.setDateTimeFormat(formatName);
        dateTimeFormat = null;
    }

    public Configuration getConfiguration() {
        return getTemplate().getConfiguration();
    }
    
    TemplateModel getLastReturnValue() {
        return lastReturnValue;
    }
    
    void setLastReturnValue(TemplateModel lastReturnValue) {
        this.lastReturnValue = lastReturnValue;
    }
    
    void clearLastReturnValue() {
        this.lastReturnValue = null;
    }

    NumberFormat getNumberFormatObject(String pattern)
    {
        if(numberFormats == null) {
            numberFormats = new HashMap();
        }

        NumberFormat format = (NumberFormat) numberFormats.get(pattern);
        if(format != null)
        {
            return format;
        }

        // Get format from global format cache
        synchronized(localizedNumberFormats)
        {
            Locale locale = getLocale();
            NumberFormatKey fk = new NumberFormatKey(pattern, locale);
            format = (NumberFormat)localizedNumberFormats.get(fk);
            if(format == null)
            {
                // Add format to global format cache. Note this is
                // globally done once per locale per pattern.
                if("number".equals(pattern))
                {
                    format = NumberFormat.getNumberInstance(locale);
                }
                else if("currency".equals(pattern))
                {
                    format = NumberFormat.getCurrencyInstance(locale);
                }
                else if("percent".equals(pattern))
                {
                    format = NumberFormat.getPercentInstance(locale);
                }
                else if ("computer".equals(pattern))
                {
                    format = getCNumberFormat();
                }
                else
                {
                    format = new DecimalFormat(pattern, new DecimalFormatSymbols(getLocale()));
                }
                localizedNumberFormats.put(fk, format);
            }
        }

        // Clone it and store the clone in the local cache
        format = (NumberFormat)format.clone();
        numberFormats.put(pattern, format);
        return format;
    }

    DateFormat getDateFormatObject(int dateType)
    throws
        TemplateModelException
    {
        switch(dateType) {
            case TemplateDateModel.UNKNOWN: {
                return null;
            }
            case TemplateDateModel.TIME: {
                if(timeFormat == null) {
                    timeFormat = getDateFormatObject(dateType, getTimeFormat());
                }
                return timeFormat;
            }
            case TemplateDateModel.DATE: {
                if(dateFormat == null) {
                    dateFormat = getDateFormatObject(dateType, getDateFormat());
                }
                return dateFormat;
            }
            case TemplateDateModel.DATETIME: {
                if(dateTimeFormat == null) {
                    dateTimeFormat = getDateFormatObject(dateType, getDateTimeFormat());
                }
                return dateTimeFormat;
            }
            default: {
                throw new _TemplateModelException(new Object[] {
                        "Unrecognized date type: ", new Integer(dateType) });
            }
        }
    }
    
    DateFormat getDateFormatObject(int dateType, String pattern)
    throws
        TemplateModelException
    {
        if(dateFormats == null) {
            dateFormats = new Map[4];
            dateFormats[TemplateDateModel.UNKNOWN] = new HashMap();
            dateFormats[TemplateDateModel.TIME] = new HashMap();
            dateFormats[TemplateDateModel.DATE] = new HashMap();
            dateFormats[TemplateDateModel.DATETIME] = new HashMap();
        }
        Map typedDateFormat = dateFormats[dateType];

        DateFormat format = (DateFormat) typedDateFormat.get(pattern);
        if(format != null) {
            return format;
        }

        // Get format from global format cache
        synchronized(localizedDateFormats) {
            Locale locale = getLocale();
            TimeZone timeZone = getTimeZone();
            DateFormatKey fk = new DateFormatKey(dateType, pattern, locale, timeZone);
            format = (DateFormat)localizedDateFormats.get(fk);
            if(format == null) {
                // Add format to global format cache. Note this is
                // globally done once per locale per pattern.
                StringTokenizer tok = new StringTokenizer(pattern, "_");
                int dateStyle = tok.hasMoreTokens() ? parseDateStyleToken(tok.nextToken()) : DateFormat.DEFAULT;
                if(dateStyle != -1) {
                    switch(dateType) {
                        case TemplateDateModel.UNKNOWN: {
                            throw new _TemplateModelException(new _ErrorDescriptionBuilder(
                                    "Can't convert the date to string using a built-in format because it's not known "
                                    + "which parts of the date are in use.")
                                    .tips(MessageUtil.UNKNOWN_DATE_TO_STRING_TIPS));
                        }
                        case TemplateDateModel.TIME: {
                            format = DateFormat.getTimeInstance(dateStyle, locale);
                            break;
                        }
                        case TemplateDateModel.DATE: {
                            format = DateFormat.getDateInstance(dateStyle, locale);
                            break;
                        }
                        case TemplateDateModel.DATETIME: {
                            int timeStyle = tok.hasMoreTokens() ? parseDateStyleToken(tok.nextToken()) : dateStyle;
                            if(timeStyle != -1) {
                                format = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
                            }
                            break;
                        }
                    }
                }
                if(format == null) {
                    try {
                        format = new SimpleDateFormat(pattern, locale);
                    }
                    catch(IllegalArgumentException e) {
                        throw new _TemplateModelException(e, new Object[] {
                                "Can't parse ", new _DelayedJQuote(pattern), " to a date format, because:\n", e });
                    }
                }
                format.setTimeZone(timeZone);
                localizedDateFormats.put(fk, format);
            }
        }

        // Clone it and store the clone in the local cache
        format = (DateFormat)format.clone();
        typedDateFormat.put(pattern, format);
        return format;
    }

    int parseDateStyleToken(String token) {
        if("short".equals(token)) {
            return DateFormat.SHORT;
        }
        if("medium".equals(token)) {
            return DateFormat.MEDIUM;
        }
        if("long".equals(token)) {
            return DateFormat.LONG;
        }
        if("full".equals(token)) {
            return DateFormat.FULL;
        }
        return -1;
    }
    

    /**
     * Returns the {@link DateToISO8601CalendarFactory} used by the
     * the "iso_" built-ins. Be careful when using this; it should only by used
     * with {@link DateUtil#dateToISO8601String(Date, boolean, boolean, boolean,
     * int, TimeZone, DateToISO8601CalendarFactory)}.
     */
    DateToISO8601CalendarFactory getISOBuiltInCalendar() {
        if (isoBuiltInCalendarFactory == null) {
            isoBuiltInCalendarFactory = new DateUtil.TrivialDateToISO8601CalendarFactory();
        }
        return isoBuiltInCalendarFactory;
    }

    /**
     * Returns the {@link NumberFormat} used for the <tt>c</tt> built-in.
     * This is always US English <code>"0.################"</code>, without
     * grouping and without superfluous decimal separator.
     */
    public NumberFormat getCNumberFormat() {
        // It can't be cached in a static field, because DecimalFormat-s aren't
        // thread-safe.
        if (cNumberFormat == null) {
            cNumberFormat = (DecimalFormat) C_NUMBER_FORMAT.clone();
        }
        return cNumberFormat;
    }

    TemplateTransformModel getTransform(Expression exp) throws TemplateException {
        TemplateTransformModel ttm = null;
        TemplateModel tm = exp.eval(this);
        if (tm instanceof TemplateTransformModel) {
            ttm = (TemplateTransformModel) tm;
        }
        else if (exp instanceof Identifier) {
            tm = getConfiguration().getSharedVariable(exp.toString());
            if (tm instanceof TemplateTransformModel) {
                ttm = (TemplateTransformModel) tm;
            }
        }
        return ttm;
    }

    /**
     * Returns the loop or macro local variable corresponding to this
     * variable name. Possibly null.
     * (Note that the misnomer is kept for backward compatibility: loop variables
     * are not local variables according to our terminology.)
     */
    public TemplateModel getLocalVariable(String name) throws TemplateModelException {
        if (localContextStack != null) {
            for (int i = localContextStack.size()-1; i>=0; i--) {
                LocalContext lc = (LocalContext) localContextStack.get(i);
                TemplateModel tm = lc.getLocalVariable(name);
                if (tm != null) {
                    return tm;
                }
            }
        }
        return currentMacroContext == null ? null : currentMacroContext.getLocalVariable(name);
    }

    /**
     * Returns the variable that is visible in this context.
     * This is the correspondent to an FTL top-level variable reading expression.
     * That is, it tries to find the the variable in this order:
     * <ol>
     *   <li>An loop variable (if we're in a loop or user defined directive body) such as foo_has_next
     *   <li>A local variable (if we're in a macro)
     *   <li>A variable defined in the current namespace (say, via &lt;#assign ...&gt;)
     *   <li>A variable defined globally (say, via &lt;#global ....&gt;)
     *   <li>Variable in the data model:
     *     <ol>
     *       <li>A variable in the root hash that was exposed to this
                 rendering environment in the Template.process(...) call
     *       <li>A shared variable set in the configuration via a call to Configuration.setSharedVariable(...)
     *     </ol>
     *   </li>
     * </ol>
     */
    public TemplateModel getVariable(String name) throws TemplateModelException {
        TemplateModel result = getLocalVariable(name);
        if (result == null) {
            result = currentNamespace.get(name);
        }
        if (result == null) {
            result = getGlobalVariable(name);
        }
        return result;
    }

    /**
     * Returns the globally visible variable of the given name (or null).
     * This is correspondent to FTL <code>.globals.<i>name</i></code>.
     * This will first look at variables that were assigned globally via:
     * &lt;#global ...&gt; and then at the data model exposed to the template.
     */
    public TemplateModel getGlobalVariable(String name) throws TemplateModelException {
        TemplateModel result = globalNamespace.get(name);
        if (result == null) {
            result = rootDataModel.get(name);
        }
        if (result == null) {
            result = getConfiguration().getSharedVariable(name);
        }
        return result;
    }

    /**
     * Sets a variable that is visible globally.
     * This is correspondent to FTL <code><#global <i>name</i>=<i>model</i>></code>.
     * This can be considered a convenient shorthand for:
     * getGlobalNamespace().put(name, model)
     */
    public void setGlobalVariable(String name, TemplateModel model) {
        globalNamespace.put(name, model);
    }

    /**
     * Sets a variable in the current namespace.
     * This is correspondent to FTL <code><#assign <i>name</i>=<i>model</i>></code>.
     * This can be considered a convenient shorthand for:
     * getCurrentNamespace().put(name, model)
     */
    public void setVariable(String name, TemplateModel model) {
        currentNamespace.put(name, model);
    }

    /**
     * Sets a local variable (one effective only during a macro invocation).
     * This is correspondent to FTL <code><#local <i>name</i>=<i>model</i>></code>.
     * @param name the identifier of the variable
     * @param model the value of the variable.
     * @throws IllegalStateException if the environment is not executing a
     * macro body.
     */
    public void setLocalVariable(String name, TemplateModel model) {
        if(currentMacroContext == null) {
            throw new IllegalStateException("Not executing macro body");
        }
        currentMacroContext.setLocalVar(name, model);
    }

    /**
     * Returns a set of variable names that are known at the time of call. This
     * includes names of all shared variables in the {@link Configuration},
     * names of all global variables that were assigned during the template processing,
     * names of all variables in the current name-space, names of all local variables
     * and loop variables. If the passed root data model implements the
     * {@link TemplateHashModelEx} interface, then all names it retrieves through a call to
     * {@link TemplateHashModelEx#keys()} method are returned as well.
     * The method returns a new Set object on each call that is completely
     * disconnected from the Environment. That is, modifying the set will have
     * no effect on the Environment object.
     */
    public Set getKnownVariableNames() throws TemplateModelException {
        // shared vars.
        Set set = getConfiguration().getSharedVariableNames();
        
        // root hash
        if (rootDataModel instanceof TemplateHashModelEx) {
            TemplateModelIterator rootNames =
                ((TemplateHashModelEx) rootDataModel).keys().iterator();
            while(rootNames.hasNext()) {
                set.add(((TemplateScalarModel)rootNames.next()).getAsString());
            }
        }
        
        // globals
        for (TemplateModelIterator tmi = globalNamespace.keys().iterator(); tmi.hasNext();) {
            set.add(((TemplateScalarModel) tmi.next()).getAsString());
        }
        
        // current name-space
        for (TemplateModelIterator tmi = currentNamespace.keys().iterator(); tmi.hasNext();) {
            set.add(((TemplateScalarModel) tmi.next()).getAsString());
        }
        
        // locals and loop vars
        if(currentMacroContext != null) {
            set.addAll(currentMacroContext.getLocalVariableNames());
        }
        if (localContextStack != null) {
            for (int i = localContextStack.size()-1; i>=0; i--) {
                LocalContext lc = (LocalContext) localContextStack.get(i);
                set.addAll(lc.getLocalVariableNames());
            }
        }
        return set;
    }

    /**
     * Prints the current FTL stack trace. Useful for debugging.
     * {@link TemplateException}s incorporate this information in their stack traces.
     */
    public void outputInstructionStack(PrintWriter pw) {
        outputInstructionStack(getInstructionStackSnapshot(), pw);
        pw.flush();
    }

    /**
     * Prints an FTL stack trace based on a stack trace snapshot.
     * @see #getInstructionStackSnapshot()
     * @since 2.3.20
     */
    static void outputInstructionStack(
            TemplateElement[] instructionStackSnapshot, PrintWriter pw) {
        pw.println(STACK_SECTION_SEPARATOR);
        if (instructionStackSnapshot != null) {
            for (int i = 0; i < instructionStackSnapshot.length; i++) {
                TemplateElement stackEl = instructionStackSnapshot[i];
                pw.print(i == 0 ? "==> " : "    ");
                pw.println(instructionStackItemToString(stackEl));
            }
        } else {
            pw.println("[the stack was empty]");
        }
        pw.println(STACK_SECTION_SEPARATOR);
    }
    
    /**
     * Returns the snapshot of what would be printed as FTL stack trace.
     * @since 2.3.20
     */
    TemplateElement[] getInstructionStackSnapshot() {
        int requiredLength = 0;
        int ln = instructionStack.size();
        
        for (int i = 0; i < ln; i++) {
            TemplateElement stackEl = (TemplateElement) instructionStack.get(i);
            if (i == ln || stackEl.isShownInStackTrace()) {
                requiredLength++;
            }
        }
        
        if (requiredLength == 0) return null;
        
        TemplateElement[] result = new TemplateElement[requiredLength];
        int dstIdx = requiredLength - 1;
        for (int i = 0; i < ln; i++) {
            TemplateElement stackEl = (TemplateElement) instructionStack.get(i);
            if (i == ln || stackEl.isShownInStackTrace()) {
                result[dstIdx--] = stackEl;
            }
        }
        
        return result;
    }
    
    static String instructionStackItemToString(TemplateElement stackEl) {
        StringBuffer sb = new StringBuffer(); 
        sb.append(MessageUtil.shorten(stackEl.getDescription(), 40));
        
        sb.append("  [");
        Macro enclosingMacro = getEnclosingMacro(stackEl);
        if (enclosingMacro != null) {
            sb.append(MessageUtil.formatLocationForEvaluationError(
                    enclosingMacro, stackEl.beginLine, stackEl.beginColumn));
        } else {
            sb.append(MessageUtil.formatLocationForEvaluationError(
                    stackEl.getTemplate(), stackEl.beginLine, stackEl.beginColumn));
        }
        sb.append("]");
        
        return sb.toString();
    }

    static private Macro getEnclosingMacro(TemplateElement stackEl) {
        while (stackEl != null) {
            if (stackEl instanceof Macro) return (Macro) stackEl;
            stackEl = (TemplateElement) stackEl.getParent();
        }
        return null;
    }

    private void pushLocalContext(LocalContext localContext) {
        if (localContextStack == null) {
            localContextStack = new ArrayList();
        }
        localContextStack.add(localContext);
    }

    private void popLocalContext() {
        localContextStack.remove(localContextStack.size() - 1);
    }
    
    ArrayList getLocalContextStack() {
        return localContextStack;
    }

    /**
     * Returns the name-space for the name if exists, or null.
     * @param name the template path that you have used with the <code>import</code> directive
     *     or {@link #importLib(String, String)} call, in normalized form. That is, the path must be an absolute
     *     path, and it must not contain "/../" or "/./". The leading "/" is optional.
     */
    public Namespace getNamespace(String name) {
        if (name.startsWith("/")) name = name.substring(1);
        if (loadedLibs != null) {
            return (Namespace) loadedLibs.get(name);
        } else {
            return null;
        }
    }

    /**
     * Returns the main name-space.
     * This is correspondent of FTL <code>.main</code> hash.
     */
    public Namespace getMainNamespace() {
        return mainNamespace;
    }

    /**
     * Returns the main name-space.
     * This is correspondent of FTL <code>.namespace</code> hash.
     */
    public Namespace getCurrentNamespace() {
        return currentNamespace;
    }
    
    /**
     * Returns a fictitious name-space that contains the globally visible variables
     * that were created in the template, but not the variables of the data-model.
     * There is no such thing in FTL; this strange method was added because of the
     * JSP taglib support, since this imaginary name-space contains the page-scope
     * attributes.
     */
    public Namespace getGlobalNamespace() {
        return globalNamespace;
    }
    
    
    public TemplateHashModel getDataModel() {
    	final TemplateHashModel result = new TemplateHashModel() {
            public boolean isEmpty() {
                return false;
            }

            public TemplateModel get(String key) throws TemplateModelException {
                TemplateModel value = rootDataModel.get(key);
                if (value == null) {
                    value = getConfiguration().getSharedVariable(key);
                }
                return value;
            }
        };
        
        if (rootDataModel instanceof TemplateHashModelEx) {
        	return new TemplateHashModelEx() {
        		public boolean isEmpty() throws TemplateModelException {
        			return result.isEmpty();
        		}
        		public TemplateModel get(String key) throws TemplateModelException {
        			return result.get(key);
        		}
        		
        		//NB: The methods below do not take into account
        		// configuration shared variables even though
        		// the hash will return them, if only for BWC reasons
        		public TemplateCollectionModel values() throws TemplateModelException {
        			return ((TemplateHashModelEx) rootDataModel).values();
        		}
        		public TemplateCollectionModel keys() throws TemplateModelException {
        			return ((TemplateHashModelEx) rootDataModel).keys();
        		}
        		public int size() throws TemplateModelException {
        			return ((TemplateHashModelEx) rootDataModel).size();
        		}
        	};
        }
        return result;
    }

 
    /**
     * Returns the read-only hash of globally visible variables.
     * This is the correspondent of FTL <code>.globals</code> hash.
     * That is, you see the variables created with
     * <code>&lt;#global ...></code>, and the variables of the data-model.
     * To create new global variables, use {@link #setGlobalVariable setGlobalVariable}.
     */
    public TemplateHashModel getGlobalVariables() {
        return new TemplateHashModel() {
            public boolean isEmpty() {
                return false;
            }
            public TemplateModel get(String key) throws TemplateModelException {
                TemplateModel result = globalNamespace.get(key);
                if (result == null) {
                    result = rootDataModel.get(key);
                }
                if (result == null) {
                    result = getConfiguration().getSharedVariable(key);
                }
                return result;
            }
        };
    }

    private void pushElement(TemplateElement element) {
        instructionStack.add(element);
    }

    private void popElement() {
        instructionStack.remove(instructionStack.size() - 1);
    }
    
    void replaceElemetStackTop(TemplateElement instr) {
        instructionStack.set(instructionStack.size() - 1, instr);
    }

    public TemplateNodeModel getCurrentVisitorNode() {
        return currentVisitorNode;
    }
    
    /**
     * sets TemplateNodeModel as the current visitor node. <tt>.current_node</tt>
     */
    public void setCurrentVisitorNode(TemplateNodeModel node) {
        currentVisitorNode = node;
    }
    
    TemplateModel getNodeProcessor(TemplateNodeModel node) throws TemplateException {
        String nodeName = node.getNodeName();
        if (nodeName == null) {
            throw new _MiscTemplateException(this, "Node name is null.");
        }
        TemplateModel result = getNodeProcessor(nodeName, node.getNodeNamespace(), 0);
    
        if (result == null) {
            String type = node.getNodeType();
        
            /* DD: Original version: */
            if (type == null) {
                type = "default";
            }
            result = getNodeProcessor("@" + type, null, 0);
        
            /* DD: Jonathan's non-BC version and IMHO otherwise wrong version:
            if (type != null) {
                result = getNodeProcessor("@" + type, null, 0);
            }
            if (result == null) {
                result = getNodeProcessor("@default", null, 0);
            }
            */
        }
        return result;    
    }
    
    private TemplateModel getNodeProcessor(final String nodeName, final String nsURI, int startIndex) 
    throws TemplateException 
    {
        TemplateModel result = null;
        int i;
        for (i = startIndex; i<nodeNamespaces.size(); i++) {
            Namespace ns = null;
            try {                                   
                ns = (Namespace) nodeNamespaces.get(i);
            } catch (ClassCastException cce) {
                throw new _MiscTemplateException(this,
                        "A \"using\" clause should contain a sequence of namespaces or strings that indicate the "
                        + "location of importable macro libraries.");
            }
            result = getNodeProcessor(ns, nodeName, nsURI);
            if (result != null) 
                break;
        }
        if (result != null) {
            this.nodeNamespaceIndex = i+1;
            this.currentNodeName = nodeName;
            this.currentNodeNS = nsURI;
        }
        return result;
    }
    
    private TemplateModel getNodeProcessor(Namespace ns, String localName, String nsURI) throws TemplateException {
        TemplateModel result = null;
        if (nsURI == null) {
            result = ns.get(localName);
            if (!(result instanceof Macro) && !(result instanceof TemplateTransformModel)) {
                result = null;
            }
        } else {
            Template template = ns.getTemplate();
            String prefix = template.getPrefixForNamespace(nsURI);
            if (prefix == null) {
                // The other template cannot handle this node
                // since it has no prefix registered for the namespace
                return null;
            }
            if (prefix.length() >0) {
                result = ns.get(prefix + ":" + localName);
                if (!(result instanceof Macro) && !(result instanceof TemplateTransformModel)) {
                    result = null;
                }
            } else {
                if (nsURI.length() == 0) {
                    result = ns.get(Template.NO_NS_PREFIX + ":" + localName);
                    if (!(result instanceof Macro) && !(result instanceof TemplateTransformModel)) {
                        result = null;
                    }
                }
                if (nsURI.equals(template.getDefaultNS())) {
                    result = ns.get(Template.DEFAULT_NAMESPACE_PREFIX + ":" + localName);
                    if (!(result instanceof Macro) && !(result instanceof TemplateTransformModel)) {
                        result = null;
                    }
                }
                if (result == null) {
                    result = ns.get(localName);
                    if (!(result instanceof Macro) && !(result instanceof TemplateTransformModel)) {
                        result = null;
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Emulates <code>include</code> directive, except that <code>name</code> must be tempate
     * root relative.
     *
     * <p>It's the same as <code>include(getTemplateForInclusion(name, encoding, parse))</code>.
     * But, you may want to separately call these two methods, so you can determine the source of
     * exceptions more precisely, and thus achieve more intelligent error handling.
     *
     * @see #getTemplateForInclusion(String name, String encoding, boolean parse)
     * @see #include(Template includedTemplate)
     */
    public void include(String name, String encoding, boolean parse)
    throws IOException, TemplateException
    {
        include(getTemplateForInclusion(name, encoding, parse));
    }

    /**
     * Gets a template for inclusion; used with {@link #include(Template includedTemplate)}.
     * The advantage over simply using <code>config.getTemplate(...)</code> is that it chooses
     * the default encoding as the <code>include</code> directive does.
     *
     * @param name the name of the template, relatively to the template root directory
     * (not the to the directory of the currently executing template file!).
     * (Note that you can use {@link freemarker.cache.TemplateCache#getFullTemplatePath}
     * to convert paths to template root relative paths.)
     * @param encoding the encoding of the obtained template. If null,
     * the encoding of the Template that is currently being processed in this
     * Environment is used.
     * @param parse whether to process a parsed template or just include the
     * unparsed template source.
     */
    public Template getTemplateForInclusion(String name, String encoding, boolean parse)
    throws IOException
    {
        if (encoding == null) {
            encoding = getTemplate().getEncoding();
        }
        if (encoding == null) {
            encoding = getConfiguration().getEncoding(this.getLocale());
        }
        return getConfiguration().getTemplate(name, getLocale(), encoding, parse);
    }

    /**
     * Processes a Template in the context of this <code>Environment</code>, including its
     * output in the <code>Environment</code>'s Writer.
     *
     * @param includedTemplate the template to process. Note that it does <em>not</em> need
     * to be a template returned by
     * {@link #getTemplateForInclusion(String name, String encoding, boolean parse)}.
     */
    public void include(Template includedTemplate)
    throws TemplateException, IOException
    {
        Template prevTemplate = getTemplate();
        setParent(includedTemplate);
        importMacros(includedTemplate);
        try {
            visit(includedTemplate.getRootTreeNode());
        }
        finally {
            setParent(prevTemplate);
        }
    }
    
    /**
     * Emulates <code>import</code> directive, except that <code>name</code> must be tempate
     * root relative.
     *
     * <p>It's the same as <code>importLib(getTemplateForImporting(name), namespace)</code>.
     * But, you may want to separately call these two methods, so you can determine the source of
     * exceptions more precisely, and thus achieve more intelligent error handling.
     *
     * @see #getTemplateForImporting(String name)
     * @see #importLib(Template includedTemplate, String namespace)
     */
    public Namespace importLib(String name, String namespace)
    throws IOException, TemplateException
    {
        return importLib(getTemplateForImporting(name), namespace);
    }

    /**
     * Gets a template for importing; used with
     * {@link #importLib(Template importedTemplate, String namespace)}. The advantage
     * over simply using <code>config.getTemplate(...)</code> is that it chooses the encoding
     * as the <code>import</code> directive does.
     *
     * @param name the name of the template, relatively to the template root directory
     * (not the to the directory of the currently executing template file!).
     * (Note that you can use {@link freemarker.cache.TemplateCache#getFullTemplatePath}
     * to convert paths to template root relative paths.)
     */
    public Template getTemplateForImporting(String name) throws IOException {
        return getTemplateForInclusion(name, null, true);
    }
    
    /**
     * Emulates <code>import</code> directive.
     *
     * @param loadedTemplate the template to import. Note that it does <em>not</em> need
     * to be a template returned by {@link #getTemplateForImporting(String name)}.
     */
    public Namespace importLib(Template loadedTemplate, String namespace)
    throws IOException, TemplateException
    {
        if (loadedLibs == null) {
            loadedLibs = new HashMap();
        }
        String templateName = loadedTemplate.getName();
        Namespace existingNamespace = (Namespace) loadedLibs.get(templateName);
        if (existingNamespace != null) {
            if (namespace != null) {
                setVariable(namespace, existingNamespace);
            }
        }
        else {
            Namespace newNamespace = new Namespace(loadedTemplate);
            if (namespace != null) {
                currentNamespace.put(namespace, newNamespace);
                if (currentNamespace == mainNamespace) {
                    globalNamespace.put(namespace, newNamespace);
                }
            }
            Namespace prevNamespace = this.currentNamespace;
            this.currentNamespace = newNamespace;
            loadedLibs.put(templateName, currentNamespace);
            Writer prevOut = out;
            this.out = NullWriter.INSTANCE;
            try {
                include(loadedTemplate);
            } finally {
                this.out = prevOut;
                this.currentNamespace = prevNamespace;
            }
        }
        return (Namespace) loadedLibs.get(templateName);
    }
    
    String renderElementToString(TemplateElement te) throws IOException, TemplateException {
        Writer prevOut = out;
        try {
            StringWriter sw = new StringWriter();
            this.out = sw;
            visit(te);
            return sw.toString();
        } 
        finally {
            this.out = prevOut;
        }
    }

    void importMacros(Template template) {
        for (Iterator it = template.getMacros().values().iterator(); it.hasNext();) {
            visitMacroDef((Macro) it.next());
        }
    }

    /**
     * @return the namespace URI registered for this prefix, or null.
     * This is based on the mappings registered in the current namespace.
     */
    public String getNamespaceForPrefix(String prefix) {
        return currentNamespace.getTemplate().getNamespaceForPrefix(prefix);
    }
    
    public String getPrefixForNamespace(String nsURI) {
        return currentNamespace.getTemplate().getPrefixForNamespace(nsURI);
    }
    
    /**
     * @return the default node namespace for the current FTL namespace
     */
    public String getDefaultNS() {
        return currentNamespace.getTemplate().getDefaultNS();
    }
    
    /**
     * A hook that Jython uses.
     */
    public Object __getitem__(String key) throws TemplateModelException {
        return BeansWrapper.getDefaultInstance().unwrap(getVariable(key));
    }

    /**
     * A hook that Jython uses.
     */
    public void __setitem__(String key, Object o) throws TemplateException {
        setGlobalVariable(key, getObjectWrapper().wrap(o));
    }

    private static final class NumberFormatKey
    {
        private final String pattern;
        private final Locale locale;

        NumberFormatKey(String pattern, Locale locale)
        {
            this.pattern = pattern;
            this.locale = locale;
        }

        public boolean equals(Object o)
        {
            if(o instanceof NumberFormatKey)
            {
                NumberFormatKey fk = (NumberFormatKey)o;
                return fk.pattern.equals(pattern) && fk.locale.equals(locale);
            }
            return false;
        }

        public int hashCode()
        {
            return pattern.hashCode() ^ locale.hashCode();
        }
    }

    private static final class DateFormatKey
    {
        private final int dateType;
        private final String pattern;
        private final Locale locale;
        private final TimeZone timeZone;

        DateFormatKey(int dateType, String pattern, Locale locale, TimeZone timeZone)
        {
            this.dateType = dateType;
            this.pattern = pattern;
            this.locale = locale;
            this.timeZone = timeZone;
        }

        public boolean equals(Object o)
        {
            if(o instanceof DateFormatKey)
            {
                DateFormatKey fk = (DateFormatKey)o;
                return dateType == fk.dateType && fk.pattern.equals(pattern) && fk.locale.equals(locale) && fk.timeZone.equals(timeZone);
            }
            return false;
        }

        public int hashCode()
        {
            return dateType ^ pattern.hashCode() ^ locale.hashCode() ^ timeZone.hashCode();
        }
    }
    
    public class Namespace extends SimpleHash {
        
        private Template template;
        
        Namespace() {
            this.template = Environment.this.getTemplate();
        }
        
        Namespace(Template template) {
            this.template = template;
        }
        
        /**
         * @return the Template object with which this Namespace is associated.
         */
        public Template getTemplate() {
            return template == null ? Environment.this.getTemplate() : template;
        }
    }

     private static final Writer EMPTY_BODY_WRITER = new Writer() {
    
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (len > 0) {
                throw new IOException(
                        "This transform does not allow nested content.");
            }
        }
    
        public void flush() {
        }
    
        public void close() {
        }
    };
    
    /**
     * See {@link #setFastInvalidReferenceExceptions(boolean)}. 
     */
    boolean getFastInvalidReferenceExceptions() {
        return fastInvalidReferenceExceptions;
    }
    
    /**
     * Sets if for invalid references {@link InvalidReferenceException#FAST_INSTANCE} should be thrown, or a new
     * {@link InvalidReferenceException}. The "fast" instance is used if we know that the error will be handled
     * so that its message will not be logged or shown anywhere.
     */
    boolean setFastInvalidReferenceExceptions(boolean b) {
        boolean res = fastInvalidReferenceExceptions;
        fastInvalidReferenceExceptions = b;
        return res;
    }
    
}
